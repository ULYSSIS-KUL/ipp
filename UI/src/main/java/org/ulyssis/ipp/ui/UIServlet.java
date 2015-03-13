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
