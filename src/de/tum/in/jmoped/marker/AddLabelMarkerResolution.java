package de.tum.in.jmoped.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

import de.tum.in.jmoped.Activator;

public class AddLabelMarkerResolution implements IMarkerResolution2 {
	
	static IMarker added;

	public String getDescription() {
		return "Add this program point for future analysis";
	}

	public Image getImage() {
		return Activator.getImageDescriptor("selected.png").createImage();
	}

	public String getLabel() {
		return "Add this program point";
	}

	public void run(IMarker marker) {
		try {
			if (added != null) {
				MarkerManager.mark(added, MarkerManager.COVERED_MARKER_ID, "Covered");
				added.delete();
			}
			added = MarkerManager.mark(marker, MarkerManager.SELECTED_MARKER_ID, "Selected");
			marker.delete();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
