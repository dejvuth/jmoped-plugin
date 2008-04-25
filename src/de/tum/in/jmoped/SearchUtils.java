package de.tum.in.jmoped;

import java.io.File;
import java.net.URL;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.osgi.framework.Bundle;

public class SearchUtils {
	
	private static Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
	
	public static URL findURL(String className) {
		if (bundle == null) {
			return null;
		}
		String fileName = String.format("bin/%s.class", className)
				.replace('/', File.separatorChar);
		Path path = new Path(fileName);
		return FileLocator.find(bundle, path, null);
	}

	static IJavaElement getJavaElement(IEditorPart editor,
			ITextSelection selection) {
		
		int offset = selection.getOffset();
		IEditorInput editorInput = editor.getEditorInput();
		IWorkingCopyManager manager = JavaUI.getWorkingCopyManager();
		ICompilationUnit unit = manager.getWorkingCopy(editorInput);
		if (unit != null) {
			try {
				// returns null if unit itself is found at offset
				IJavaElement element = unit.getElementAt(offset);
				return element != null ? element : unit;
			}
			catch (JavaModelException e) {
				System.out.println(e);
			}
		}
		IClassFile classFile = (IClassFile) editorInput.getAdapter(IClassFile.class);
		if (classFile != null) {
			try {
				IJavaElement element = classFile.getElementAt(offset);
				return element != null ? element : classFile;
			}
			catch (JavaModelException e) {
				System.out.println(e);
			}
		}
		IJavaElement je = (IJavaElement)editorInput.getAdapter(IJavaElement.class);
		if (je != null) {
			return je;
		}
		return (IJavaElement)editor.getAdapter(IJavaElement.class);
	}
	
	static IType getClassType(IJavaElement element)
	{
		if (element instanceof ICompilationUnit) {
			return ((ICompilationUnit)element).getType(Signature.getQualifier
					(element.getElementName()));
		}
		else if (element instanceof IClassFile) {
			return ((IClassFile)element).getType();
		}
		else if (element instanceof IType) {
			return (IType)element;
		}
		else if (element instanceof IMember) {
			return ((IMember)element).getDeclaringType();
		}
		return null;
	}
	
	static IMethod getMainMethod(IType type) 
	{
		if (type != null && type.exists()) {
			try {
				IMethod[] methods = type.getMethods();
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].isMainMethod()) {
						return methods[i];
					}
				}
			}
			catch (JavaModelException jme) {
				jme.printStackTrace();
			}
		}
		return null;
	}
	
	static IMethod getMethod(IEditorPart editor, ITextSelection selection) {
		
		IJavaElement javaElement = getJavaElement(editor, selection);
		if (javaElement instanceof IMethod)
			return (IMethod) javaElement;
		
		return getMainMethod(getClassType(javaElement));
	}
	
	public static String findSource(IResource[] resources, String name) 
			throws CoreException {
		
		for (IResource resource : resources) {
			
			if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				String candidate = file.getLocation().toString();
				if (candidate.endsWith(name))
					return candidate;
			}
			
			else if (resource instanceof IFolder) {
				IFolder folder = (IFolder) resource;
				String found = findSource(folder.members(), name);
				if (found != null) return found;
			}
			
			else if (resource instanceof IProject) {
				IProject project = (IProject) resource;
				String found = findSource(project.members(), name);
				if (found != null) return found;
			}
		}
		
		return null;
	}
	
	/**
	 * Finds resource of the class specified by className.
	 * The method returns <code>null</code> if not found.
	 * 
	 * @param className the class name.
	 * @return the resource of the class.
	 * @throws JavaModelException
	 */
	public static IResource findResource(String className) throws JavaModelException {
		
		IJavaProject[] projects = JavaCore
				.create(ResourcesPlugin.getWorkspace().getRoot())
				.getJavaProjects();
		if (projects == null) return null;
		
		String name = className.replace('/', '.').replace('$', '.');
		for (int i = 0; i < projects.length; i++) {
			IType type = projects[i].findType(name);
			if (type == null) continue;
			return type.getResource();
		}
		
		return null;
	}
	
	public static IDocument getDocument(IPath path) {
		
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		ITextFileBuffer buffer = manager.getTextFileBuffer(path, LocationKind.IFILE);
		if (buffer == null) return null;
		return buffer.getDocument();
	}
}
