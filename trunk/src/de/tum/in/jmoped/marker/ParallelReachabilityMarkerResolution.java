package de.tum.in.jmoped.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

import de.tum.in.jmoped.Activator;
import de.tum.in.jmoped.CoverageLaunchShortcut;
import de.tum.in.jmoped.translator.TranslatorUtils;
import de.tum.in.jmoped.underbone.LabelUtils;
import de.tum.in.jmoped.underbone.Remopla;

public class ParallelReachabilityMarkerResolution implements IMarkerResolution2 {

	public String getDescription() {
		return "Check if this program point can be reachable in parallel " +
				"with the previously seleted program point";
	}

	public Image getImage() {
		return Activator.getImageDescriptor("down2.png").createImage();
	}

	public String getLabel() {
		return "Parallel reachability analysis";
	}

	public void run(IMarker marker) {
		IMarker amarker = AddLabelMarkerResolution.added;
		String a = amarker.getAttribute(MarkerManager.LABEL, "");
		String b = marker.getAttribute(MarkerManager.LABEL, "");
		boolean reachable = CoverageLaunchShortcut.getLastRemopla().reachable(a, b);
		String title = (reachable) ? "Reachable" : "Not Reachable";
		String msg;
		if (reachable) {
			msg = "Concurrent reachable program points:%n" +
					"Line %d of method %s%nLine %d of method %s";
		} else {
			msg = "Program points under test:%n" +
					"Line %d of method %s%nLine %d of method %s";
		}
		Activator.info(title, 
				String.format(msg, 
						amarker.getAttribute(IMarker.LINE_NUMBER, 0),
						LabelUtils.trimOffset(a),
						marker.getAttribute(IMarker.LINE_NUMBER, 0),
						LabelUtils.trimOffset(b)));
	}

}
