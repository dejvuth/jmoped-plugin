package de.tum.in.jmoped;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * jMoped preference page.
 * 
 * @author suwimont
 *
 */
public class Preference
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage{

	private BooleanFieldEditor stopAfterError;
	private ComboFieldEditor bddpackage;
	private IntegerFieldEditor nodenum;
	private IntegerFieldEditor cachesize;
	private ComboFieldEditor verbosity;
	
	public static final String STOP_AFTER_ERROR = "stopAfterErrorPreference";
	public static final String BDDPACKAGE = "bddpackagePreference";
	public static final String NODENUM = "nodenumPreference";
	public static final String CACHESIZE = "cachesizePreference";
	public static final String VERBOSITY = "verbosityPreference";
	
	/**
	 * The constructor.
	 */
	public Preference() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("jMoped preferences:");
	}
	
	/**
	 * Creates the field editors in the preference page.
	 */
	public void createFieldEditors() {
		// Editor: Stops after error found
		stopAfterError = new BooleanFieldEditor(
				STOP_AFTER_ERROR,
				"Stop after &error found",
				getFieldEditorParent());
		addField(stopAfterError);
		
		// Editor: BDD Package
		bddpackage = new ComboFieldEditor(BDDPACKAGE,
				"BDD &package:                    ",
				new String[][] { {"Java", "java"}, {"CUDD", "cudd"} },
				getFieldEditorParent());
		addField(bddpackage);
		
		// Editor: BDD Nodes
		nodenum = new IntegerFieldEditor(NODENUM, "BDD &nodes:", 
				getFieldEditorParent());
		addField(nodenum);
		
		// Editor: BDD Cache
		cachesize = new IntegerFieldEditor(CACHESIZE, "BDD cache &size:", 
				getFieldEditorParent());
		addField(cachesize);
		
		// Editor: Verbosity
		verbosity = new ComboFieldEditor(VERBOSITY, "&Verbosity:",
				new String[][] { {"None", "0"}, {"Statistics", "1"}, {"Debug", "2"}, {"All", "3"} },
				getFieldEditorParent());
		addField(verbosity);
	}
	
	public void init(IWorkbench workbench) {
	}
	
	/**
	 * The preference initializer.
	 * 
	 * @author suwimont
	 *
	 */
	public static class Initializer extends AbstractPreferenceInitializer {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
		 */
		public void initializeDefaultPreferences() {
			IPreferenceStore store = Activator.getDefault().getPreferenceStore();
			store.setDefault(STOP_AFTER_ERROR, false);
			store.setDefault(BDDPACKAGE, "cudd");
			store.setDefault(NODENUM, 1000000);
			store.setDefault(CACHESIZE, 1000000);
			store.setDefault(VERBOSITY, 0);
		}
	}
}
