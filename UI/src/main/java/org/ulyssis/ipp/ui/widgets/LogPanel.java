package org.ulyssis.ipp.ui.widgets;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.ulyssis.ipp.status.StatusMessage;
import org.ulyssis.ipp.ui.UIApplication;
import org.ulyssis.ipp.ui.state.SharedState;

import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.WAbstractTableModel;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTableView;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WWidget;

public final class LogPanel extends CollapsablePanel {
	private final WTemplate barContent;
	private final WTableView log;
	private final SharedState sharedState;
	private final MessageTable messageTable;
	
	private static final class MessageTable extends WAbstractTableModel {
		private final List<StatusMessage> messages = new ArrayList<>();
		private final List<Instant> messageTimes = new ArrayList<>();
		
		public void addMessage(Instant instant, StatusMessage message) {
			beginInsertRows(null, 0, 0);
			messages.add(message);
			messageTimes.add(instant);
			endInsertRows();
		}

		@Override
		public int getColumnCount(WModelIndex parent) {
			return 3;
		}

		@Override
		public int getRowCount(WModelIndex parent) {
			return messages.size();
		}

		@Override
		public Object getData(WModelIndex index, int role) {
			if (index.getColumn() >= 3)
				return null;
			if (index.getRow() >= messages.size())
				return null;
			if (role != ItemDataRole.DisplayRole)
				return null;
			
			int i = messages.size() - index.getRow() - 1;
			if (index.getColumn() == 0) {
				// TODO: Not just Europe/Brussels!
				return LocalDateTime.ofInstant(messageTimes.get(i), ZoneId.systemDefault()).toString(); // TODO: Formatting?
			} else if (index.getColumn() == 1) {
				return messages.get(i).getType().toString();
			} else if (index.getColumn() == 2) {
				return messages.get(i).getDetails();
			}
			
			return null;
		}
		
		@Override
		public Object getHeaderData(int section, Orientation orientation, int role) {
			if (orientation == Orientation.Horizontal && role == ItemDataRole.DisplayRole) {
				if (section == 0) {
					return "Time";
				} else if (section == 1) {
					return "Type";
				} else if (section == 2) {
					return "Details";
				}
			}
			return super.getHeaderData(section, orientation, role);
		}
	}

	public LogPanel() {
		this(null);
	}
	
	public LogPanel(WContainerWidget parent) {
		super(parent);
		
		UIApplication app = UIApplication.getInstance();
		sharedState = app.getSharedState();
		
		barContent = new WTemplate(WString.tr("log-panel-bar"));
		barContent.addStyleClass("log-panel-bar");
		log = new WTableView();
		log.addStyleClass("log-panel-log");
		log.setHeight(new WLength(200));
		log.setColumnWidth(0, new WLength(200));
		log.setColumnWidth(2, new WLength(600));
		messageTable = new MessageTable();
		log.setModel(messageTable);
		log.setSortingEnabled(false);
		
		barContent.bindEmpty("message-time");
		barContent.bindEmpty("message-type");
		barContent.bindEmpty("message");
		
		addStyleClass("log-panel");
		
		sharedState.addStatusListener(app, statusListener);
	}
	
	private final Consumer<StatusMessage> statusListener = this::onNewStatus;
	private void onNewStatus(StatusMessage message) {
		if (message.getType() != StatusMessage.MessageType.NEW_SNAPSHOT) {
			Instant now = Instant.now();
			messageTable.addMessage(now, message);
			barContent.bindString("message-time", LocalDateTime.ofInstant(now, ZoneId.systemDefault()).toString());
			barContent.bindString("message-type", message.getType().toString());
			barContent.bindString("message", message.getDetails());
		}
	}

	@Override
	protected WWidget barContentWidget() {
		return barContent;
	}

	@Override
	protected WWidget contentWidget() {
		return log;
	}

}
