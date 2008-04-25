package de.tum.in.jmoped.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class CoveredMarkerResolutionGenerator implements
		IMarkerResolutionGenerator2 {

	public boolean hasResolutions(IMarker marker) {
		return true;
	}

	public IMarkerResolution[] getResolutions(IMarker marker) {
		if (AddLabelMarkerResolution.added == null)
			return new IMarkerResolution[] { new AddLabelMarkerResolution() };
		
		return new IMarkerResolution[] { 
				new AddLabelMarkerResolution(),
				new ParallelReachabilityMarkerResolution() };
	}

}
