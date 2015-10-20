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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import eu.webtoolkit.jwt.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.processor.Database;
import org.ulyssis.ipp.publisher.Score;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.ui.TimeUtils;
import org.ulyssis.ipp.ui.UIApplication;
import org.ulyssis.ipp.ui.state.SharedState;
import org.ulyssis.ipp.ui.state.SharedState.SnapshotScoreListener;

public class HeaderWidget extends WTemplate {
    private final Logger LOG = LogManager.getLogger(HeaderWidget.class);

	private final SnapshotScoreListener onNewSnapshot = this::newSnapshot;
	private void newSnapshot(Snapshot snapshot, Score score, boolean newSnapshot) {
		Instant now = Instant.now();
		bindString("current-date", TimeUtils.formatDateForInstant(now));
		bindString("current-time", TimeUtils.formatTimeForInstant(now));

		bindString("start-date", TimeUtils.formatDateForInstant(snapshot.getStartTime()));
		bindString("start-time", TimeUtils.formatTimeForInstant(snapshot.getStartTime()));
	}
	
	public HeaderWidget() {
		this(null);
	}
	
	public HeaderWidget(WContainerWidget parent) {
		super(WString.tr("header"), parent);
		
		UIApplication app = UIApplication.getInstance();
		SharedState sharedState = app.getSharedState();
		sharedState.addScoreListener(app, onNewSnapshot);
		
		Instant now = Instant.now();
		bindString("current-date", TimeUtils.formatDateForInstant(now));
		bindString("current-time", TimeUtils.formatTimeForInstant(now));
		
		bindEmpty("start-date");
		bindEmpty("start-time");

		final WPushButton showResultsButton = new WPushButton("Show results");
		bindWidget("show-results", showResultsButton);
		showResultsButton.clicked().addListener(this, new Signal1.Listener<WMouseEvent>() {
			@Override
			public void trigger(WMouseEvent wMouseEvent) {
                try (Connection connection = Database.createConnection(EnumSet.of(Database.ConnectionFlags.READ_ONLY))) {
                    Optional<Snapshot> snapshot = Snapshot.loadLatest(connection);
                    final WDialog dialog = new WDialog("Results");
                    dialog.setClosable(true);
                    if (snapshot.isPresent()) {
                        Score score = new Score(snapshot.get(), false);
                        WTable table = new WTable(dialog.getContents());
                        table.setStyleClass("results-table");
                        List<Score.Team> teams = new ArrayList<>(score.getTeams());
                        table.setHeaderCount(1);
                        table.getElementAt(0, 0).addWidget(new WText("Position"));
                        table.getElementAt(0, 1).addWidget(new WText("Team name"));
                        table.getElementAt(0, 2).addWidget(new WText("Lap count"));
                        int pos = 1;
                        for (int i = 0; i < teams.size(); ++i) {
                            Score.Team team = teams.get(i);
                            table.getElementAt(i+1, 0).addWidget(new WText(String.valueOf(pos), TextFormat.PlainText));
                            table.getElementAt(i+1, 1).addWidget(new WText(team.getName(), TextFormat.PlainText));
                            table.getElementAt(i+1, 2).addWidget(new WText(String.valueOf(team.getLaps()), TextFormat.PlainText));
                            if (i + 1 != teams.size()) {
                                if (teams.get(i+1).getLaps() != team.getLaps()) {
                                    pos = i + 2;
                                }
                            }
                        }
                    } else {
                        new WText("No results yet", dialog.getContents());
                    }
                    dialog.show();
                } catch (SQLException | IOException e) {
                    LOG.error("An exception occurred while getting the latest snapshot", e);
                }
            }
		});
	}
}
