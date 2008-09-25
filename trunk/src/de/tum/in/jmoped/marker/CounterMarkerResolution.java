package de.tum.in.jmoped.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

import de.tum.in.jmoped.Activator;
import de.tum.in.jmoped.CoverageLaunchShortcut;
import de.tum.in.jmoped.ProgressView;
import de.tum.in.jmoped.translator.MethodArgument;
import de.tum.in.jmoped.translator.Translator;
import de.tum.in.jmoped.underbone.Remopla;

/**
 *
 * 
 * @author suwimont
 *
 */
public class CounterMarkerResolution implements IMarkerResolution2 {
	
	private Type type;
	
	public CounterMarkerResolution(Type type) {
		this.type = type;
	}

	public String getDescription() {
		return "Count the number of arguments of the method where the analysis started " +
				"that lead to this " + type.name + ".";
	}

	public Image getImage() {
		return Activator.getImageDescriptor(type.image).createImage();
	}

	public String getLabel() {
		return "Count method arguments";
	}

	public void run(IMarker marker) {
		String label = marker.getAttribute(MarkerManager.LABEL, "");
		Remopla remopla = CoverageLaunchShortcut.getLastRemopla();
		Translator translator = CoverageLaunchShortcut.getLastTranslator();
//		MethodArgument[] args = translator.getMethodArguments(
//				remopla.getRawArguments(label), remopla.getFloats());
		Activator.info("Argument Count", 
				"Result: " + remopla.countRawArguments(label) + " arguments.");
//		ProgressView view = Activator.findProgressView();
//		view.setArguments(args);
	}

	public enum Type {
		ASSERT("AssertionError", "assertion.png"),
		IOOB("ArrayIndexOutOfBoundException", "ioob.png"),
		NPE("NullPointerException", "npe.png");
		
		private final String name;
		private final String image;
		
		Type(String name, String image) {
			this.name = name;
			this.image = image;
		}
	}
}
