package de.tum.in.jmoped.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class NpeMarkerResolutionGenerator implements
		IMarkerResolutionGenerator2 {

	public boolean hasResolutions(IMarker marker) {
		return true;
	}

	public IMarkerResolution[] getResolutions(IMarker marker) {
		return new IMarkerResolution[] { 
				new ArgumentMarkerResolution(ArgumentMarkerResolution.Type.NPE) };
	}

}
