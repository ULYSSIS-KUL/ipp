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

import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTemplate;

public class SafetyButton extends WTemplate {
	private final WCheckBox safety;
	private final WPushButton button;
	
	public SafetyButton(String label) {
		this(label, null);
	}
	
	public SafetyButton(String label, WContainerWidget parent) {
		super(WString.tr("safety-button"), parent);
		
		safety = new WCheckBox("Safety");
		bindWidget("safety", safety);
		
		safety.setChecked(true);
		
		button = new WPushButton(label);
		bindWidget("button", button);
		
		button.setEnabled(false);
		
		safety.clicked().addListener(this, () -> {
			if (safety.isChecked()) {
				button.setEnabled(false);
			} else {
				button.setEnabled(true);
			}
		});
		
		button.clicked().addListener(this, () -> {
			safety.setChecked(true);
			button.setEnabled(false);
		});
	}
	
	public WPushButton getButton() {
		return button;
	}
}
