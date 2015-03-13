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
