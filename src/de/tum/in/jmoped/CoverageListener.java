package de.tum.in.jmoped;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.gjt.jclasslib.structures.ClassFile;
import org.gjt.jclasslib.structures.InvalidByteCodeException;
import org.gjt.jclasslib.structures.attributes.SourceFileAttribute;

import de.tum.in.jmoped.marker.MarkerManager;
import de.tum.in.jmoped.translator.Translator;
import de.tum.in.jmoped.translator.TranslatorUtils;
import de.tum.in.jmoped.underbone.ProgressMonitor;
import de.tum.in.jmoped.underbone.RemoplaListener;

/**
 * A coverage listener.
 * 
 * @author suwimont
 *
 */
public class CoverageListener implements RemoplaListener {

	/**
	 * The translator.
	 */
	private Translator translator;
	
	/**
	 * Maps class names to their resources.
	 */
	private HashMap<String, IResource> resources = new HashMap<String, IResource>();
	
	/**
	 * The progress monitors.
	 */
	private ProgressMonitor monitor;
	
	/**
	 * All labels to be tested.
	 */
	private Set<String> labels;
	
	/**
	 * Maps class name to its marker info
	 */
	private HashMap<String, MarkerInfo> marked = new HashMap<String, MarkerInfo>();
	
	/**
	 * Determines whether to stop the analysis when an error is found.
	 */
	private boolean stopAfterError;
	
	/**
	 * The constructor.
	 * 
	 * @param translator the constructor.
	 */
	public CoverageListener(Translator translator, boolean stopAfterError) {
		this.translator = translator;
		this.stopAfterError = stopAfterError;
	}
	
	/**
	 * Wraps the progress monitor.
	 * 
	 * @param monitor the progress monitor.
	 */
	public void setProgressMonitor(ProgressMonitor monitor) {
		this.monitor = monitor;
	}
	
	/**
	 * Starts the listener.
	 * 
	 * @param labels the labels to be tested.
	 * @see ProgressMonitor#beginTask(String, int).
	 */
	public void beginTask(String name, Set<String> labels) {
		MarkerManager.init();
		this.labels = labels;
		monitor.beginTask(name, labels.size());
		info("After first post*: %d labels%n", labels.size());
	}
	
	/**
	 * Ends the listener.
	 * 
	 * @see ProgressMonitor#done().
	 */
	public void done() {
		
		monitor.done();
		info("Remaining: %d labels%n", labels.size());
		
		// Marks the unreached labels
		for (String label : labels) {
			
			// Gets the class name
			String className = TranslatorUtils.extractClassName(label);
			if (className == null) continue;
			
			// Gets the source line
			int line = translator.getSourceLine(label);
			
			// Bypasses the marked line
			MarkerInfo minfo = marked.get(className);
			if (minfo != null && minfo.isMarked(line))
				continue;
			
			// Marks as not covered
			if (minfo == null) {
				minfo = new MarkerInfo();
				marked.put(className, minfo);
			}
			minfo.markAsNotCovered(line);
			mark(className, MarkerManager.NOT_COVERED_MARKER_ID, line, "Not covered", label);
		}
	}
	
	/**
	 * Stores the resource for the class specified by className.
	 * 
	 * @param className the class name.
	 * @param resource the resource.
	 */
	public void addResource(String className, IResource resource) {
		resources.put(className, resource);
	}
	
	/**
	 * Gets the resource of the class specified by className.
	 * 
	 * @param className the class name.
	 * @return the resource.
	 * @throws CoreException
	 * @throws InvalidByteCodeException
	 */
	public IResource getResource(String className) throws CoreException, InvalidByteCodeException {
		
		// Retrieves the resource, if any
		if (resources.containsKey(className))
			return resources.get(className);
		
		// Finds source file, returns null if not found
//		String s = SearchUtils.findSource(
//				ResourcesPlugin.getWorkspace().getRoot().members(), 
//				className + ".java");
//		if (s == null) {
//			System.out.printf("Cannot find source code of class: %s%n", className);
//			addResource(className, null);
//			return null;
//		}
//		
//		IPath path = new Path(s);
//		IWorkspace workspace = ResourcesPlugin.getWorkspace();
//		IWorkspaceRoot root = workspace.getRoot();
//		resource = root.getFileForLocation(path);
		
		IResource resource = SearchUtils.findResource(className);
		if (resource == null) {
			
			// Tries finding source file name from the class file
			log("Resouce for %s not found%n", className);
			ClassFile cf = translator.getClassTranslator(className).getClassFile();
			SourceFileAttribute attr = (SourceFileAttribute) cf.findAttribute(SourceFileAttribute.class);
			if (attr != null) {
				String source = cf.getConstantPoolUtf8Entry(attr.getSourcefileIndex())
						.getString();
				log("Source attribute found: %s%n", source);
				String sourceClassName = source.substring(0, source.length() - 5);
				log("Trying with %s%n", sourceClassName);
				resource = SearchUtils.findResource(sourceClassName);
				
				// Guesses once more
				if (resource == null) {
					sourceClassName = className.substring(0, className.lastIndexOf('/')) 
							+ '/' + sourceClassName;
					log("Trying with %s%n", sourceClassName);
					resource = SearchUtils.findResource(sourceClassName);
				}
			}
			if (resource == null)
				log("Cannot find source code for class: %s, Giving up!%n%n", className);
		}
		addResource(className, resource);
		
		return resource;
	}
	
	/**
	 * Puts the <code>type</code>d marker of the class specified by the
	 * <code>className</code> in front of the <code>line</code>.
	 * The marker has <code>msg</code> as the message ({@link IMarker#MESSAGE}), 
	 * and <code>label</code> as the label ({@link MarkerManager#LABEL}).
	 * 
	 * @param className the class name.
	 * @param type the marker type.
	 * @param line the line number.
	 * @param msg the message.
	 * @param label the label.
	 */
	public void mark(String className, String type, int line, String msg, String label) {
		
		IResource resource;
		try {
			resource = getResource(className);
			if (resource == null) return;
			
			MarkerManager.mark(resource, className, type, line, msg, label);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Deletes the marker specified by the class name, type, and line.
	 * 
	 * @param className the class name
	 * @param type the marker type
	 * @param line the source line
	 */
	public void unmark(String className, String type, int line) {
		
		try {
			IResource resource = getResource(className);
			if (resource == null) return;
			MarkerManager.unmark(resource, className, type, line);
		} catch (Exception e) {
			System.err.printf("Error while unmarking %s with %s at line %d%n",
					className, type, line);
			e.printStackTrace();
		}
	}
	
	public void reach(String label) {
		
		// Removes label from the set
		if (labels.remove(label)) monitor.worked(1);
		
		// Gets the class name from the label
		String className = TranslatorUtils.extractClassName(label);
		if (className == null) return;
		
		// Gets the source line
		int line = translator.getSourceLine(label);
		
		// Gets the marker info
		MarkerInfo minfo = marked.get(className);
		if (minfo == null) { 
			minfo = new MarkerInfo();
			marked.put(className, minfo);
		}
		
		// In case of assertion label
		if (TranslatorUtils.isAssertionName(label)) {
			
			// Bypasses if already marked as assertion failed
			if (minfo.isMarkedAsAssertionFailed(line)) return;
			
			// Unmarks the existing covered marker (if any)
			unmark(className, MarkerManager.COVERED_MARKER_ID, line);
			
			// Marks as assertion failed
			minfo.markAsAssertionFailed(line);
			mark(className, MarkerManager.ASSERTION_FAILED_MARKER_ID, line, "Assertion failed", label);
			
			// Cancels when error found
			if (stopAfterError)
				monitor.setCanceled(true);
			
			return;
		}
		
		// In case of NPE label
		if (TranslatorUtils.isNpeName(label)) {
			
			// Bypasses if already marked as npe
			if (minfo.isMarkedAsNpe(line)) return;
			
			// Unmarks the existing covered marker (if any)
			unmark(className, MarkerManager.COVERED_MARKER_ID, line);
			
			// Marks as NPE
			minfo.markAsNpe(line);
			mark(className, MarkerManager.NPE_MARKER_ID, line, "NullPointerException", label);
			
			// Cancels when error found
			if (stopAfterError)
				monitor.setCanceled(true);
			
			return;
		}
		
		if (TranslatorUtils.isIoobName(label)) {
			
			// Bypasses if already marked as npe
			if (minfo.isMarkedAsIoob(line)) return;
			
			// Unmarks the existing covered marker (if any)
			unmark(className, MarkerManager.COVERED_MARKER_ID, line);
			
			// Marks as NPE
			minfo.markAsIoob(line);
			mark(className, MarkerManager.IOOB_MARKER_ID, line, "ArrayIndexOutOfBoundException", label);
			
			// Cancels when error found
			if (stopAfterError)
				monitor.setCanceled(true);
			
			return;
		}
		
		if (TranslatorUtils.isHeapOverflowName(label)) {
			
			// Bypasses if already marked as npe
			if (minfo.isMarkedAsHeapOverflowed(line)) return;
			
			// Unmarks the existing covered marker (if any)
			unmark(className, MarkerManager.COVERED_MARKER_ID, line);
			
			// Marks as NPE
			minfo.markAsIoob(line);
			mark(className, MarkerManager.HO_MARKER_ID, line, "Not enough heap", label);
			
			return;
		}
		
		// Bypasses if already marked as covered
		if (minfo.isMarkedAsCovered(line)) return;
		
		// Marks as covered
		minfo.markAsCovered(line);
		mark(className, MarkerManager.COVERED_MARKER_ID, line, "Covered", label);
	}
	
	/**
	 * Marks the resource at line as covered.
	 * 
	 * @param resource the resource.
	 * @param line the line.
	 * @throws CoreException
	 */
	void mark(IResource resource, int line) throws CoreException {
		
		HashMap<String, Object> attr = new HashMap<String, Object>();
		attr.put(IMarker.LINE_NUMBER, line);
		MarkerUtilities.createMarker(resource, attr, MarkerManager.COVERED_MARKER_ID);
	}
	
	/**
	 * Logs the msg.
	 * 
	 * @param msg the message.
	 * @param args the message arguments.
	 */
	private static void log(String msg, Object... args) {
		CoverageLaunchShortcut.log(String.format(msg, args));
	}
	
	/**
	 * Logs the msg.
	 * 
	 * @param msg the message.
	 * @param args the message arguments.
	 */
	private static void info(String msg, Object... args) {
		CoverageLaunchShortcut.log(String.format(msg, args));
	}
	
	/**
	 * Records lines of codes that are marked.
	 * 
	 * @author suwimont
	 *
	 */
	private class MarkerInfo {
		
		/**
		 * Maps lines to marker ids.
		 */
		private HashMap<Integer, HashSet<String>> marked = new HashMap<Integer, HashSet<String>>();
		
		public void markAsCovered(int line) {
			mark(line, MarkerManager.COVERED_MARKER_ID);
		}
		
		public void markAsAssertionFailed(int line) {
			mark(line, MarkerManager.ASSERTION_FAILED_MARKER_ID);
		}
		
		public void markAsNpe(int line) {
			mark(line, MarkerManager.NPE_MARKER_ID);
		}
		
		public void markAsIoob(int line) {
			mark(line, MarkerManager.IOOB_MARKER_ID);
		}
		
		public void makrAsHeapOverflowed(int line) {
			mark(line, MarkerManager.HO_MARKER_ID);
		}
		
		public void markAsNotCovered(int line) {
			mark(line, MarkerManager.NOT_COVERED_MARKER_ID);
		}
		
		/**
		 * Marks line.
		 * 
		 * @param line the line of code.
		 */
		public void mark(int line, String id) {
			
			HashSet<String> ids = marked.get(line);
			if (ids == null) {
				ids = new HashSet<String>();
				marked.put(line, ids);
			}
			ids.add(id);
		}
		
		/**
		 * Returns <code>true</code> if the line is marked;
		 * or <code>false</code> otherwise.
		 * 
		 * @param line the line of code.
		 * @return <code>true</code> iff the line is marked.
		 */
		public boolean isMarked(int line) {
			return marked.containsKey(line);
		}
		
		public boolean isMarkedAsCovered(int line) {
			return isMarked(line, MarkerManager.COVERED_MARKER_ID);
		}
		
		public boolean isMarkedAsAssertionFailed(int line) {
			return isMarked(line, MarkerManager.ASSERTION_FAILED_MARKER_ID);
		}
		
		public boolean isMarkedAsNpe(int line) {
			return isMarked(line, MarkerManager.NPE_MARKER_ID);
		}
		
		public boolean isMarkedAsIoob(int line) {
			return isMarked(line, MarkerManager.IOOB_MARKER_ID);
		}
		
		public boolean isMarkedAsHeapOverflowed(int line) {
			return isMarked(line, MarkerManager.HO_MARKER_ID);
		}
		
		public boolean isMarkedAsNotCovered(int line) {
			return isMarked(line, MarkerManager.NOT_COVERED_MARKER_ID);
		}
		
		private boolean isMarked(int line, String id) {
			
			if (!marked.containsKey(line))
				return false;
			
			HashSet<String> ids = marked.get(line);
			if (ids == null) return false;
			return ids.contains(id);
		}
	}
}
