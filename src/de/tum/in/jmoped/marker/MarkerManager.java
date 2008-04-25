package de.tum.in.jmoped.marker;

import java.util.HashMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.MarkerUtilities;

import de.tum.in.jmoped.Activator;
import de.tum.in.jmoped.translator.TranslatorUtils;

/**
 * The manager for all jMoped markers.
 * 
 * @author suwimont
 *
 */
public class MarkerManager {

	/**
	 * Label marker attribute.
	 */
	public static final String LABEL =  Activator.PLUGIN_ID + ".label";
	
	public static final String ASSERTION_FAILED_MARKER_ID = Activator.PLUGIN_ID + ".assertionfailedmarker";
	public static final String COVERED_MARKER_ID = Activator.PLUGIN_ID + ".coveredmarker";
	public static final String NOT_COVERED_MARKER_ID = Activator.PLUGIN_ID + ".notcoveredmarker";
	public static final String SELECTED_MARKER_ID = Activator.PLUGIN_ID + ".selectedmarker";
	public static final String NPE_MARKER_ID = Activator.PLUGIN_ID + ".npemarker";
	public static final String IOOB_MARKER_ID = Activator.PLUGIN_ID + ".ioobmarker";
	public static final String HO_MARKER_ID = Activator.PLUGIN_ID + ".notenoughheapmarker";
	
	/**
	 * Initializes the markers. Must be called at the beginning of every
	 * jMoped analysis.
	 */
	public static void init() {
		
		deleteMarkers();
		AddLabelMarkerResolution.added = null;
	}
	
	public static IMarker mark(IResource resource, String className, String type, 
			int line, String msg, String label) throws CoreException {
		
		if (resource == null) return null;
		
		HashMap<String, Object> attr = new HashMap<String, Object>();
		attr.put(IMarker.LINE_NUMBER, line);
		attr.put(IMarker.MESSAGE, msg);
		attr.put(MarkerManager.LABEL, label);
//		MarkerUtilities.createMarker(resource, attr, type);
		IMarker marker = resource.createMarker(type);
		marker.setAttributes(attr);
		return marker;
	}
	
	public static IMarker mark(IMarker marker, String type, String msg) throws CoreException {
		
		String label = marker.getAttribute(MarkerManager.LABEL, "");
		return mark(marker.getResource(), 
				TranslatorUtils.extractClassName(label), 
				type,  
				MarkerUtilities.getLineNumber(marker), msg, label);
	}
	
	/**
	 * Deletes the marker specified by the class name, type, and line.
	 * 
	 * @param className the class name
	 * @param type the marker type
	 * @param line the source line
	 * @throws CoreException 
	 */
	public static void unmark(IResource resource, String className, String type, 
			int line) throws CoreException {
		
		IMarker[] markers = resource.findMarkers(type, false, IResource.DEPTH_INFINITE);
		if (markers == null) return;
		for (IMarker marker : markers) {
			int findLine = MarkerUtilities.getLineNumber(marker);
			if (findLine != line) continue;
			marker.delete();
			break;
		}
	}

	/**
	 * Deletes all markers.
	 */
	public static void deleteMarkers() {
		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		try {
			root.deleteMarkers(COVERED_MARKER_ID, false, IResource.DEPTH_INFINITE);
			root.deleteMarkers(NOT_COVERED_MARKER_ID, false, IResource.DEPTH_INFINITE);
			root.deleteMarkers(ASSERTION_FAILED_MARKER_ID, false, IResource.DEPTH_INFINITE);
			root.deleteMarkers(SELECTED_MARKER_ID, false, IResource.DEPTH_INFINITE);
			root.deleteMarkers(NPE_MARKER_ID, false, IResource.DEPTH_INFINITE);
			root.deleteMarkers(IOOB_MARKER_ID, false, IResource.DEPTH_INFINITE);
			root.deleteMarkers(HO_MARKER_ID, false, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
