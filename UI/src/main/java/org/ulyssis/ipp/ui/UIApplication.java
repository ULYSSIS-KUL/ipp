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
package org.ulyssis.ipp.ui;

import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.config.Team;
import org.ulyssis.ipp.ui.state.SharedState;
import org.ulyssis.ipp.ui.widgets.ContestControlPanel;
import org.ulyssis.ipp.ui.widgets.HeaderWidget;
import org.ulyssis.ipp.ui.widgets.LogPanel;
import org.ulyssis.ipp.ui.widgets.TeamPanel;

import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WXmlLocalizedStrings;

public class UIApplication extends WApplication {
    private final SharedState sharedState;

    public UIApplication(SharedState sharedState, WEnvironment env) {
        super(env);

        this.sharedState = sharedState;

        WXmlLocalizedStrings xmlLocalizedStrings = new WXmlLocalizedStrings();
        xmlLocalizedStrings.use("UI/src/main/resources/template");
        setLocalizedStrings(xmlLocalizedStrings);

        getRoot().setStyleClass("container");

        useStyleSheet(new WLink("/style.css"));

        new HeaderWidget(getRoot());
        new ContestControlPanel(getRoot());
        new LogPanel(getRoot());
        for (Team team : Config.getCurrentConfig().getTeams()) {
            new TeamPanel(team, getRoot());
        }

        enableUpdates(true);
    }

    public SharedState getSharedState() {
        return sharedState;
    }

    public static UIApplication getInstance() {
        return (UIApplication) WApplication.getInstance();
    }

    @Override
    public void destroy() {
    	sharedState.removeApplication(this);
        super.destroy();
    }
}
