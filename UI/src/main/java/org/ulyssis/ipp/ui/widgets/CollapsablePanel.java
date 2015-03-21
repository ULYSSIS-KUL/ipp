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

import eu.webtoolkit.jwt.Icon;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WWidget;

import java.util.Objects;

public abstract class CollapsablePanel extends WTemplate {
    public enum State {
        Collapsed,
        Extended
    }

    private State state = State.Collapsed;
    private final WTemplate bar;
    private WContainerWidget openClose;
    private Icon openCloseIcon;

    public CollapsablePanel() {
        this(null);
    }

    public CollapsablePanel(WContainerWidget parent) {
        super(WString.tr("collapsable-panel"), parent);

        setStyleClass("collapsable-panel");
        addStyleClass("closed");
        
        bar = new WTemplate(WString.tr("collapsable-panel-bar")) {
        	@Override
        	public WWidget resolveWidget(String varName) {
        		if (Objects.equals(varName, "bar-content")) {
        			barContentWidget().addStyleClass("bar-content");
        			return barContentWidget();
        		} else {
        			return super.resolveWidget(varName);
        		}
        	}
        };
        bar.addStyleClass("collapsable-panel-bar");
        bindWidget("bar", bar);

        openClose = new WContainerWidget();
        openClose.setStyleClass("open-close");
        openCloseIcon = new Icon("plus", openClose);
        bar.bindWidget("open-close", openClose);

        bar.clicked().addListener(this, this::toggleOpenClosed);
    }

    @Override
    public WWidget resolveWidget(String varName) {
        if (Objects.equals(varName, "content")) {
            return contentWidget();
        } else {
            return super.resolveWidget(varName);
        }
    }

    protected abstract WWidget barContentWidget();
    protected abstract WWidget contentWidget();
    
    public State getState() {
    	return state;
    }

    protected void toggleOpenClosed() {
        if (state == State.Collapsed) {
            state = State.Extended;
            openCloseIcon.setName("minus");
            removeStyleClass("closed");
            addStyleClass("open");
        } else if (state == State.Extended) {
            state = State.Collapsed;
            openCloseIcon.setName("plus");
            removeStyleClass("open");
            addStyleClass("closed");
        }
    }
}
