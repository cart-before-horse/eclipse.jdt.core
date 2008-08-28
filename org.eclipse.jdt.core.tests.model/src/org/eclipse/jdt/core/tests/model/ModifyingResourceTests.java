/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.model;

import java.io.*;
import java.util.StringTokenizer;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;
import org.eclipse.jdt.core.tests.util.Util;
import org.eclipse.jdt.internal.core.JavaElement;

public class ModifyingResourceTests extends AbstractJavaModelTests {

public ModifyingResourceTests(String name) {
	super(name);
}
public ModifyingResourceTests(String name, int tabs) {
	super(name, tabs);
}
protected void assertElementDescendants(String message,  String expected, IJavaElement element) throws CoreException {
	String actual = expandAll(element);
	if (!expected.equals(actual)){
	 	System.out.print(displayString(actual, 3));
	 	System.out.println(",");
	}
	assertEquals(
		message,
		expected,
		actual);
}
protected void assertStatus(String expected, IStatus status) {
	String actual = status.getMessage();
	if (!expected.equals(actual)) {
	 	System.out.print(Util.displayString(actual, 2));
	 	System.out.println(",");
	}
	assertEquals(expected, actual);
}
protected void assertStatus(String message, String expected, IStatus status) {
	String actual = status.getMessage();
	if (!expected.equals(actual)) {
	 	System.out.print(Util.displayString(actual, 2));
	 	System.out.println(",");
	}
	assertEquals(message, expected, actual);
}
/**
 * E.g. <code>
 * org.eclipse.jdt.tests.core.ModifyingResourceTests.generateClassFile(
 *   "A",
 *   "public class A {\n" +
 *   "}")
 */
public static void generateClassFile(String className, String javaSource) throws IOException {
	String cu = "d:/temp/" + className + ".java";
	Util.createFile(cu, javaSource);
	BatchCompiler.compile(cu + " -d d:/temp -classpath " + System.getProperty("java.home") + "/lib/rt.jar", new PrintWriter(System.out), new PrintWriter(System.err), null/*progress*/);
	FileInputStream input = new FileInputStream("d:/temp/" + className + ".class");
	try {
		System.out.println("{");
		byte[] buffer = new byte[80];
		int read = 0;
		while (read != -1) {
			read = input.read(buffer);
			if (read != -1) System.out.print("\t");
			for (int i = 0; i < read; i++) {
				System.out.print(buffer[i]);
				System.out.print(", ");
			}
			if (read != -1) System.out.println();
		}
		System.out.print("}");
	} finally {
		input.close();
	}
}

protected boolean createExternalFolder(String relativePath) {
	return new File(getExternalPath(), relativePath).mkdirs();
}

protected void createExternalFile(String relativePath, String contents) {
	Util.writeToFile(contents, getExternalPath() + relativePath);
}

protected IFile createFile(String path, InputStream content) throws CoreException {
	IFile file = getFile(path);
	file.create(content, true, null);
	try {
		content.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
	return file;
}

protected IFile createFile(String path, byte[] content) throws CoreException {
	return createFile(path, new ByteArrayInputStream(content));
}

protected IFile createFile(String path, String content) throws CoreException {
	return createFile(path, content.getBytes());
}
protected IFile createFile(String path, String content, String charsetName) throws CoreException, UnsupportedEncodingException {
	return createFile(path, content.getBytes(charsetName));
}
protected File createFolder(File parent, String name) {
	File file = new File(parent, name);
	file.mkdirs();
	return file;
}
protected IFolder createFolder(String path) throws CoreException {
	return createFolder(new Path(path));
}
protected void deleteExternalResource(String relativePath) {
	deleteResource(new File(getExternalPath() + relativePath));
}
protected void deleteFile(String filePath) throws CoreException {
	deleteResource(getFile(filePath));
}
protected void deleteFolder(String folderPath) throws CoreException {
	deleteFolder(new Path(folderPath));
}
protected IFile editFile(String path, String content) throws CoreException {
	IFile file = getFile(path);
	InputStream input = new ByteArrayInputStream(content.getBytes());
	file.setContents(input, IResource.FORCE, null);
	return file;
}
/*
 * Expands (i.e. open) the given element and returns a toString() representation
 * of the tree.
 */
protected String expandAll(IJavaElement element) throws CoreException {
	StringBuffer buffer = new StringBuffer();
	this.expandAll(element, 0, buffer);
	return buffer.toString();
}
private void expandAll(IJavaElement element, int tab, StringBuffer buffer) throws CoreException {
	IJavaElement[] children = null;
	// force opening of element by getting its children
	if (element instanceof IParent) {
		IParent parent = (IParent)element;
		children = parent.getChildren();
	}
	((JavaElement)element).toStringInfo(tab, buffer);
	if (children != null) {
		for (int i = 0, length = children.length; i < length; i++) {
			buffer.append("\n");
			this.expandAll(children[i], tab+1, buffer);
		}
	}
}
protected void renameProject(String project, String newName) throws CoreException {
	getProject(project).move(new Path(newName), true, null);
}
protected IClassFile getClassFile(String path) {
	return (IClassFile)JavaCore.create(getFile(path));
}
protected IFolder getFolder(String path) {
	return getFolder(new Path(path));
}
protected IPackageFragment getPackage(String path) {
	if (path.indexOf('/', 1) != -1) { // if path as more than one segment
		IJavaElement element = JavaCore.create(this.getFolder(path));
		if (element instanceof IPackageFragmentRoot) {
			return ((IPackageFragmentRoot)element).getPackageFragment("");
		}
		return (IPackageFragment)element;
	}
	IProject project = getProject(path);
	return JavaCore.create(project).getPackageFragmentRoot(project).getPackageFragment("");
}
protected IPackageFragmentRoot getPackageFragmentRoot(String path) {
	if (path.indexOf('/', 1) != -1) { // if path as more than one segment
		if (path.endsWith(".jar")) {
			return  (IPackageFragmentRoot)JavaCore.create(getFile(path));
		}
		return (IPackageFragmentRoot)JavaCore.create(this.getFolder(path));
	}
	IProject project = getProject(path);
	return JavaCore.create(project).getPackageFragmentRoot(project);
}
protected String getSortedByProjectDeltas() {
	StringBuffer buffer = new StringBuffer();
	for (int i=0, length = this.deltaListener.deltas.length; i<length; i++) {
		IJavaElementDelta[] projects = this.deltaListener.deltas[i].getAffectedChildren();
		int projectsLength = projects.length;

		// sort by project
		IJavaElementDelta[] sorted = new IJavaElementDelta[projectsLength];
		System.arraycopy(projects, 0, sorted, 0, projectsLength);
		org.eclipse.jdt.internal.core.util.Util.sort(
			sorted,
			new  org.eclipse.jdt.internal.core.util.Util.Comparer() {
				public int compare(Object a, Object b) {
					return a.toString().compareTo(b.toString());
				}
			});

		for (int j=0; j<projectsLength; j++) {
			buffer.append(sorted[j]);
			if (j != projectsLength-1) {
				buffer.append("\n");
			}
		}
		if (i != length-1) {
			buffer.append("\n\n");
		}
	}
	return buffer.toString();
}
protected void moveFile(String sourcePath, String destPath) throws CoreException {
	getFile(sourcePath).move(getFile(destPath).getFullPath(), false, null);
}
protected void moveFolder(String sourcePath, String destPath) throws CoreException {
	this.getFolder(sourcePath).move(this.getFolder(destPath).getFullPath(), false, null);
}
protected void swapFiles(String firstPath, String secondPath) throws CoreException {
	final IFile first = getFile(firstPath);
	final IFile second = getFile(secondPath);
	IWorkspaceRunnable runnable = new IWorkspaceRunnable(	) {
		public void run(IProgressMonitor monitor) throws CoreException {
			IPath tempPath = first.getParent().getFullPath().append("swappingFile.temp");
			first.move(tempPath, false, monitor);
			second.move(first.getFullPath(), false, monitor);
			getWorkspaceRoot().getFile(tempPath).move(second.getFullPath(), false, monitor);
		}
	};
	getWorkspace().run(runnable, null);
}
protected IClassFile createClassFile(String libPath, String classFileRelativePath, String contents) throws CoreException {
	IClassFile classFile = getClassFile(libPath + "/" + classFileRelativePath);
//	classFile.getResource().delete(false, null);
	Util.delete(classFile.getResource());
	IJavaProject javaProject = classFile.getJavaProject();
	IProject project = javaProject.getProject();
	String sourcePath = project.getLocation().toOSString() + File.separatorChar + classFile.getType().getElementName() + ".java";
	String libOSPath = new Path(libPath).segmentCount() > 1 ? getFolder(libPath).getLocation().toOSString() : getProject(libPath).getLocation().toOSString();
	Util.compile(new String[] {sourcePath, contents}, javaProject.getOptions(true), libOSPath);
	project.refreshLocal(IResource.DEPTH_INFINITE, null);
	return classFile;
}
/*
 * Returns a new classpath from the given source folders and their respective exclusion/inclusion patterns.
 * The folder path is an absolute workspace-relative path.
 * The given array as the following form:
 * [<folder>, "<pattern>[|<pattern]*"]*
 * E.g. new String[] {
 *   "/P/src1", "p/A.java",
 *   "/P", "*.txt|com.tests/**"
 * }
 */
protected IClasspathEntry[] createClasspath(String[] foldersAndPatterns, boolean hasInclusionPatterns, boolean hasExclusionPatterns) {
	int length = foldersAndPatterns.length;
	int increment = 1;
	if (hasInclusionPatterns) increment++;
	if (hasExclusionPatterns) increment++;
	IClasspathEntry[] classpath = new IClasspathEntry[length/increment];
	for (int i = 0; i < length; i+=increment) {
		String src = foldersAndPatterns[i];
		IPath[] accessibleFiles = new IPath[0];
		if (hasInclusionPatterns) {
			String patterns = foldersAndPatterns[i+1];
			StringTokenizer tokenizer = new StringTokenizer(patterns, "|");
			int patternsCount =  tokenizer.countTokens();
			accessibleFiles = new IPath[patternsCount];
			for (int j = 0; j < patternsCount; j++) {
				accessibleFiles[j] = new Path(tokenizer.nextToken());
			}
		}
		IPath[] nonAccessibleFiles = new IPath[0];
		if (hasExclusionPatterns) {
			String patterns = foldersAndPatterns[i+increment-1];
			StringTokenizer tokenizer = new StringTokenizer(patterns, "|");
			int patternsCount =  tokenizer.countTokens();
			nonAccessibleFiles = new IPath[patternsCount];
			for (int j = 0; j < patternsCount; j++) {
				nonAccessibleFiles[j] = new Path(tokenizer.nextToken());
			}
		}
		IPath folderPath = new Path(src);
		classpath[i/increment] = JavaCore.newSourceEntry(folderPath, accessibleFiles, nonAccessibleFiles, null);
	}
	return classpath;
}
/*
 * Returns a new classpath from the given folders and their respective accessible/non accessible files patterns.
 * The folder path is an absolute workspace-relative path. If the given project name is non-null,
 * the folder path is considered a project path if it has 1 segment that is different from the project name.
 * The given array as the following form:
 * [<folder>, "<+|-><pattern>[|<+|-><pattern]*"]*
 * E.g. new String[] {
 *   "/P/src1", "+p/A.java",
 *   "/P", "-*.txt|+com.tests/**"
 * }
 */
protected IClasspathEntry[] createClasspath(String projectName, String[] foldersAndPatterns) {
	int length = foldersAndPatterns.length;
	IClasspathEntry[] classpath = new IClasspathEntry[length/2];
	for (int i = 0; i < length; i+=2) {
		String src = foldersAndPatterns[i];
		String patterns = foldersAndPatterns[i+1];
		classpath[i/2] = createSourceEntry(projectName, src, patterns);
	}
	return classpath;
}
public IClasspathEntry createSourceEntry(String referingProjectName, String src, String patterns) {
	StringTokenizer tokenizer = new StringTokenizer(patterns, "|");
	int ruleCount =  tokenizer.countTokens();
	IAccessRule[] accessRules = new IAccessRule[ruleCount];
	int nonAccessibleRules = 0;
	for (int j = 0; j < ruleCount; j++) {
		String rule = tokenizer.nextToken();
		int kind;
		boolean ignoreIfBetter = false;
		switch (rule.charAt(0)) {
			case '+':
				kind = IAccessRule.K_ACCESSIBLE;
				break;
			case '~':
				kind = IAccessRule.K_DISCOURAGED;
				break;
			case '?':
				kind = IAccessRule.K_NON_ACCESSIBLE;
				ignoreIfBetter = true;
				break;
			case '-':
			default:		// TODO (maxime) consider forbidding unspecified rule start; this one tolerates
							// 		shortcuts that only specify a path matching pattern
				kind = IAccessRule.K_NON_ACCESSIBLE;
				break;
		}
		nonAccessibleRules++;
		accessRules[j] = JavaCore.newAccessRule(new Path(rule.substring(1)), ignoreIfBetter ? kind | IAccessRule.IGNORE_IF_BETTER : kind);
	}

	IPath folderPath = new Path(src);
	if (referingProjectName != null && folderPath.segmentCount() == 1 && !referingProjectName.equals(folderPath.lastSegment())) {
		return JavaCore.newProjectEntry(folderPath, accessRules, true/*combine access restrictions*/, new IClasspathAttribute[0], false);
	} else {
		IPath[] accessibleFiles = new IPath[ruleCount-nonAccessibleRules];
		int accessibleIndex = 0;
		IPath[] nonAccessibleFiles = new IPath[nonAccessibleRules];
		int nonAccessibleIndex = 0;
		for (int j = 0; j < ruleCount; j++) {
			IAccessRule accessRule = accessRules[j];
			if (accessRule.getKind() == IAccessRule.K_ACCESSIBLE)
				accessibleFiles[accessibleIndex++] = accessRule.getPattern();
			else
				nonAccessibleFiles[nonAccessibleIndex++] = accessRule.getPattern();
		}
		return JavaCore.newSourceEntry(folderPath, accessibleFiles, nonAccessibleFiles, null);
	}
}
protected void deleteExternalFile(String filePath) throws CoreException {
	deleteResource(new File(filePath));
}
}
