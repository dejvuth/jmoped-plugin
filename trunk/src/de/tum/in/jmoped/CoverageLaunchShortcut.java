package de.tum.in.jmoped;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

import de.tum.in.jmoped.marker.MarkerManager;
import de.tum.in.jmoped.translator.Translator;
import de.tum.in.jmoped.underbone.ProgressMonitor;
import de.tum.in.jmoped.underbone.Remopla;
import de.tum.in.wpds.Sat;
import de.tum.in.wpds.Utils;

/**
 * The analysis starts here.
 * 
 * @author suwimont
 *
 */
public class CoverageLaunchShortcut implements ILaunchShortcut {
	
	/**
	 * Verbosity level.
	 */
	private static int verbosity = 0;
	
	/**
	 * The logger.
	 */
	private static Logger logger = Utils.getLogger(CoverageLaunchShortcut.class, 
			"%t/CoverageLaunchShortcut%g.log");
	
	private static IMethod method;
	private static Remopla remopla;
	private static Translator translator;
	
	/**
	 * Handles error <code>e</code>.
	 * 
	 * @param e the error.
	 */
	private static void handleError(Throwable e) {
		Activator.error(e);
		String msg = e.getMessage();
		if (msg == null) {
			if (e.getClass().getName().equals(e.toString()))
				msg = e.toString();
			else
				msg = String.format("%s: %s", 
						e.getClass().getName(), e.toString());
		}
		try {
			MessageDialog.openError(Activator.getActiveWorkbenchShell(), "jMoped Error", msg);
		} catch (NullPointerException npe) {
			System.err.println(e);
		}
		
		if (!(e instanceof OutOfMemoryError) && remopla != null) {
			remopla.free();
			remopla = null;
		}
	}
	
	/**
	 * Gets the method of the previous analysis.
	 * 
	 * @return the method of the previous analysis.
	 */
	public static IMethod getLastMethod() {
		return method;
	}
	
	/**
	 * Gets the remopla of the previous analysis.
	 * 
	 * @return the remopla of the previous analysis.
	 */
	public static Remopla getLastRemopla() {
		return remopla;
	}
	
	/**
	 * Gets the translator of the previous analysis.
	 * 
	 * @return the translator of the previous analysis.
	 */
	public static Translator getLastTranslator() {
		return translator;
	}
	
	/**
	 * Frees the resources used in the previous analysis.
	 */
	static void free() {
		if (remopla == null) return;
		remopla.free();
		remopla = null;
		translator = null;
		System.gc();
	}
	
	/**
	 * Launches the analysis.
	 */
	static void launch() {
		
		try {
			free();
			Activator.showProgressView();
			
			// Reads preferences
			IPreferenceStore pref = Activator.getDefault().getPreferenceStore();
			int verbose = pref.getInt(Preference.VERBOSITY);
			Sat.setVerbosity(verbose);
			Translator.setVerbosity(verbose);
			Remopla.setVerbosity(verbose);
			
			IJavaProject project = method.getJavaProject();
			String location = project.getResource().getLocation().toOSString();
			
			// Creates translator
			String className = method.getDeclaringType().getFullyQualifiedName().replace('.', '/');
			URL liburl = FileLocator.find(
					Platform.getBundle(Activator.PLUGIN_ID), 
					new Path("lib" + File.separator + "translator.jar"), null);
			translator = new Translator(
					className,
					new String[] { location, location + "/bin", FileLocator.resolve(liburl).toURI().getRawPath() },
					method.isConstructor() ? "<init>" : method.getElementName(),
					method.getSignature());
			
			// Gets the configs (bits and heap size)
			ProgressView view = ProgressView.getInstance();
			ConfigComposite config = view.getConfig();
			
			// Creates Remopla
			final boolean executeRemopla = config.executeRemopla();
			final int threadBound = config.getThreadBound();
			remopla = translator.translate(config.getBits(), 
					config.getHeapSize(), !executeRemopla, threadBound, config.symbolic());
			log("Bits: %d, Heap Size: %d%n", config.getBits(), config.getHeapSize());
			log(remopla.toString().replace("%", "%%"));
			
			// Creates coverage listener
			CoverageListener listener = new CoverageListener(translator, pref.getBoolean(Preference.STOP_AFTER_ERROR));
			listener.addResource(translator.getInitClassName(), method.getResource());
			remopla.setListener(listener);

			System.gc();
			
			// Creates progress monitor
			final ProgressMonitor monitor = view.getProgressMonitor();
			
			// Runs the analysis
			final int contextSwitchBound = config.getContextSwitchBound();
			CoverageRunner r = new CoverageRunner(
					pref.getString(Preference.BDDPACKAGE),
					pref.getInt(Preference.NODENUM),
					pref.getInt(Preference.CACHESIZE),
					executeRemopla, threadBound,
					contextSwitchBound, config.symbolic(), monitor);
			Thread t = new Thread(r);
			t.start();
			
		} catch (Throwable e) {
			handleError(e);
		}
	}
	
	private static class CoverageRunner implements Runnable {
		
		String bddpackage;
		int nodenum;
		int cachesize;
		boolean executeRemopla;
		int threadBound;
		int contextSwitchBound;
		boolean symbolic;
		ProgressMonitor monitor;
		
		public CoverageRunner(String bddpackage, int nodenum, int cachesize, 
				boolean executeRemopla, int threadBound, 
				int contextSwitchBound, boolean symbolic, ProgressMonitor monitor) {
			this.bddpackage = bddpackage;
			this.nodenum = nodenum;
			this.cachesize = cachesize;
			this.executeRemopla = executeRemopla;
			this.threadBound = threadBound;
			this.contextSwitchBound = contextSwitchBound;
			this.symbolic = symbolic;
			this.monitor = monitor;
		}
		
		public void run() {
			try {
				long startTime = System.nanoTime();
				if (executeRemopla)
					remopla.run(monitor);
				else if (threadBound == 1)
					remopla.coverage(bddpackage, nodenum, cachesize, monitor);
				else {
					remopla.coverage(bddpackage, nodenum, cachesize, threadBound, contextSwitchBound, symbolic, monitor);
				}
				long estimatedTime = System.nanoTime() - startTime;
				double time = ((double) estimatedTime) / Math.pow(10, 9);
				log("Time: %.2f seconds%n", time);
			} catch (Throwable e) {
				handleError(e);
			} finally {
				System.gc();
			}
		}
	}

	public void launch(ISelection selection, String mode) {
		// TODO Auto-generated method stub

	}

	/**
	 * Looks for the selected method from the editor and launches the analysis.
	 * 
	 * @param editor the selected editor.
	 * @param mode ignored.
	 */
	public void launch(IEditorPart editor, String mode) {
		
		ISelection selection = editor.getSite().getPage().getSelection();
		if (selection instanceof ITextSelection) {
			method = SearchUtils.getMethod(editor, (ITextSelection) selection);
			launch();
		} else {
			System.err.println("Unsupported implementation: selection not instanceof ITextSelection");
			MessageDialog.openError(null, "jMoped Error", "Unsupported operation");
		}
	}
	
	/**
	 * Logs translator information.
	 * 
	 * @param msg
	 * @param args
	 */
	public static void log(String msg, Object... args) {
		log(2, msg, args);
	}
	
	/**
	 * Logs translator information.
	 * 
	 * @param msg
	 * @param args
	 */
	public static void info(String msg, Object... args) {
		log(1, msg, args);
	}
	
	private static void log(int threshold, String msg, Object... args) {
		if (verbosity >= threshold)
			logger.fine(String.format(msg, args));
	}
	
	@SuppressWarnings("unused")
	private ProgressMonitor getProgressMonitor() {
		
		ProgressView view = ProgressView.getInstance();
		if (view == null) return null;
		
		return view.getProgressMonitor();
	}

	@SuppressWarnings("unused")
	private void testMarker(IEditorPart editor) {
		
		ISelection selection = editor.getSite().getPage().getSelection();
		ITextSelection txtSel = (ITextSelection) selection;
		IPath path = new Path("/Users/suwimont/Documents/runtime-EclipseApplication/examples/src/test/Test1.java");
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IFile file = root.getFileForLocation(path);
		try {
			
			file.deleteMarkers(MarkerManager.COVERED_MARKER_ID, false, IResource.DEPTH_INFINITE);
			IMarker marker = file.createMarker(MarkerManager.COVERED_MARKER_ID);
			marker.setAttribute(IMarker.LINE_NUMBER, txtSel.getStartLine()+1);
			marker.setAttribute(IMarker.MESSAGE, "A sample marker message");
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
			marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
			System.out.println(txtSel.getStartLine()+1);
		} catch (CoreException e) {
			
			e.printStackTrace();
		}
	}
}
