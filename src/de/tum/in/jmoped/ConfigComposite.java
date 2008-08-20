package de.tum.in.jmoped;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IMemento;

public class ConfigComposite extends Composite {
	
	private Spinner bitsSpinner;
	private Spinner heapSizeSpinner;
	
	private Spinner threadBoundSpinner;
	private Spinner contextSwitchBoundSpinner;
	
	private Button lazyCheckbox;
	private Button executeRemoplaCheckbox;

	public ConfigComposite(Composite parent, int style, IMemento memento) {
		
		super(parent, style);
		setLayout(new GridLayout(2, false));
		
		// Bits spinner
		new Label(this, SWT.HORIZONTAL).setText("Number of Bits: ");
		bitsSpinner = new Spinner(this, SWT.BORDER);
		bitsSpinner.setMinimum(1);
		bitsSpinner.setMaximum(32);
		bitsSpinner.setIncrement(1);
		bitsSpinner.setPageIncrement(2);
		bitsSpinner.setSelection(getValue(memento, ProgressView.BITS, 7));
		bitsSpinner.setLayoutData(new GridData(40, SWT.DEFAULT));
		
		// Heap size spinner
		new Label(this, SWT.HORIZONTAL).setText("Heap Size: ");
		heapSizeSpinner = new Spinner(this, SWT.BORDER);
		heapSizeSpinner.setMinimum(2);
		heapSizeSpinner.setMaximum(65535);
		heapSizeSpinner.setIncrement(1);
		heapSizeSpinner.setPageIncrement(2);
		heapSizeSpinner.setSelection(getValue(memento, ProgressView.HEAP_SIZE, 127));
		heapSizeSpinner.setLayoutData(new GridData(40, SWT.DEFAULT));
		
		// Synchrnizes the spinners
		new SpinnerSyncListener();
		
		// Thread bound spinner
		new Label(this, SWT.HORIZONTAL).setText("Thread Bound: ");
		threadBoundSpinner = new Spinner(this, SWT.BORDER);
		threadBoundSpinner.setMinimum(1);
		threadBoundSpinner.setMaximum(32);
		threadBoundSpinner.setIncrement(1);
		threadBoundSpinner.setPageIncrement(2);
		threadBoundSpinner.setSelection(getValue(memento, ProgressView.THREAD_BOUND, 1));
		threadBoundSpinner.setLayoutData(new GridData(40, SWT.DEFAULT));
//		threadBoundSpinner.setEnabled(multithreading);
		
		// Context-switch bound spinner
		new Label(this, SWT.HORIZONTAL).setText("Context Bound: ");
		contextSwitchBoundSpinner = new Spinner(this, SWT.BORDER);
		contextSwitchBoundSpinner.setMinimum(1);
		contextSwitchBoundSpinner.setMaximum(32);
		contextSwitchBoundSpinner.setIncrement(1);
		contextSwitchBoundSpinner.setPageIncrement(2);
		contextSwitchBoundSpinner.setSelection(getValue(
				memento, ProgressView.CONTEXT_SWITCH_BOUND, 2));
		contextSwitchBoundSpinner.setLayoutData(new GridData(40, SWT.DEFAULT));
		contextSwitchBoundSpinner.setEnabled(threadBoundSpinner.getSelection() > 1);
		
		// Checkbox: Lazy
		lazyCheckbox = createCheckbox(this, memento, 
				"Lazy splitting", ProgressView.LAZY, false);
		lazyCheckbox.setEnabled(threadBoundSpinner.getSelection() > 1);
		
		threadBoundSpinner.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				boolean multithreading = multithreading() ? true : false;
				contextSwitchBoundSpinner.setEnabled(multithreading);
				lazyCheckbox.setEnabled(multithreading);
			}
			
		});
		
		
		
		// Checkbox: Execute remopla
		executeRemoplaCheckbox = createCheckbox(this, memento, 
				"Execute Remopla", ProgressView.EXECUTE_REMOPLA, false);
		executeRemoplaCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
	
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = !executeRemopla();
				heapSizeSpinner.setEnabled(enabled);
				threadBoundSpinner.setEnabled(enabled);
				contextSwitchBoundSpinner.setEnabled(enabled && multithreading());
				lazyCheckbox.setEnabled(enabled && multithreading());
			}
		});
		
		if (executeRemopla()) {
			heapSizeSpinner.setEnabled(false);
			threadBoundSpinner.setEnabled(false);
			contextSwitchBoundSpinner.setEnabled(false);
			lazyCheckbox.setEnabled(false);
		}
	}
	
	public int getBits() {
		
		return bitsSpinner.getSelection();
	}
	
	public int getHeapSize() {
		
		return heapSizeSpinner.getSelection();
	}
	
	/**
	 * Returns <code>true</code> if the checkbox "execute remopla" is selected;
	 * or <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> iff the checkbox "execute remopla" is selected.
	 */
	public boolean executeRemopla() {
		return executeRemoplaCheckbox.getSelection();
	}
	
	/**
	 * Returns <code>true</code> if the checkbox "Multithreading" is seletected;
	 * or <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> iff the checkbox "Multithreading" is seletected.
	 */
	public boolean multithreading() {
		return getThreadBound() > 1;
	}
	
	public int getThreadBound() {
		return threadBoundSpinner.getSelection();
	}
	
	public int getContextSwitchBound() {
		return contextSwitchBoundSpinner.getSelection();
	}
	
	public boolean lazy() {
		return lazyCheckbox.getSelection();
	}
	
	private static Button createCheckbox(Composite parent, IMemento memento,
			String label, String key, boolean value) {
		
		Button checkbox = new Button(parent, SWT.CHECK);
		checkbox.setText(label);
		checkbox.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));
		if (memento != null) {
			Integer i = memento.getInteger(key);
			if (i != null) value = (i.intValue() == 1) ? true : false;
		}
		checkbox.setSelection(value);
		
		return checkbox;
	}
	
	/**
	 * Gets the integer value specified by <code>key</code> from the 
	 * <code>memento</code>. If not found, the default <code>value</code>
	 * is returned.
	 * 
	 * @param memento the memento.
	 * @param key the key.
	 * @param value the default value.
	 * @return the integer value of the key.
	 */
	private static int getValue(IMemento memento, String key, int value) {
		
		if (memento == null) return value;
		
		Integer i = memento.getInteger(key);
		return (i != null) ? i.intValue() : value;
	}
	
	private class SpinnerSyncListener implements ModifyListener, 
			SelectionListener, DisposeListener {
		
		public SpinnerSyncListener() {
			bitsSpinner.addModifyListener(this);
			bitsSpinner.addSelectionListener(this);
			bitsSpinner.addDisposeListener(this);
			
			heapSizeSpinner.addModifyListener(this);
			heapSizeSpinner.addSelectionListener(this);
			heapSizeSpinner.addDisposeListener(this);
		}
		
		private void modified(TypedEvent event) throws CoreException {
			if (event.getSource() == bitsSpinner) {
				int bits = bitsSpinner.getSelection();
				
				int maxHeapSize = (int) Math.pow(2, bits) - 1;
				if (maxHeapSize < heapSizeSpinner.getSelection()) {
					heapSizeSpinner.setSelection(maxHeapSize);
				}
			}
			else {
				int heapSize = heapSizeSpinner.getSelection();
				
				int maxBits = (int) Math.ceil(Math.log(heapSize + 1)/Math.log(2.0));
				if (maxBits > bitsSpinner.getSelection()) {
					bitsSpinner.setSelection(maxBits);
				}
			}
		}
		
		public void modifyText(ModifyEvent e) {
			try {
				modified(e);
			} catch (CoreException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	
		public void widgetSelected(SelectionEvent e) {
			try {
				modified(e);
			} catch (CoreException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	
		public void widgetDisposed(DisposeEvent e) {
			if (bitsSpinner != null && heapSizeSpinner != null) {
				bitsSpinner.removeModifyListener(this);
				bitsSpinner.removeSelectionListener(this);
				bitsSpinner.removeDisposeListener(this);
				
				heapSizeSpinner.removeModifyListener(this);
				heapSizeSpinner.removeSelectionListener(this);
				heapSizeSpinner.removeDisposeListener(this);
				
				bitsSpinner = heapSizeSpinner = null;
			}
		}
	}
}
