package de.tum.in.jmoped.translator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.gjt.jclasslib.bytecode.AbstractInstruction;
import org.gjt.jclasslib.bytecode.BranchInstruction;
import org.gjt.jclasslib.bytecode.ImmediateByteInstruction;
import org.gjt.jclasslib.bytecode.ImmediateShortInstruction;
import org.gjt.jclasslib.bytecode.Opcodes;
import org.gjt.jclasslib.io.ClassFileReader;
import org.gjt.jclasslib.structures.AccessFlags;
import org.gjt.jclasslib.structures.CPInfo;
import org.gjt.jclasslib.structures.ClassFile;
import org.gjt.jclasslib.structures.InvalidByteCodeException;
import org.gjt.jclasslib.structures.MethodInfo;
import org.gjt.jclasslib.structures.attributes.CodeAttribute;
import org.gjt.jclasslib.structures.attributes.LocalVariableTableAttribute;
import org.gjt.jclasslib.structures.attributes.LocalVariableTableEntry;
import org.gjt.jclasslib.structures.constants.ConstantClassInfo;
import org.gjt.jclasslib.structures.constants.ConstantNameAndTypeInfo;
import org.gjt.jclasslib.structures.constants.ConstantReference;
import org.gjt.jclasslib.structures.constants.ConstantUtf8Info;

import de.tum.in.jmoped.translator.stub.StubManager;

/**
 * The translator's utility class.
 * 
 * @author suwimont
 *
 */
public class TranslatorUtils {
	
	/**
	 * Determines where Java is installed.
	 */
	public static final String JAVA_HOME;
	
	static {
		// Sets JAVA_HOME variable to where Java is.
		String javaHome = System.getenv("JAVA_HOME");
		if (javaHome != null) {
			JAVA_HOME = javaHome;
		} else {
			// System.getProperty("java.home") outputs e.g. /opt/jdk1.5.0_04/jre
			javaHome = System.getProperty("java.home");
			// But we want only /opt/jdk1.5.0_04
			JAVA_HOME = javaHome.substring(0, javaHome.length() - 4);
		}
	}

	/**
	 * Searches all classpath directories for the file and all jars, otherwise
	 * looks into the rt.jar
	 * 
	 * @param className packages are expected to be separated by a '/' 
	 * or '.' , e.g. "java/util/StringTokenizer" or "java.util.StringTokenizer"
	 * @throws Exception if there was an error reading the class file or the
	 * file was not found
	 */
	public static ClassFile findClassFile(String className,
			String[] searchPaths) throws InvalidByteCodeException, IOException {
		
		// Creates file name of the class name.
		String filename = className.replaceAll("/|\\.", File.separator) + ".class";
		
		// Joins the given search paths and the classpaths
		ArrayList<String> paths = new ArrayList<String>();
		if (searchPaths != null)
			paths.addAll(Arrays.asList(searchPaths));
		String[] classpaths = System.getProperty("java.class.path",".")
				.split(System.getProperty("path.separator"));
		if (classpaths != null)
			paths.addAll(Arrays.asList(classpaths));
		
		ClassFile cf;
		for (String path : paths) {
			File f = new File(path);
			// if path is a file, it must be a jar file -> looks inside
			if (f.isFile()) {
				cf = findClassFile(new JarFile(f), filename);
				if (cf != null) return cf;
			}
			// if path is a folder
			else {
				File possibleClassFile = new File(f, filename);
				if (possibleClassFile.isFile()) {
					return ClassFileReader.readFromFile(possibleClassFile);
				}
			}
		}
		
		// Finds in stubs
		cf = StubManager.findClassFile(className);
		if (cf != null) return cf;
		
		// Falls back to JAVA_HOME/jre/lib/rt.jar
		File rtFile = new File(JAVA_HOME,  File.separator + "jre" 
				+ File.separator + "lib" + File.separator + "rt.jar");
		if (rtFile.isFile()) {
			cf = findClassFile(new JarFile(rtFile), filename);
			if (cf != null) return cf;
		}
		
		// Falls back to JAVA_HOME/lib/rt.jar
		rtFile = new File(JAVA_HOME, File.separator + "lib" + File.separator + "rt.jar");
		if (rtFile.isFile()) {
			cf = findClassFile(new JarFile(rtFile), filename);
			if (cf != null) return cf;
		}
		
		// Falls back to JAVA_HOME/Classes/classes.jar (MacOS X)
		rtFile = new File(JAVA_HOME, File.separator + "Classes" + File.separator + "classes.jar");
		if (rtFile.isFile()) {
			cf = findClassFile(new JarFile(rtFile), filename);
			if (cf != null) return cf;
		}
		
		throw new FileNotFoundException(String.format(
				"File for class %s not found in %s or rt.jar.", 
				className, paths));
	}
	
	/**
	 * Converts the filesystem path to a jar path and looks it up in the
	 * jarFile. 
	 * <p>
	 * If successful a class file object is returned.
	 * @throws Exception
	 */
	public static ClassFile findClassFile(JarFile jarFile, String path)
			throws InvalidByteCodeException, IOException {
		
	    try {
	    	String jarPath = path.replace(File.separatorChar, '/');
	        JarEntry jarEntry = jarFile.getJarEntry(jarPath);
	        if (jarEntry != null) {
	        	return ClassFileReader.readFromInputStream
					(jarFile.getInputStream(jarEntry));
	        }
	    } 
	    finally {
	        jarFile.close();
	    }	
	    return null;
	}

	/**
	 * Gets the class name from the label.
	 * The method returns <code>null</code> if the label is non-standard.
	 * 
	 * @param label
	 * @return
	 */
	public static String extractClassName(String label) {
		
		int index = label.indexOf(".");
		if (index == -1) return null;
		return label.substring(0, index);
	}
	
	/**
	 * Gets the method name from the label.
	 * The method returns <code>null</code> if the label is non-standard.
	 * 
	 * @param label
	 * @return
	 */
	public static String extractMethodName(String label) {
		
		return label.substring(label.indexOf(".") + 1, label.indexOf("("));
	}
	
	public static String extractMethodDescriptorFromLabel(String label) {
		
		return label.substring(label.indexOf("("), getOffsetPosition(label));
	}
	
	public static String extractMethodDescriptor(String formattedName) {
		
		return formattedName.substring(formattedName.indexOf('('));
	}
	
	/**
	 * Returns the position of the first digit of the offset number.
	 * @return -1 if there is none
	 */
	public static int getOffsetPosition(String label) {
		
		int offset = label.length() - 1;
		
		// Bypasses non-digit suffix
		while (offset >= 0 && !Character.isDigit(label.charAt(offset)))
			offset--;
		
		// Goes through the digits
		while (offset >= 0 && Character.isDigit(label.charAt(offset)))
			offset--;
		
		// At least one digit
		if (offset < label.length() - 1)
			return offset + 1;
			
		return -1;
	}
	
	/**
	 * Extracts the offset number at the end of the label string.
	 * 
	 * @param label
	 * @return o if there is no number
	 */
	public static int getOffset(String label)
	{
		int offset = getOffsetPosition(label);
		if (offset >= 0) {
			int to = label.lastIndexOf(SUFFIX_SEPARATOR);
			if (to == -1) to = label.length();
			return Integer.parseInt(label.substring(offset, to));
		}
		return 0;
	}
	
	public static String trimOffset(String label) {
		
		return label.substring(0, getOffsetPosition(label));
	}
	
	public static String formatName(String className, String method) {
		
		return className + "." + method;
	}
	
	/**
	 * Formats the class name, method name, and method descriptor.
	 * The formatted name is a string that concatenates the three strings
	 * in the above-mentioned order with a dot between class and method name. 
	 * 
	 * @param className the class name.
	 * @param methodName the method name.
	 * @param methodDesc the method descriptor.
	 * @return the formatted name.
	 */
	public static String formatName(String className, String methodName, String methodDesc) {
		
		return className + "." + methodName + methodDesc;
	}
	
	public static String formatName(String[] name) {
		
		return formatName(name[0], name[1], name[2]);
	}
	
	public static String formatName(String className, String methodName, 
			String methodDesc, int offset) {
		
		return formatName(formatName(className, methodName, methodDesc), offset);
	}
	
	public static String formatName(String[] name, int offset) {
		
		return formatName(name[0], name[1], name[2], offset);
	}
	
	public static String formatName(String prefix, int offset) {
		
		return prefix + offset;
	}
	
	private static final String SUFFIX_SEPARATOR = "#";
	private static final String ASSERTION_ERROR = "AssertionError";
	private static final String HO_ERROR = "HO";
	private static final String NPE_ERROR = "NPE";
	private static final String IOOB_ERROR = "IOOB";
	
	/**
	 * Creates a new label by suffixing the <code>label</code>.
	 * The new label indicates an assertion error at the <code>label</code>. 
	 * 
	 * @param label the label.
	 * @return the assertion error label.
	 */
	public static String formatAssertionName(String label) {
		return label + SUFFIX_SEPARATOR + ASSERTION_ERROR;
	}
	
	/**
	 * Returns <code>true</code> if the <code>label</code> is an assertion name.
	 * 
	 * @param label the label.
	 * @return <code>true</code> if the <code>label</code> is an assertion name.
	 */
	public static boolean isAssertionName(String label) {
		return label.endsWith(ASSERTION_ERROR);
	}
	
	/**
	 * Creates a new label by suffixing the <code>label</code>.
	 * The new label indicates a heap overflow at the <code>label</code>. 
	 * 
	 * @param label the label.
	 * @return the assertion error label.
	 */
	public static String formatHeapOverflowName(String label) {
		return label + SUFFIX_SEPARATOR + HO_ERROR;
	}
	
	/**
	 * Returns <code>true</code> if the <code>label</code> is a heap-overflow name.
	 * 
	 * @param label the label.
	 * @return <code>true</code> if the <code>label</code> is a heap-overflow name.
	 */
	public static boolean isHeapOverflowName(String label) {
		return label.endsWith(HO_ERROR);
	}
	
	/**
	 * Creates a new label by suffixing the <code>label</code>.
	 * The new label indicates a NullPointerException at the <code>label</code>. 
	 * 
	 * @param label the label.
	 * @return the assertion error label.
	 */
	public static String formatNpeName(String label) {
		return label + SUFFIX_SEPARATOR + NPE_ERROR;
	}
	
	/**
	 * Returns <code>true</code> if the <code>label</code> is a NullPointerException name.
	 * 
	 * @param label the label.
	 * @return <code>true</code> if the <code>label</code> is a NullPointerException name.
	 */
	public static boolean isNpeName(String label) {
		return label.endsWith(NPE_ERROR);
	}
	
	/**
	 * Creates a new label by suffixing the <code>label</code>.
	 * The new label indicates a ArrayIndexOutOfBoundsException at the <code>label</code>. 
	 * 
	 * @param label the label.
	 * @return the assertion error label.
	 */
	public static String formatIoobName(String label) {
		return label + SUFFIX_SEPARATOR + IOOB_ERROR;
	}
	
	/**
	 * Returns <code>true</code> if the <code>label</code> is a ArrayIndexOutOfBoundsException name.
	 * 
	 * @param label the label.
	 * @return <code>true</code> if the <code>label</code> is a ArrayIndexOutOfBoundsException name.
	 */
	public static boolean isIoobName(String label) {
		return label.endsWith(IOOB_ERROR);
	}
	
	public static String trimSuffix(String label) {
		
		return label.substring(0, label.lastIndexOf(SUFFIX_SEPARATOR));
	}
	
	private static final String THREADSAVE = "save";
	
	public static String formatSave(int i) {
		return THREADSAVE + i;
	}
	
	private static final String WAITFLAG = "waitflag";
	
	public static String formatWaitFlag(int i) {
		return WAITFLAG + i;
	}
	
	private static final String WAITFOR = "waitfor";
	
	public static String formatWaitFor(int i) {
		return WAITFOR + i;
	}
	
	public static String branchTarget(String name, AbstractInstruction ainst) {
		
		int to = ainst.getOffset() + ((BranchInstruction) ainst).getBranchOffset();
		return formatName(name, to);
	}
	
	public static int immediateByte(AbstractInstruction ainst) {
		
		return ((ImmediateByteInstruction) ainst).getImmediateByte();
	}
	
	public static int immediateShort(AbstractInstruction ainst) {
		
		return ((ImmediateShortInstruction) ainst).getImmediateShort();
	}
	
	public static String getNewarrayType(AbstractInstruction ainst) {
		int atype = TranslatorUtils.immediateByte(ainst);
		switch (atype) {
		case Opcodes.NEWARRAY_T_BOOLEAN: 	return "[Z";
		case Opcodes.NEWARRAY_T_BYTE: 		return "[B";
		case Opcodes.NEWARRAY_T_CHAR: 		return "[C";
		case Opcodes.NEWARRAY_T_DOUBLE: 	return "[D";
		case Opcodes.NEWARRAY_T_FLOAT: 		return "[F";
		case Opcodes.NEWARRAY_T_INT: 		return "[I";
		case Opcodes.NEWARRAY_T_LONG: 		return "[J";
		case Opcodes.NEWARRAY_T_SHORT: 		return "[S";
		}
		return null;
	}
	
	public static String removeArrayPrefix(String s) {
		if (s == null) return null;
		return s.replaceAll("\\[+L", "").replace(";", "");
	}
	
	public static String insertArrayType(String s, int dim) {
		if (dim == 0) return s;
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < dim; i++)
			b.append('[');
		b.append('L');
		b.append(s);
		b.append(';');
		return b.toString();
	}
	
	public static int countDims(String type) {
		int dim = 0;
		while (type.charAt(dim) == '[')
			dim++;
		return dim;
	}
	
	/**
	 * Resolves class name from the abstraction instruction.
	 * The instruction must be followed by (short) index to the constant pool.
	 *
	 * @param cp the constant pool.
	 * @param ainst the abstract instruction.
	 * @return the class name.
	 */
	public static String resolveClassName(CPInfo[] cp,
			AbstractInstruction ainst) {
		
		ImmediateShortInstruction isInst = (ImmediateShortInstruction) ainst;
		ConstantClassInfo ccInfo = (ConstantClassInfo) cp[isInst.getImmediateShort()];
		
		try {
			return StubManager.removeStub(ccInfo.getName());
		} catch (InvalidByteCodeException e) {
			System.err.println("Unexpected error: " + ainst);
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Returns names of class and method and method's descriptor 
	 * in <code>invokevirtual</code>, <code>invokespecial</code>, 
	 * and <code>invokestatic</code> instruction.
	 *
	 * @param classFile the jclasslib class file.
	 * @param inst the <code>invokevirtual</code>, 
	 *				<code>invokespecial</code>, or <code>invokestatic</code>
	 *				instruction.
	 * @return array of string containing the class name at index 0,
	 *				 method name at index 1, and method descriptor at
	 *				 index 2.
	 */
	public static String[] getReferencedName(CPInfo[] cp,
			AbstractInstruction ainst) {
			
		try {
			ImmediateShortInstruction isInst = (ImmediateShortInstruction) ainst;
			ConstantReference constRef = (ConstantReference) cp[isInst.getImmediateShort()];
			ConstantClassInfo ccInfo = constRef.getClassInfo();
			ConstantNameAndTypeInfo cnatInfo = constRef.getNameAndTypeInfo();
			
			String[] invokedName = new String[3];
			invokedName[0] = StubManager.removeStub(ccInfo.getName());
			invokedName[1] = cnatInfo.getName();
			invokedName[2] = StubManager.removeStub(cnatInfo.getDescriptor());
			
			return invokedName;
		} catch (InvalidByteCodeException e) {
			System.err.println("Unexpected error: " + ainst);
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Unused.
	 * 
	 * @param names
	 * @param name0
	 * @param name1
	 * @param name2
	 * @return
	 */
	public static boolean nameEquals(String[] names, String name0, String name1, String name2) {
		
		if (names.length != 3) return false;
		return names[0].equals(name0) && names[1].equals(name1) && names[2].equals(name2);
	}
	
	public static boolean nameStartsWith(String[] names, String name0, String name1) {
		
		if (names.length < 2) return false;
		return names[0].equals(name0) && names[1].equals(name1);
	}
	
//	public static boolean isVoid(ClassFile cf, AbstractInstruction ainst)
//			throws InvalidByteCodeException{
//		
//		ImmediateShortInstruction isInst = (ImmediateShortInstruction) ainst;
//		ConstantReference constRef = (ConstantReference) 
//			cf.getConstantPoolEntry(isInst.getImmediateShort(), 
//					ConstantReference.class);
//		ConstantNameAndTypeInfo cnatInfo = constRef.getNameAndTypeInfo();
//		return cnatInfo.getDescriptor().endsWith("V");
//	}
	
	public static String getCalledDescriptor(CPInfo[] cp, AbstractInstruction ainst) {
		
		ImmediateShortInstruction isInst = (ImmediateShortInstruction) ainst;
		ConstantReference constRef = (ConstantReference) cp[isInst.getImmediateShort()];
		ConstantNameAndTypeInfo cnatInfo;
		try {
			cnatInfo = constRef.getNameAndTypeInfo();
			return cnatInfo.getDescriptor();
		} catch (InvalidByteCodeException e) {
			System.err.println("Unexpected error: " + ainst);
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Returns <code>true</code> if the descriptor is from a method
	 * that returns void.
	 * 
	 * @param desc the method's descriptor.
	 * @return  <code>true</code> if the descriptor is from a method
	 * that returns void.
	 */
	public static boolean isVoid(String desc) {
		return desc.endsWith("V");
	}
	
	/**
	 * Returns <code>true</code> if the method returns void.
	 * 
	 * @param method the method.
	 * @return  <code>true</code> if the method returns void.
	 */
	public static boolean isVoid(MethodInfo method) {
		try {
			return isVoid(method.getDescriptor());
		} catch (InvalidByteCodeException e) {
			throw new TranslatorError("Error while reading a method");
		}
	}
	
	public static boolean isStatic(int flag) {
		
		return (flag & AccessFlags.ACC_STATIC) != 0;
	}
	
	/**
	 * Returns the number of parameters given the method's descriptor in 
	 * the JVM standard.
	 *
	 * @param descriptor the method descriptor.
	 * @return the number of parameters.
	 */
	public static int countParams(String descriptor) {
		
		int count = 0;
		String param = descriptor.substring(1, descriptor.indexOf(")"));
		
		for (int i = 0; i < param.length(); i++) {
			if (param.charAt(i) == '[') {
				continue;
			}
			if (param.charAt(i) == 'L') {
				while (param.charAt(i) != ';') {
					i++;
				}
			}
			count++;
		}
		
		return count;
	}
	
	public static int countParams(CPInfo[] cp, AbstractInstruction ainst) {
		
		return countParams(getCalledDescriptor(cp, ainst));
	}
	
	/**
	 * Returns the array of parameter types as specified by the
	 * <code>descriptor</code>. 
	 * 
	 * If <code>stc</code> is <code>true</code>
	 * (indicating that the corresponding method is static), the size
	 * of the array is the number of the parameters.
	 * The array determines the sequence of parameter types, i.e.
	 * the first element corresponds to the first parameter type and so on.
	 * An element of the array is <code>true</code> if and only if
	 * the parameter type is long or double.
	 * 
	 * If <code>stc</code> is <code>false</code>, the size of the array 
	 * is the number of parameters plus one. The first element is always
	 * <code>false</code>. The rest of the array determines the sequence
	 * of parameter types as in the previous case.
	 * 
	 * @param stc <code>true</code> if the corresponding method is static.
	 * @param descriptor the method descriptor.
	 * @return the array of booleans determining whether the parameters are
	 * 			long or double.
	 */
	public static boolean[] doubleParams(boolean stc, String descriptor) {
		
		List<String> params = getParamTypes(descriptor);
		boolean[] array = new boolean[(stc) ? params.size() : params.size() + 1];
		int i = (stc) ? 0 : 1;
		for (String param : params) {
			array[i++] = param.equals("J") || param.equals("D");
		}
		return array;
	}
	
	public static boolean[] doubleParams(boolean stc, MethodInfo method) {
		try {
			return doubleParams(stc, method.getDescriptor());
		} catch (InvalidByteCodeException e) {
			throw new TranslatorError("Error while reading a method");
		}
	}
	
	/**
	 * Matches two parameter lists.
	 * 
	 * @param A
	 * @param B
	 * @return
	 */
	static boolean matchParams(List<String> A, List<String> B) {
		
		if (A.size() != B.size()) return false;
		
		for (int i = 0; i < A.size(); i++) {
			String a = A.get(i);
			String b = B.get(i);
			if (a.equals(b)) continue;
			
			if (!a.startsWith("L") || !b.startsWith("Q")) return false;
			int index = a.lastIndexOf('/');
			if (index == -1) return false;
			if (!a.substring(index + 1).equals(b.substring(1))) return false;
		}
		
		return true;
	}
	
	/**
	 * Returns the list of parameter types as specified by the 
	 * <code>descriptor</code>.
	 * 
	 * @param descriptor the method descriptor.
	 * @return the list of parameter types.
	 */
	public static List<String> getParamTypes(String descriptor) {
		
		String param = descriptor.substring(1, descriptor.indexOf(")"));
		StringBuilder tmp;
		
		List<String> paramList = new ArrayList<String>();
		for (int i = 0; i < param.length(); i++) {
			switch (param.charAt(i)) {		
				case '[': 
					tmp = new StringBuilder();
					while (param.charAt(i) == '[') {
						tmp.append("[");
						i++;
					}
					if (param.charAt(i) == 'L') {
						while (param.charAt(i) != ';') {
							tmp.append(param.charAt(i));
							i++;
						}
						tmp.append(";");
					} else {
						tmp.append(param.charAt(i));
					}
					paramList.add(tmp.toString());
					break;
					
				case 'L':
				case 'Q':
					tmp = new StringBuilder();
					while (param.charAt(i) != ';') {
						tmp.append(param.charAt(i));
						i++;
					}
					tmp.append(";");
					paramList.add(tmp.toString());
					break;
					
				default:
					paramList.add(new String(new char[] {param.charAt(i)}));
			}
		}
		
		return paramList;
	}
	
	public static LocalVariableTableEntry[] getLocalVariableTableEntries(MethodInfo mi) {
			
		CodeAttribute codeAttr = (CodeAttribute) mi.findAttribute(CodeAttribute.class);
		LocalVariableTableAttribute lvtAttr = (LocalVariableTableAttribute)
				codeAttr.findAttribute(LocalVariableTableAttribute.class);
		return lvtAttr.getLocalVariableTable();
	}
	
	public static String getConstantUtf8(CPInfo[] cp, int index) {
		
		return ((ConstantUtf8Info) cp[index]).getString();
	}
}
