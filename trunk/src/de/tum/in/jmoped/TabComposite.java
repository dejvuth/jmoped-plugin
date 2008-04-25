package de.tum.in.jmoped;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import de.tum.in.jmoped.translator.MethodArgument;

/**
 * Tab for jMoped's progress view.
 * 
 * @author suwimont
 *
 */
public class TabComposite extends Composite {
	
	private CTabFolder tab;
	
	private ArgumentComposite argument;
	private static final int ARGUMENT_INDEX = 0;

	public TabComposite(Composite parent, int style) {
		
		super(parent, style);
		
		setLayout(new FillLayout(SWT.VERTICAL));
		
		tab = new CTabFolder(this, SWT.TOP);
		
		CTabItem item = new CTabItem(tab, SWT.NONE);
		argument = new ArgumentComposite(tab, SWT.NONE);
		item.setControl(argument);
		item.setText("Arguments");
	}
	
	public void clearArguments() {
		argument.setArguments(null);
	}
	
	public void setArguments(MethodArgument[] args) {
		argument.setArguments(args);
		tab.setSelection(ARGUMENT_INDEX);
	}
}
