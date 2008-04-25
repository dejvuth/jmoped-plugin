package de.tum.in.jmoped;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.formatter.IndentManipulation;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import de.tum.in.jmoped.translator.MethodArgument;

/**
 * Composite for displaying method arguments.
 * 
 * @author suwimont
 *
 */
public class ArgumentComposite extends Composite {

	private TreeViewer tree;
	
	/**
	 * The constructor.
	 * 
	 * @param parent
	 * @param style
	 */
	public ArgumentComposite(Composite parent, int style) {
		super(parent, style);
		setLayout(new FillLayout());
		
		// Creates empty tree
		tree = new TreeViewer(this);
		tree.setContentProvider(new ArgumentContentProvider());
		tree.setLabelProvider(new ArgumentLabelProvider());
		
		// Creates context menu
		createContextMenu();
	}
	
	/**
	 * Displays the arguments.
	 * 
	 * @param args the arguments.
	 */
	public void setArguments(MethodArgument[] args) {
		tree.setInput(args);
	}
	
	/**
	 * Creates context menu right-clicked on an argument(s).
	 */
	private void createContextMenu() {
		MenuManager manager = new MenuManager();
		manager.setRemoveAllWhenShown(true);
		Menu menu = manager.createContextMenu(tree.getControl());
		tree.getControl().setMenu(menu);
		
		manager.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
			
		});
	}
	
	private void fillContextMenu(IMenuManager manager) {
		
		ITreeSelection sel = (ITreeSelection) tree.getSelection();
		if (sel.isEmpty()) return;
		
		manager.add(new NewTestCaseAction(CoverageLaunchShortcut.getLastMethod(), sel));
	}
	
	private class ArgumentContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {
			return null;
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return false;
		}

		public Object[] getElements(Object inputElement) {
			return (MethodArgument[]) inputElement;
		}

		public void dispose() {
			// TODO Auto-generated method stub
			
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private class ArgumentLabelProvider extends LabelProvider {
		
	}
	
	/**
	 * Imitates {@link org.eclipse.jdt.ui.actions.AbstractOpenWizardAction}.
	 * 
	 * @author suwimont
	 *
	 */
	private class NewTestCaseAction extends Action {
		
		private IMethod method;
		private ITreeSelection sel;
		private Shell shell;
		
		public NewTestCaseAction(IMethod method, ITreeSelection sel) {
			this.method = method;
			this.sel = sel;
			setText("Create JUnit test case");
			setImageDescriptor(Activator.getImageDescriptor("junit.gif"));
		}
		
		public void run() {
			IWizard wizard = new NewTestCaseWizard(method, getMethodArguments());
			
			Shell shell = getShell();
			WizardDialog dialog = new WizardDialog(shell, wizard);
			dialog.create();
			int res = dialog.open();
			notifyResult(res == Window.OK);
		}
		
		private MethodArgument[] getMethodArguments() {
			
			MethodArgument[] args = new MethodArgument[sel.size()];
			Iterator itr = sel.iterator();
			int i = 0;
			while (itr.hasNext()) {
				args[i++] = (MethodArgument) itr.next();
			}
			
			return args;
		}
		
		private Shell getShell() {
			if (shell != null) return shell;
			return Activator.getActiveWorkbenchShell();
		}
	}
	
	private class NewTestCaseWizard extends Wizard {
		
		private IMethod method;
		private MethodArgument[] args;
		private NewTestCaseWizardPage page;
		
		public NewTestCaseWizard(IMethod method, MethodArgument[] args) {
			this.method = method;
			this.args = args;
			setDefaultPageImageDescriptor(Activator.getImageDescriptor("newtest_wiz.png"));
		}
		
		public void addPages() {
			super.addPages();
			page = new NewTestCaseWizardPage(method, args);
			addPage(page);
			setWindowTitle("New JUnit Test Case");
		}
		
		/**
		 * Immitates {@link NewElementWizard}
		 */
		@Override
		public boolean performFinish() {
			
			IType type = page.getExistingType();
			if (type == null) {
				try {
					page.createType(null);
				} catch (CoreException e) {
					Activator.error(e);
					return false;
				} catch (InterruptedException e) {
					Activator.error(e);
					return false;
				}
			} else {
				try {
					page.appendMethod(type);
				} catch (JavaModelException e) {
					Activator.error(e);
					return false;
				}
			}
			
			IResource resource = page.getModifiedResource();
			if (resource == null) return false;
			BasicNewResourceWizard.selectAndReveal(resource, Activator.getActiveWorkbenchWindow());
			openResource((IFile) resource);
			return true;
		}
		
		private void openResource(final IFile file) {
			final IWorkbenchPage page = Activator.getActivePage();
			if (page == null) return;
			
			final Display display = getShell().getDisplay();
			if (display == null) return;
			
			display.asyncExec(new Runnable() {
				public void run() {
					try {
						IDE.openEditor(page, file, true);
					} catch (PartInitException e) {
						Activator.error(e);
					}
				}
			});
		}
	}
	
	/**
	 * New JUnit test case wizard page.
	 * 
	 * @author suwimont
	 *
	 */
	private class NewTestCaseWizardPage extends NewTypeWizardPage {
		
		private IMethod method;
		private MethodArgument[] args;
		private Text methodText;
		private final IStatus OK_STATUS = new Status(IStatus.OK, 
				Activator.PLUGIN_ID, IStatus.OK, DESCRIPTION, null);
		
		private static final String DESCRIPTION = "Create a new JUnit test case.";
		
		/**
		 * The constructor.
		 * 
		 * @param method
		 */
		public NewTestCaseWizardPage(IMethod method, MethodArgument[] args) {
			super(CLASS_TYPE, "New Test Case Wizard Page");
			this.method = method;
			this.args = args;
			
			setTitle("JUnit Test Case");
			setDescription(DESCRIPTION);
		}

		/**
		 * Creates controls.
		 */
		public void createControl(Composite parent) {
			initializeDialogUnits(parent);
			
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setFont(parent.getFont());
			
			int nColumns = 4;
			GridLayout layout = new GridLayout();
			layout.numColumns = nColumns;
			composite.setLayout(layout);
			
			createContainerControls(composite, nColumns);
			createPackageControls(composite, nColumns);
			createSeparator(composite, nColumns);
			createTypeNameControls(composite, nColumns);
			createModifierControls(composite, nColumns);
			createMethodNameControls(composite, nColumns);
			
			setControl(composite);
			
			Dialog.applyDialogFont(composite);
			initControl();
		}
		
		/**
		 * Creates the test method.
		 */
		protected void createTypeMembers(IType newType,
                NewTypeWizardPage.ImportsManager imports,
                IProgressMonitor monitor) throws JavaModelException {
			
			boolean stc = Flags.isStatic(method.getFlags());
			String caller = (stc) 
					? method.getDeclaringType().getFullyQualifiedName()
					: method.getDeclaringType().getElementName().toLowerCase();
			caller += "." + method.getElementName();
			
			String newline = System.getProperty("line.separator");
			StringBuilder content = new StringBuilder();
			content.append("@Test public void ");
			content.append(methodText.getText());
			content.append("() {");
			content.append(newline);
			
			// Creates an instance with a constructor
			if (!stc) {
				content.append("\t");
				content.append(newInstance());
				content.append(newline);
			}
			
			// Calls the method
			for (MethodArgument arg : args) {
				content.append("\t");
				content.append(caller);
				content.append("(");
				content.append(arg.toJavaString());
				content.append(");");
				content.append(newline);
			}
			
			content.append("}");
			newType.createMethod(content.toString(), null, false, monitor);
			if (imports != null) imports.addImport("org.junit.Test");
		}
		
		private String newInstance() throws JavaModelException {
			
			// Finds a constructor with minimal number of parameters
			IType type = method.getDeclaringType();
			IMethod[] methods = type.getMethods();
			IMethod constructor = null;
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].getElementName().equals(type.getElementName())) {
					if (constructor == null 
							|| methods[i].getNumberOfParameters() < constructor.getNumberOfParameters()) {
						constructor = methods[i];
					}
				}
			}
			
			// Creates a default argument string
			StringBuilder args = new StringBuilder();
			if (constructor != null && constructor.getNumberOfParameters() > 0) {
				String[] params = constructor.getParameterTypes();
				args.append(defaultArgument(params[0]));
				for (int i = 1; i < params.length; i++) {
					args.append(", ");
					args.append(defaultArgument(params[i]));
				}
			}
			
			String className = type.getFullyQualifiedName();
			return String.format("%s %s = new %s(%s);", 
					className, type.getElementName().toLowerCase(), className, args.toString());
		}
		
		private String defaultArgument(String param) {
			switch(param.charAt(0)) {
			case 'L':
			case 'Q':
			case '[':
				return "null";
			case 'D':
			case 'F':
				return "0.0";
			case 'Z':
				return "false";
			default:
				return "0";
			}
		}
		
		protected IStatus typeNameChanged() {
			
			// Disables the modifiers if the type already exists.
			boolean modifiable = true;
			if (getExistingType() != null) modifiable = false;
			setModifiers(getModifiers(), modifiable);
			
			return super.typeNameChanged();
		}
		
		/**
		 * Imitates {@link NewClassWizardPage}.
		 */
		private void doStatusUpdate() {
			
			// Makes appending new method to existing type possible
			if (fTypeNameStatus.getSeverity() == IStatus.ERROR
					&& fTypeNameStatus.getMessage().equals("Type already exists.")) {
				fTypeNameStatus = OK_STATUS;
			}
			
			// status of all used components
			IStatus[] status= new IStatus[] {
				fContainerStatus,
				fPackageStatus,
				fTypeNameStatus,
				fModifierStatus,
				methodNameChanged()
			};
			
			// the mode severe status will be displayed and the OK button enabled/disabled.
			updateStatus(status);
		}
		
		/**
		 * Copied from {@link NewClassWizardPage}.
		 */
		protected void handleFieldChanged(String fieldName) {
			super.handleFieldChanged(fieldName);
			
			doStatusUpdate();
		}
		
		protected void setFocus() {
			methodText.setFocus();
		}
		
		/**
		 * Initializes controls.
		 */
		private void initControl() {
			
			initContainerPage(method);
			initTypePage(method);
			initTypeName();
			initMethodName();
		}
		
		/**
		 * Taken from {@link #createTypeNameControls(Composite, int)}, 
		 * {@link StringDialogField}, and {@link DialogField}.
		 * 
		 * @param parent
		 * @param nColumns
		 */
		private void createMethodNameControls(Composite parent, int nColumns) {
			
			Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
			label.setFont(parent.getFont());
			label.setEnabled(true);		
			label.setText("Method Name:");
			label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			
			methodText = new Text(parent, SWT.SINGLE | SWT.BORDER);
			methodText.setFont(parent.getFont());
			methodText.setEnabled(true);
			GridData gd = new GridData();
			gd.horizontalAlignment = GridData.FILL;
			gd.grabExcessHorizontalSpace = false;
			gd.horizontalSpan = nColumns - 2;
			methodText.setLayoutData(gd);
			
			methodText.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					doStatusUpdate();
				}
				
			});
		}
		
		private IStatus methodNameChanged() {
			
			IType type = getExistingType();
			if (type == null) {
				return OK_STATUS;
			}
			
			String name = methodText.getText();
			try {
				for (IMethod method : type.getMethods()) {
					if (method.getElementName().equals(name)) {
						String msg = String.format("Method %s already exists.", name);
						return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 
								IStatus.ERROR, msg, null);
					}
						
				}
			} catch (JavaModelException e1) {
				Activator.error(e1);
			}
			
			String msg = String.format("Class %s already exists. New method will be appended.", 
					type.getElementName());
			return new Status(IStatus.INFO, Activator.PLUGIN_ID, IStatus.INFO, msg, null);
		}
		
		/**
		 * Initializes class name.
		 * 
		 * @param method
		 */
		private void initTypeName() {
			
			IType type = method.getDeclaringType();
			if (type == null) return;
			
			String name = type.getElementName();
			setTypeName(name + "Test", true);
		}
		
		/**
		 * Initializes the "method name" text field.
		 */
		private void initMethodName() {
			
			String name = method.getElementName();
			StringBuilder test = new StringBuilder("test");
			test.append(Character.toUpperCase(name.charAt(0)));
			if (name.length() > 1)
				test.append(name.substring(1));
			String s = test.toString();
			methodText.setText(s);
			methodText.setFocus();
		}
		
		/**
		 * Appends the test method to the given <code>type</code>.
		 * 
		 * @param type
		 * @throws JavaModelException
		 */
		public void appendMethod(IType type) throws JavaModelException {
			
			ICompilationUnit cu = type.getCompilationUnit();
			createTypeMembers(type, null, null);
			
			ISourceRange range = type.getSourceRange();
			
			IBuffer buf = cu.getBuffer();
			String originalContent = buf.getText(range.getOffset(), range.getLength());
			int indent = getIndentUsed(type);
			String lineDelimiter = getLineDelimiterUsed(type.getJavaProject());
			String formattedContent = format(CodeFormatter.K_CLASS_BODY_DECLARATIONS,
					originalContent, indent, null, lineDelimiter, type.getJavaProject());
			formattedContent = trimLeadingTabsAndSpaces(formattedContent);
			buf.replace(range.getOffset(), range.getLength(), formattedContent);
		}
		
		/**
		 * Returns <code>IType</code> of the selected name if already exists.
		 * The method returns <code>null</code> otherwise.
		 * 
		 * @return <code>IType</code> of the selected name if exists.
		 */
		public IType getExistingType() {
			
			IJavaProject project = method.getJavaProject();
			try {
				return project.findType(getPackageText() + "." + getTypeName());
			} catch (JavaModelException e) {
				return null;
			}
		}
		
		/*
		 * The codes below here are taken as Eclipse in many places
		 * in order to support the method "appendMethod".
		 */
		
		/**
		 * Evaluates the indentation used by a Java element. (in tabulators)
		 */	
		private int getIndentUsed(IJavaElement elem) throws JavaModelException {
			if (elem instanceof ISourceReference) {
				ICompilationUnit cu= (ICompilationUnit) elem.getAncestor(IJavaElement.COMPILATION_UNIT);
				if (cu != null) {
					IBuffer buf= cu.getBuffer();
					int offset= ((ISourceReference)elem).getSourceRange().getOffset();
					int i= offset;
					// find beginning of line
					while (i > 0 && !IndentManipulation.isLineDelimiterChar(buf.getChar(i - 1)) ){
						i--;
					}
					return computeIndentUnits(buf.getText(i, offset - i), elem.getJavaProject());
				}
			}
			return 0;
		}
		
		private int computeIndentUnits(String line, IJavaProject project) {
			return IndentManipulation.measureIndentUnits(line, getTabWidth(project), 
					getIndentWidth(project));
		}
		
		/**
		 * Gets the current tab width.
		 * 
		 * @param project The project where the source is used, used for project
		 *        specific options or <code>null</code> if the project is unknown
		 *        and the workspace default should be used
		 * @return The tab width
		 */
		private int getTabWidth(IJavaProject project) {
			/*
			 * If the tab-char is SPACE, FORMATTER_INDENTATION_SIZE is not used
			 * by the core formatter.
			 * We piggy back the visual tab length setting in that preference in
			 * that case.
			 */
			String key;
			if (JavaCore.SPACE.equals(getCoreOption(project, DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR)))
				key= DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE;
			else
				key= DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;
			
			return getCoreOption(project, key, 4);
		}
		
		/**
		 * Returns the possibly <code>project</code>-specific core preference
		 * defined under <code>key</code>.
		 * 
		 * @param project the project to get the preference from, or
		 *        <code>null</code> to get the global preference
		 * @param key the key of the preference
		 * @return the value of the preference
		 * @since 3.1
		 */
		private String getCoreOption(IJavaProject project, String key) {
			if (project == null)
				return JavaCore.getOption(key);
			return project.getOption(key, true);
		}
		
		private int getIndentWidth(IJavaProject project) {
			String key;
			if (DefaultCodeFormatterConstants.MIXED.equals(getCoreOption(project, DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR)))
				key= DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE;
			else
				key= DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;
			
			return getCoreOption(project, key, 4);
		}
		
		/**
		 * Returns the possibly <code>project</code>-specific core preference
		 * defined under <code>key</code>, or <code>def</code> if the value is
		 * not a integer.
		 * 
		 * @param project the project to get the preference from, or
		 *        <code>null</code> to get the global preference
		 * @param key the key of the preference
		 * @param def the default value
		 * @return the value of the preference
		 * @since 3.1
		 */
		private int getCoreOption(IJavaProject project, String key, int def) {
			try {
				return Integer.parseInt(getCoreOption(project, key));
			} catch (NumberFormatException e) {
				return def;
			}
		}
		
		/**
		 * Returns the line delimiter which is used in the specified project.
		 * 
		 * @param project the java project, or <code>null</code>
		 * @return the used line delimiter
		 */
		private String getLineDelimiterUsed(IJavaProject project) {
			return getProjectLineDelimiter(project);
		}

		private String getProjectLineDelimiter(IJavaProject javaProject) {
			IProject project= null;
			if (javaProject != null)
				project= javaProject.getProject();
			
			String lineDelimiter= getLineDelimiterPreference(project);
			if (lineDelimiter != null)
				return lineDelimiter;
			
			return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		private String getLineDelimiterPreference(IProject project) {
			IScopeContext[] scopeContext;
			if (project != null) {
				// project preference
				scopeContext= new IScopeContext[] { new ProjectScope(project) };
				String lineDelimiter= Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
				if (lineDelimiter != null)
					return lineDelimiter;
			}
			// workspace preference
			scopeContext= new IScopeContext[] { new InstanceScope() };
			String platformDefault= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			return Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, platformDefault, scopeContext);
		}
		
		private String format(int kind, String string, int indentationLevel, int[] positions, String lineSeparator, IJavaProject project) {
			Map options= project != null ? project.getOptions(true) : null;
			return format(kind, string, 0, string.length(), indentationLevel, positions, lineSeparator, options);
		}
		

		/**
		 * Old API. Consider to use format2 (TextEdit)
		 */	
		private String format(int kind, String string, int offset, int length, int indentationLevel, int[] positions, String lineSeparator, Map options) {
			TextEdit edit= format2(kind, string, offset, length, indentationLevel, lineSeparator, options);
			if (edit == null) {
				//JavaPlugin.logErrorMessage("formatter failed to format (no edit returned). Will use unformatted text instead. kind: " + kind + ", string: " + string); //$NON-NLS-1$ //$NON-NLS-2$
				return string.substring(offset, offset + length);
			}
			String formatted= getOldAPICompatibleResult(string, edit, indentationLevel, positions, lineSeparator, options);
			return formatted.substring(offset, formatted.length() - (string.length() - (offset + length)));
		}
		
		/**
		 * Creates edits that describe how to format the given string. Returns <code>null</code> if the code could not be formatted for the given kind.
		 * @throws IllegalArgumentException If the offset and length are not inside the string, a
		 *  IllegalArgumentException is thrown.
		 */
		private TextEdit format2(int kind, String string, int offset, int length, int indentationLevel, String lineSeparator, Map options) {
			if (offset < 0 || length < 0 || offset + length > string.length()) {
				throw new IllegalArgumentException("offset or length outside of string. offset: " + offset + ", length: " + length + ", string size: " + string.length());   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			}
			return ToolFactory.createCodeFormatter(options).format(kind, string, offset, length, indentationLevel, lineSeparator);
		}
		
		private String getOldAPICompatibleResult(String string, TextEdit edit, int indentationLevel, int[] positions, String lineSeparator, Map options) {
			Position[] p= null;
			
			if (positions != null) {
				p= new Position[positions.length];
				for (int i= 0; i < positions.length; i++) {
					p[i]= new Position(positions[i], 0);
				}
			}
			String res= evaluateFormatterEdit(string, edit, p);
			
			if (positions != null) {
				for (int i= 0; i < positions.length; i++) {
					Position curr= p[i];
					positions[i]= curr.getOffset();
				}
			}			
			return res;
		}
		
		private String evaluateFormatterEdit(String string, TextEdit edit, Position[] positions) {
			try {
				Document doc= createDocument(string, positions);
				edit.apply(doc, 0);
				if (positions != null) {
					for (int i= 0; i < positions.length; i++) {
						Assert.isTrue(!positions[i].isDeleted, "Position got deleted"); //$NON-NLS-1$
					}
				}
				return doc.get();
			} catch (BadLocationException e) {
				Activator.error(e); // bug in the formatter
				Assert.isTrue(false, "Formatter created edits with wrong positions: " + e.getMessage()); //$NON-NLS-1$
			}
			return null;
		}
		
		private Document createDocument(String string, Position[] positions) throws IllegalArgumentException {
			Document doc= new Document(string);
			try {
				if (positions != null) {
					final String POS_CATEGORY= "myCategory"; //$NON-NLS-1$
					
					doc.addPositionCategory(POS_CATEGORY);
					doc.addPositionUpdater(new DefaultPositionUpdater(POS_CATEGORY) {
						protected boolean notDeleted() {
							if (fOffset < fPosition.offset && (fPosition.offset + fPosition.length < fOffset + fLength)) {
								fPosition.offset= fOffset + fLength; // deleted positions: set to end of remove
								return false;
							}
							return true;
						}
					});
					for (int i= 0; i < positions.length; i++) {
						try {
							doc.addPosition(POS_CATEGORY, positions[i]);
						} catch (BadLocationException e) {
							throw new IllegalArgumentException("Position outside of string. offset: " + positions[i].offset + ", length: " + positions[i].length + ", string size: " + string.length());   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
						}
					}
				}
			} catch (BadPositionCategoryException cannotHappen) {
				// can not happen: category is correctly set up
			}
			return doc;
		}
		
		/**
		 * Removes leading tabs and spaces from the given string. If the string
		 * doesn't contain any leading tabs or spaces then the string itself is 
		 * returned.
		 */
		private String trimLeadingTabsAndSpaces(String line) {
			int size= line.length();
			int start= size;
			for (int i= 0; i < size; i++) {
				char c= line.charAt(i);
				if (!IndentManipulation.isIndentChar(c)) {
					start= i;
					break;
				}
			}
			if (start == 0)
				return line;
			else if (start == size)
				return ""; //$NON-NLS-1$
			else
				return line.substring(start);
		}
	}
}
