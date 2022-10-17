package org.ulyssis.ipp.ui.widgets;

import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WString;
import org.ulyssis.ipp.status.StatusMessage;
import org.ulyssis.ipp.ui.UIApplication;
import org.ulyssis.ipp.ui.state.SharedState;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Consumer;

public class NotificationWidget extends WContainerWidget {
    private final Consumer<StatusMessage> statusListener = this::onNewStatus;

    public NotificationWidget(WContainerWidget parent) {
        super(parent);

        UIApplication app = UIApplication.getInstance();
        SharedState sharedState = app.getSharedState();
        sharedState.addStatusListener(app, statusListener);

        addStyleClass("notification-container");
    }

    private void addNotification(String text) {
        WTemplate notif = new WTemplate(WString.tr("notification"));
        notif.bindString("notification-text", text);

        WPushButton closeButton = new WPushButton();
        closeButton.clicked().addListener(this, () -> {
            removeWidget(notif);
        });
        closeButton.setText("X");
        closeButton.addStyleClass("close-button");

        notif.bindWidget("close-button", closeButton);
        addWidget(notif);
    }

    private void onNewStatus(StatusMessage message) {
        if (message.getType() == StatusMessage.MessageType.READ_OUTLIER) {
            addNotification(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toString()+ " - Outlier detected! " + message.getDetails());
        }
    }
}
