/*
 * Copyright (C) 2014-2015 ULYSSIS VZW
 *
 * This file is part of i++.
 * 
 * i++ is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Affero General Public License
 * as published by the Free Software Foundation. No other versions apply.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.ulyssis.ipp.ui.widgets;

import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.WAbstractTableModel;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WMenu;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WProgressBar;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WSpinBox;
import eu.webtoolkit.jwt.WStackedWidget;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.chart.Axis;
import eu.webtoolkit.jwt.chart.SeriesType;
import eu.webtoolkit.jwt.chart.WCartesianChart;
import eu.webtoolkit.jwt.chart.WDataSeries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.TagId;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.config.Team;
import org.ulyssis.ipp.control.commands.AddTagCommand;
import org.ulyssis.ipp.control.commands.CorrectionCommand;
import org.ulyssis.ipp.control.commands.RemoveTagCommand;
import org.ulyssis.ipp.publisher.Score;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.snapshot.events.Event;
import org.ulyssis.ipp.snapshot.events.TagSeenEvent;
import org.ulyssis.ipp.ui.UIApplication;
import org.ulyssis.ipp.ui.state.SharedState;
import org.ulyssis.ipp.utils.JedisHelper;
import org.ulyssis.ipp.utils.Serialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.io.BaseEncoding.DecodingException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TeamPanel extends CollapsablePanel {
    private static final Logger LOG = LogManager.getLogger(TeamPanel.class);

    private final Team team;
    private final WTemplate barContent;
    private final WContainerWidget content;
    private final WMenu menu;
    private final WStackedWidget stack;
    private final WContainerWidget chartContainer;
    private final WTemplate tagsView;
    private final WTemplate correctionsView;
    
    private final WContainerWidget tagsTable;
    private final WLineEdit addTagEdit;
    private final WPushButton addTagButton;
    
    private final WSpinBox correctionSpinner;
    private final WPushButton addCorrection;
    
    private final WProgressBar projectedProgress;
    private final WProgressBar actualProgress;

    private UIApplication application;
    private final SharedState sharedState;
    
    private final List<WCartesianChart> charts = new ArrayList<>();
    private final List<EventsModel> itemModels = new ArrayList<>();
    
    private Optional<Snapshot> latestSnapshot = Optional.empty();
    
    private final Config config;
    private final Jedis jedis;
    
    private static final class EventsModel extends WAbstractTableModel {
    	private List<TagSeenEvent> events = Collections.emptyList();
    	private Instant oneHourAgo = Instant.now().minus(1L, ChronoUnit.HOURS);
    	
    	public void setEvents(Instant oneHourAgo, List<TagSeenEvent> events) {
    		this.oneHourAgo = oneHourAgo;
    		this.events = events;
    		this.reset();
    	}

		@Override
		public int getColumnCount(WModelIndex parent) {
			return 2;
		}

		@Override
		public int getRowCount(WModelIndex parent) {
			return Math.max(0, events.size() - 1);
		}

		@Override
		public Object getData(WModelIndex index, int role) {
			if (role == ItemDataRole.DisplayRole) {
				if (index.getRow() >= events.size() - 1 || index.getColumn() >= 2) {
					return null;
				}
                if (index.getRow() < 0) {
                    return 0; // TODO: What to do if we don't have enough events?
                }
				if (index.getColumn() == 0) {
					return Duration.between(oneHourAgo, events.get(index.getRow() + 1).getTime()).toMillis() / 1000D;
				} else if (index.getColumn() == 1) {
					return Duration.between(events.get(index.getRow()).getTime(), events.get(index.getRow() + 1).getTime()).toMillis() / 1000D;
				}
			}
			return null;
		}
    	
    }
    
    private final SharedState.SnapshotScoreListener scoreListener = this::processNewScore;
    private void processNewScore(Snapshot snapshot, Score score, boolean newSnapshot) {
        updateTags(snapshot);
    	latestSnapshot = Optional.of(snapshot);
        try {
            barContent.bindInt("rounds", snapshot.getTeamStates().getNbLapsForTeam(team.getTeamNb()));
            int i = 0;
            for (Score.Team team : score.getTeams()) {
            	if (team.getNb() == this.team.getTeamNb()) {
            		break;
            	}
            	i ++;
            }
            int ranking = i + 1;
            barContent.bindInt("pos", ranking);
            score.getTeams().stream().filter(team -> team.getNb() == this.team.getTeamNb()).findFirst().ifPresent(scoreTeam -> {
            	snapshot.getTeamStates().getStateForTeam(this.team.getTeamNb()).ifPresent(teamState -> {
            		double pos = teamState.getLastTagSeenEvent().map(ev -> config.getReader(ev.getReaderId()).getPosition()).orElse(0.0);
            		actualProgress.setValue(pos / config.getTrackLength());
            		projectedProgress.setValue(scoreTeam.getPosition());
            		if (scoreTeam.getNonLimitedPosition() > 1.0) {
            			double alpha = (scoreTeam.getNonLimitedPosition() - 1.0) * 4;
            			// TODO: Use the alpha to indicate how severe the delay is
            		}
            		if (getState() == State.Extended && newSnapshot && menu.getCurrentIndex() == 0) {
            			updateCharts(snapshot);
            		}
            	});
            });
        } catch (Exception e) {
        	LOG.error("Exception", e);
        }
    }
    
    private Set<TagId> oldTags = new HashSet<TagId>();
    
    private void updateTags(Snapshot snapshot) {
    	Set<TagId> newTags = new HashSet<TagId>();
    	snapshot.getTeamTagMap().getTagToTeam().forEach((tag, teamNb) -> {
    		if (team.getTeamNb() == teamNb) {
    			newTags.add(tag);
    		}
    	});
    	if (!newTags.equals(oldTags)) {
    		oldTags = newTags;
    		tagsTable.clear();
    		for (TagId tag : newTags) {
    			WTemplate tagsTableItem = new WTemplate(WString.tr("tags-table-item"));
    			tagsTableItem.bindString("tag-id", tag.toString());
    			final WPushButton removeTagButton = new WPushButton("Remove");
    			tagsTableItem.bindWidget("remove-button", removeTagButton);
    			removeTagButton.clicked().addListener(this, () -> removeTag(tag, removeTagButton));
    			tagsTable.addWidget(tagsTableItem);
    		}
    	}
    }
    
    private void removeTag(TagId tag, WPushButton removeButton) {
    	sharedState.getCommandDispatcher().sendAsync(new RemoveTagCommand(tag, team.getTeamNb()));
    }
    
    // TODO: This shouldn't be entirely based on the latest snapshot...
    private void updateCharts(Snapshot snapshot) {
    	Instant now = Instant.now();
    	Instant anHourAgo = now.minus(Duration.ofHours(1L));
    	try {
            Set<byte[]> events = jedis.zrangeByScore("events".getBytes(StandardCharsets.UTF_8), anHourAgo.toEpochMilli(), now.toEpochMilli());
            if (events == null) {
            	LOG.warn("No events");
            	return;
            }
            List<Optional<Instant>> eventTimes = new ArrayList<>();
            List<List<TagSeenEvent>> eventLists = new ArrayList<>();
            for (int i = 0; i < config.getNbReaders(); i++) {
            	eventTimes.add(Optional.empty());
            	eventLists.add(new ArrayList<>());
            }
            for (byte[] eventBytes : events) {
            	try {
            		Event event = Serialization.getJsonMapper().readValue(eventBytes, Event.class);
            		if (event instanceof TagSeenEvent) {
            			final TagSeenEvent tagSeenEvent = (TagSeenEvent)event;
            			Optional<Integer> teamNb = snapshot.getTeamTagMap().tagToTeam(tagSeenEvent.getTag());
            			if (teamNb.isPresent() && teamNb.get().equals(team.getTeamNb())) {
            				eventTimes.get(tagSeenEvent.getReaderId()).ifPresent(lastTime -> {
            					try {
            						eventLists.get(tagSeenEvent.getReaderId()).add(tagSeenEvent);
            					} catch (Exception e) {
            						LOG.error("Exception", e);
            					}
            				});
            				eventTimes.set(tagSeenEvent.getReaderId(), Optional.of(event.getTime()));
            			}
            		}
                } catch (JsonParseException e) {
                	LOG.error("Error parsing event", e);
                } catch (JsonMappingException e) {
                	LOG.error("Error mapping JSON", e);
                } catch (IOException e) {
                	LOG.error("General IO exception", e);
                }
            }
            for (int i = 0; i < config.getNbReaders(); i++) {
            	itemModels.get(i).setEvents(anHourAgo, eventLists.get(i));
            }
    	} catch (JedisConnectionException e) {
    		LOG.error("Couldn't fetch events", e);
    	}
    }

    public TeamPanel(Team team) {
        this(team, null);
    }

    public TeamPanel(Team team, WContainerWidget parent) {
        super(parent);

        this.team = team;

        application = UIApplication.getInstance();
        config = Config.getCurrentConfig();
        sharedState = application.getSharedState();
        jedis = JedisHelper.get(sharedState.getRedisURI());

        barContent = new WTemplate(WString.tr("team-bar"));
        content = new WContainerWidget();
        stack = new WStackedWidget();
        stack.addStyleClass("teams-stack");
        menu = new WMenu(stack, Orientation.Horizontal);
        menu.addStyleClass("teams-menu");
        chartContainer = new WContainerWidget();
        chartContainer.addStyleClass("teams-charts");
        tagsView = new WTemplate(WString.tr("tags-view"));
        tagsView.addStyleClass("tags-view");
        correctionsView = new WTemplate(WString.tr("corrections-view"));
        correctionsView.addStyleClass("corrections-view");

        menu.addItem("Charts", chartContainer);
        menu.addItem("Tags", tagsView);
        menu.addItem("Corrections", correctionsView);
        
        content.addWidget(menu);
        content.addWidget(stack);

        barContent.bindInt("nb", team.getTeamNb());
        barContent.bindString("name", team.getName());
        barContent.bindInt("pos", team.getTeamNb());
        barContent.bindInt("rounds", 0);
        
        projectedProgress = new WProgressBar();
        projectedProgress.setStyleClass("projected-progress");
        projectedProgress.setMinimum(0.0);
        projectedProgress.setMaximum(1.0);
        barContent.bindWidget("projected-progress", projectedProgress);
        
        actualProgress = new WProgressBar();
        actualProgress.setStyleClass("actual-progress");
        actualProgress.setMinimum(0.0);
        actualProgress.setMaximum(1.0);
        barContent.bindWidget("actual-progress", actualProgress);
        
        for (int i = 0; i < config.getNbReaders(); i++) {
        	WCartesianChart readerChart = new WCartesianChart();
        	readerChart.addStyleClass("reader-chart");
        	EventsModel itemModel = new EventsModel();
        	readerChart.setModel(itemModel);
        	readerChart.setXSeriesColumn(0);
        	WDataSeries series = new WDataSeries(1, SeriesType.LineSeries);
        	readerChart.addSeries(series);
        	readerChart.resize(300, 200);
        	readerChart.getAxis(Axis.XAxis).setRange(-3600, 0);
        	readerChart.getAxis(Axis.YAxis).setMinimum(0);
        	chartContainer.addWidget(readerChart);
        	charts.add(readerChart);
        	itemModels.add(itemModel);
        }
        
        tagsTable = new WContainerWidget();
        tagsView.bindWidget("tags-table", tagsTable);
        addTagEdit = new WLineEdit();
        tagsView.bindWidget("add-tag-id", addTagEdit);
        addTagButton = new WPushButton("Add");
        tagsView.bindWidget("add-tag-button", addTagButton);
        addTagButton.clicked().addListener(this, this::addTag);
        
        correctionSpinner = new WSpinBox();
        correctionSpinner.setMinimum(-2000);
        correctionSpinner.setMaximum(2000);
        correctionsView.bindWidget("correction-spinner", correctionSpinner);
        addCorrection = new WPushButton("Commit");
        correctionsView.bindWidget("commit-button", addCorrection);
        addCorrection.clicked().addListener(this, this::performCorrection);

        sharedState.addScoreListener(application, scoreListener);
    }
    
    private void addTag() {
    	String tagIdString = addTagEdit.getText();
    	addTagEdit.setText("");
    	try {
    		TagId tag = new TagId(tagIdString);
    		sharedState.getCommandDispatcher().sendAsync(new AddTagCommand(tag, team.getTeamNb()));
    	} catch (IllegalArgumentException e) {
    		// TODO: Handle decoding exception!
    	}
    }
    
    private void performCorrection() {
    	int correction = correctionSpinner.getValue();
    	correctionSpinner.setValue(0);
    	sharedState.getCommandDispatcher().sendAsync(new CorrectionCommand(team.getTeamNb(), correction));
    }

    @Override
    public void remove() {
        sharedState.removeScoreListener(application, scoreListener);
        super.remove();
    }

    @Override
    protected WWidget barContentWidget() {
        return barContent;
    }

    @Override
    protected WWidget contentWidget() {
        return content;
    }
    
    @Override
    protected void toggleOpenClosed() {
    	super.toggleOpenClosed();
    	if (latestSnapshot.isPresent()) {
    		updateCharts(latestSnapshot.get());
    	}
    }
}
