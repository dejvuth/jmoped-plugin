package de.tum.in.jmoped;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import de.tum.in.jmoped.translator.MethodArgument;
import de.tum.in.jmoped.underbone.ProgressMonitor;
import de.tum.in.jmoped.underbone.VarManager;

/**
 * The jMoped's progress view.
 * 
 * @author suwimont
 *
 */
public class ProgressView extends ViewPart {
	
	/**
	 * The name of the view.
	 */
	public static final String NAME = "de.tum.in.jmoped.ProgressView";
	
	/**
	 * The progress monitor.
	 */
	private ProgressMonitor monitor;
	
	/**
	 * The stop action.
	 */
	private Action stopAction;
	
	/**
	 * The rerun action.
	 */
	private Action rerunAction;
	
	/**
	 * The title label.
	 */
	private Label titleLabel;
	
	/**
	 * The progress label.
	 */
	private Label progressLabel;
	
	/**
	 * The progress bar.
	 */
	private ProgressBar progressBar;
	
	/**
	 * The group of parameters.
	 */
	private Group paramGroup;
	
	/**
	 * The composite of configurations.
	 */
	private ConfigComposite config;
	
	/**
	 * The tab: Arguments
	 */
	private TabComposite tab;
	
	/**
	 * The memento
	 */
	private IMemento memento;
	
	static final String BITS = NAME + ".BITS";
	static final String HEAP_SIZE = NAME + ".HEAP_SIZE";
	static final String EXECUTE_REMOPLA = NAME + ".EXECUTE_REMOPLA";
	static final String THREAD_BOUND = NAME + ".THREAD_BOUND";
	static final String CONTEXT_SWITCH_BOUND = NAME + ".CONTEXT_SWTICH_BOUND";
	static final String SYMBOLIC = NAME + ".SYMBOLIC";

	@Override
	public void createPartControl(Composite parent) {
		
		monitor = new CoverageMonitor();
		
		rerunAction = new RerunAction();
		getViewSite().getActionBars().getToolBarManager().add(rerunAction);
		
		stopAction = new StopAction();
		getViewSite().getActionBars().getToolBarManager().add(stopAction);
		
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		
		// Labels
		titleLabel = createLabel(parent);
		progressLabel = createLabel(parent);
		
		// Progress bar
		progressBar = new ProgressBar(parent, SWT.HORIZONTAL);
		progressBar.setEnabled(false);
		progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		// Parameter group
		paramGroup = new Group(parent, SWT.NONE);
		paramGroup.setLayout(new GridLayout(2, false));
		paramGroup.setText("Parameters");
		paramGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		// Config controller
		config = new ConfigComposite(paramGroup, SWT.NONE, memento);
		config.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 2, 1));
		
		// Tab
		tab = new TabComposite(parent, SWT.NONE);
		tab.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}
	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		
		this.memento = memento;
		super.init(site, memento);
	}
	
	public void saveState(IMemento memento) {
		
		if (isCreated()) {
			memento.putInteger(BITS, config.getBits());
			memento.putInteger(HEAP_SIZE, config.getHeapSize());
			memento.putInteger(EXECUTE_REMOPLA, config.executeRemopla() ? 1 : 0);
			memento.putInteger(THREAD_BOUND, config.getThreadBound());
			memento.putInteger(CONTEXT_SWITCH_BOUND, config.getContextSwitchBound());
			memento.putInteger(SYMBOLIC, config.symbolic() ? 1 : 0);
		}
		
		super.saveState(memento);
	}
	
	/**
	 * Gets the progress monitor.
	 * 
	 * @return the progress monitor.
	 */
	public ProgressMonitor getProgressMonitor() {
		
		return monitor;
	}
	
	/**
	 * Gets the config controller.
	 * 
	 * @return the config controller.
	 */
	public ConfigComposite getConfig() {
		
		return config;
	}
	
	public void setArguments(MethodArgument[] args) {
		tab.setArguments(args);
	}
	
	private static class ProgressViewGetter implements Runnable {
		
		public ProgressView view;
		
		public void run() {
			view = Activator.findProgressView();
		}
	}
	
	/**
	 * Returns the displayed progress view instance.
	 * 
	 * @return the progress view instance.
	 */
	public static ProgressView getInstance() {
		
		ProgressViewGetter r = new ProgressViewGetter();
		Activator.getDisplay().syncExec(r);
		
		return r.view;
	}
	
	/**
	 * Returns <code>true</code> if the progress view is already created.
	 * 
	 * @return <code>true</code> if the progress view is already created.
	 */
	public boolean isCreated() {
		
		return progressBar != null;
	}

	private static Label createLabel(Composite parent) {
		
		Label label = new Label(parent, SWT.HORIZONTAL);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return label;
	}
	
	public static ImageDescriptor getEnabledImageDescriptor(String name) {
		
		return getImageDescriptor("enabled/", name);
	}
	
	public static ImageDescriptor getDisabledImageDescriptor(String name) {
		
		return getImageDescriptor("disabled/", name);
	}
	
	public static ImageDescriptor getHoverImageDescriptor(String name) {
		
		return getImageDescriptor("hover/", name);
	}
	
	private static ImageDescriptor getImageDescriptor(String prefix, String name) {
		
		try {
			return ImageDescriptor.createFromURL(getImageURL(prefix, name));
		}
		catch (MalformedURLException mue) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}
	
	private static URL baseURL;
	
	private static URL getImageURL(String prefix, String name) 
		throws MalformedURLException {
		
		if (baseURL == null) {
			baseURL = new URL(Platform.getBundle(Activator.PLUGIN_ID).getEntry("/"), "icons/");
			if (baseURL == null) throw new MalformedURLException();
		}
		return new URL(baseURL, prefix != null ? prefix + name : name);
	}
	
	private void dispatch(final Runnable r) {
		
		if (!progressBar.isDisposed()) {
			
			progressBar.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!progressBar.isDisposed())
						r.run();
				}
			});
		}
	}

	/**
	 * A graphical monitor.
	 * 
	 * @author suwimont
	 *
	 */
	private class CoverageMonitor implements ProgressMonitor {
		
		private boolean canceled;
		private long startTime;
		
		public void beginTask(final String name, final int totalWork) {
			
			canceled = false;
			dispatch(new Runnable() {
				public void run() {
					titleLabel.setText(name);
					progressLabel.setText("");
					progressBar.setEnabled(true);
					progressBar.setMaximum(totalWork);
					progressBar.setSelection(0);
					rerunAction.setEnabled(false);
					stopAction.setEnabled(true);
					tab.clearArguments();
					startTime = System.currentTimeMillis();
				}
			});
		}
		
		public void done() {
			
			dispatch(new Runnable() {
				public void run() {
					float elapsed = (float) ((System.currentTimeMillis() - startTime) / 1000.0);
					if (isCanceled()) {
						progressLabel.setText(String.format("Canceled. %.2fs", elapsed));
					} else {
						progressLabel.setText(String.format("Finished. %.2fs", elapsed));
						progressBar.setSelection(progressBar.getMaximum());
					}
					CoverageLaunchShortcut.info("Max BDD: %.2f * 10^3 nodes%n", 
							((float) VarManager.getMaxNodeNum())/1000.0);
					CoverageLaunchShortcut.info("Time: %.2fs%n%n", elapsed);
					rerunAction.setEnabled(true);
					stopAction.setEnabled(false);
				}
			});
		}
		
		public void worked(final int work) {
			
			dispatch(new Runnable() {
				public void run() {
					progressBar.setSelection(progressBar.getSelection() + work);
				}
			});
		}
		
		public void subTask(final String name) {
			
			dispatch(new Runnable() {
				public void run() {
					progressLabel.setText(name);
				}
			});
		}

		public boolean isCanceled() {
			
			return canceled;
		}

		public void setCanceled(boolean value) {
			
			canceled = value;
		}
	}
	
	private class StopAction extends Action {
		
		public StopAction() {
			
			setText("Stop");
			setToolTipText("Stop jMoped");
			setImageDescriptor(ProgressView.getEnabledImageDescriptor("stop.gif"));
			setDisabledImageDescriptor(ProgressView.getDisabledImageDescriptor("stop.gif"));
			setHoverImageDescriptor(ProgressView.getHoverImageDescriptor("stop.gif"));
			setEnabled(false);
		}
		
		public void run() {
			
			monitor.setCanceled(true);
		}
	}
	
	private class RerunAction extends Action {
		
		public RerunAction() {
			
			setText("Rerun");
			setToolTipText("Rerun jMoped");
			setImageDescriptor(ProgressView.getEnabledImageDescriptor("relaunch.gif"));
			setDisabledImageDescriptor(ProgressView.getDisabledImageDescriptor("relaunch.gif"));
			setEnabled(false);
		}
		
		public void run() {
			
			CoverageLaunchShortcut.launch();
		}
	}
}
