package org.ulyssis.ipp.ui.widgets;

import java.time.Instant;

import org.ulyssis.ipp.publisher.Score;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.ui.TimeUtils;
import org.ulyssis.ipp.ui.UIApplication;
import org.ulyssis.ipp.ui.state.SharedState;
import org.ulyssis.ipp.ui.state.SharedState.SnapshotScoreListener;

import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTemplate;

public class HeaderWidget extends WTemplate {
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
	}
}
