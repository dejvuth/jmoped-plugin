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

public class ArgumentMarkerResolution implements IMarkerResolution2 {
	
	private Type type;
	
	public ArgumentMarkerResolution(Type type) {
		this.type = type;
	}

	public String getDescription() {
		return "Find all arguments of the method where the analysis started " +
				"that lead to this " + type.name + ".";
	}

	public Image getImage() {
		return Activator.getImageDescriptor(type.image).createImage();
	}

	public String getLabel() {
		return "Find method arguments";
	}

	public void run(IMarker marker) {
		String label = marker.getAttribute(MarkerManager.LABEL, "");
		Remopla remopla = CoverageLaunchShortcut.getLastRemopla();
		Translator translator = CoverageLaunchShortcut.getLastTranslator();
		MethodArgument[] args = translator.getMethodArguments(
				remopla.getRawArguments2(label), remopla.getFloats());
		Activator.info("Method arguments found", 
				"Total: " + args.length + " arguments.");
		ProgressView view = Activator.findProgressView();
		view.setArguments(args);
	}

	public enum Type {
		ASSERT("AssertionError", "assertion.png"),
		COVERED("program point", "covered.png"),
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
