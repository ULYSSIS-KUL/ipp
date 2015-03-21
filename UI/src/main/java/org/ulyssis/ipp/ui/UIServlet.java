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

import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WtServlet;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.ui.state.SharedState;

import javax.servlet.ServletException;
import java.nio.file.Paths;

public class UIServlet extends WtServlet {
	private static final long serialVersionUID = 1L;

    private SharedState sharedState;

	@Override
    public WApplication createApplication(WEnvironment wEnvironment) {
        return new UIApplication(sharedState, wEnvironment);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        Config.setCurrentConfig(Config.fromConfigurationFile(Paths.get("config.json")).get());
        sharedState = new SharedState();
    }

    @Override
    public void destroy() {
        sharedState.stop();
        super.destroy();
    }
}
