package de.tum.in.jmoped;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Unused
 * 
 * @author suwimont
 *
 */
public class CoverageLaunchConfigurationMainTab extends
		AbstractLaunchConfigurationTab {

//	private LaunchConfigurationControl configControl;
	
	public void createControl(Composite parent) {
		
//		Composite top = new Composite(parent, SWT.NONE);
//		top.setLayout(new FillLayout());
//		setControl(top);
		
		
	}

	public String getName() {
		
		return "jMoped";
	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		// TODO Auto-generated method stub

	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub

	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub

	}

}
