package org.ulyssis.ipp.ui.widgets;

import org.ulyssis.ipp.control.commands.SetEndTimeCommand;
import org.ulyssis.ipp.control.commands.SetStartTimeCommand;
import org.ulyssis.ipp.control.commands.SetStatusCommand;
import org.ulyssis.ipp.control.commands.SetStatusMessageCommand;
import org.ulyssis.ipp.publisher.Score;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.ui.UIApplication;
import org.ulyssis.ipp.ui.state.SharedState;
import org.ulyssis.ipp.updates.Status;

import eu.webtoolkit.jwt.WComboBox;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WTextArea;
import eu.webtoolkit.jwt.WWidget;

public class ContestControlPanel extends CollapsablePanel {
	private final WTemplate barContent;
	private final WTemplate content;
	
	private final SharedState sharedState;
	private final WTextArea messageEdit;
	private final WPushButton setMessageButton;
	private final WComboBox statusCombo;
	private final WPushButton setStatusButton;
	private final SafetyButton startButton;
	private final SafetyButton stopButton;
	
	private final SharedState.SnapshotScoreListener onNewScore = this::newScore;
	private void newScore(Snapshot snapshot, Score score, boolean newSnapshot) {
		content.bindString("current-message", snapshot.getStatusMessage());
		content.bindString("current-status", snapshot.getStatus().toString());
	}
	
	public ContestControlPanel() {
		this(null);
	}
	
	public ContestControlPanel(WContainerWidget parent) {
		super(parent);
		
		UIApplication app = UIApplication.getInstance();
		sharedState = app.getSharedState();
		
		sharedState.addScoreListener(app, onNewScore);

		barContent = new WTemplate(WString.tr("control-panel-bar"));
		content = new WTemplate(WString.tr("control-panel-content"));
		
		content.bindEmpty("current-message");
		content.bindEmpty("current-status");
		
		messageEdit = new WTextArea();
		content.bindWidget("message-edit", messageEdit);
		setMessageButton = new WPushButton("Set message");
		content.bindWidget("message-button", setMessageButton);
		setMessageButton.clicked().addListener(this, () -> {
			sharedState.getCommandDispatcher().sendAsync(new SetStatusMessageCommand(messageEdit.getText()));
		});
		
		statusCombo = new WComboBox();
		content.bindWidget("status-combo", statusCombo);
		setStatusButton = new WPushButton("Set status");
		content.bindWidget("status-button", setStatusButton);
		for (Status status : Status.values()) {
			statusCombo.addItem(status.toString());
		}
		setStatusButton.clicked().addListener(this, () -> {
			sharedState.getCommandDispatcher().sendAsync(new SetStatusCommand(Status.valueOf(statusCombo.getCurrentText().toString())));
		});
		
		startButton = new SafetyButton("Start");
		content.bindWidget("start-button", startButton);
		startButton.getButton().clicked().addListener(this, () -> {
			sharedState.getCommandDispatcher().sendAsync(new SetStartTimeCommand());
		});
		
		stopButton = new SafetyButton("Stop");
		content.bindWidget("stop-button", stopButton);
		stopButton.getButton().clicked().addListener(this, () -> {
			sharedState.getCommandDispatcher().sendAsync(new SetEndTimeCommand());
		});
	}

	@Override
	protected WWidget barContentWidget() {
		return barContent;
	}

	@Override
	protected WWidget contentWidget() {
		return content;
	}

}
