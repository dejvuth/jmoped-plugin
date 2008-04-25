package de.tum.in.jmoped;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "de.tum.in.jmoped";

	// The shared instance
	private static Activator plugin;
	
	private static URL iconsURL;
	
	/**
	 * The constructor
	 */
	public Activator() {
		try {
			iconsURL = new URL(Platform.getBundle(PLUGIN_ID).getEntry("/"), "icons/");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		CoverageLaunchShortcut.free();
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	public static void error(Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, 0, e.getMessage(), e));
	}
	
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		
		if (plugin == null) return null;
		
		IWorkbench workbench = plugin.getWorkbench();
		if (workbench == null) return null;
		
		return workbench.getActiveWorkbenchWindow();
	}
	
	public static Shell getActiveWorkbenchShell() {
		
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window == null) return null;
		return window.getShell();
	}
	
	/**
	 * Gets the active workbench page from the active workbemch window.
	 * 
	 * @return
	 */
	public static IWorkbenchPage getActivePage() {
		
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window == null) return null;
		
		return window.getActivePage();
	}
	
	public static void showProgressView() throws PartInitException {
		
		IWorkbenchPage page = getActivePage();
		if (page == null) return;
		
		page.showView(ProgressView.NAME);
	}
	
	public static void info(String title, String message) {
		
		MessageDialog.openInformation(getActiveWorkbenchWindow().getShell(), title, message);
	}

	/**
	 * Finds the progress view. The method opens a new view if not yet opened.
	 * 
	 * @return the progress view.
	 */
	public static ProgressView findProgressView() {
		
		IWorkbenchPage page = getActivePage();
		if (page == null ) return null;
		
		ProgressView view = (ProgressView) page.findView(ProgressView.NAME);
		if (view != null) return view;
		
		try {
			return (ProgressView) page.showView(ProgressView.NAME);
		} catch (PartInitException e) {
			return null;
		}
	}
	
	public static Display getDisplay() {
		
		Display display = Display.getCurrent();
		if (display == null) display = Display.getDefault();
		
		return display;
	}
	
	/**
	 * Gets the image descriptor of the image specified by <code>name</code>.
	 * 
	 * @param name the name of the image.
	 * @return the image descriptor.
	 */
	public static ImageDescriptor getImageDescriptor(String name) {
		return getImageDescriptor(null, name);
	}
	
	/**
	 * Gets the image descriptor of the image specified by <code>name</code>
	 * and <code>prefix</code>.
	 * 
	 * @param prefix the prefix of the image.
	 * @param name the name of the image.
	 * @return the image descriptor.
	 */
	public static ImageDescriptor getImageDescriptor(String prefix, String name) {
		try {
			return ImageDescriptor.createFromURL(getImageURL(prefix, name));
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}
	
	private static URL getImageURL(String prefix, String name) throws MalformedURLException {
		if (iconsURL == null)
			throw new MalformedURLException();
		return new URL(iconsURL, (prefix != null) ? prefix + name : name);
	}
}
