/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contributions for
 *								bug 349326 - [1.7] new warning for missing try-with-resources
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;

public class UnresolvedReferenceBinding extends ReferenceBinding {

ReferenceBinding resolvedType;
TypeBinding[] wrappers;
UnresolvedReferenceBinding prototype;
TypeBinding enclosingType;

UnresolvedReferenceBinding(char[][] compoundName, PackageBinding packageBinding) {
	this.compoundName = compoundName;
	this.sourceName = compoundName[compoundName.length - 1]; // reasonable guess
	this.fPackage = packageBinding;
	this.wrappers = null;
	this.prototype = this;
	computeId();
}

public UnresolvedReferenceBinding(UnresolvedReferenceBinding prototype) {
	super(prototype);
	this.resolvedType = prototype.resolvedType;
	this.wrappers = null;
	this.prototype = prototype.prototype;
}

public TypeBinding clone(TypeBinding outerType) {
	if (this.resolvedType != null)
		throw new IllegalStateException();
	UnresolvedReferenceBinding copy = new UnresolvedReferenceBinding(this);
	copy.enclosingType = outerType;
	this.addWrapper(copy, null);
	return copy;
}

void addWrapper(TypeBinding wrapper, LookupEnvironment environment) {
	if (this.resolvedType != null) {
		// the type reference B<B<T>.M> means a signature of <T:Ljava/lang/Object;>LB<LB<TT;>.M;>;
		// when the ParameterizedType for Unresolved B is created with args B<T>.M, the Unresolved B is resolved before the wrapper is added
		wrapper.swapUnresolved(this, this.resolvedType, environment);
		return;
	}
	if (this.wrappers == null) {
		this.wrappers = new TypeBinding[] {wrapper};
	} else {
		int length = this.wrappers.length;
		System.arraycopy(this.wrappers, 0, this.wrappers = new TypeBinding[length + 1], 0, length);
		this.wrappers[length] = wrapper;
	}
}
public String debugName() {
	return toString();
}
public int depth() {
	// we don't yet have our enclosing types wired, but we know the nesting depth from our compoundName:
	int last = this.compoundName.length-1;
	return CharOperation.occurencesOf('$', this.compoundName[last]);
}
public boolean hasTypeBit(int bit) {
	// shouldn't happen since we are not called before analyseCode(), but play safe:
	return false;
}

public TypeBinding prototype() {
	return this.prototype;
}

ReferenceBinding resolve(LookupEnvironment environment, boolean convertGenericToRawType) {
	if (this != this.prototype) {
		this.prototype.resolve(environment, convertGenericToRawType);
		return this.resolvedType;
	}
    ReferenceBinding targetType = this.resolvedType;
	if (targetType == null) {
		targetType = this.fPackage.getType0(this.compoundName[this.compoundName.length - 1]);
		if (targetType == this) {
			targetType = environment.askForType(this.compoundName);
		}
		if (targetType == null || targetType == this) { // could not resolve any better, error was already reported against it
			// report the missing class file first - only if not resolving a previously missing type
			if ((this.tagBits & TagBits.HasMissingType) == 0 && !environment.mayTolerateMissingType) {
				environment.problemReporter.isClassPathCorrect(
					this.compoundName,
					environment.unitBeingCompleted,
					environment.missingClassFileLocation);
			}
			// create a proxy for the missing BinaryType
			targetType = environment.createMissingType(null, this.compoundName);
		}
		setResolvedType(targetType, environment);
	}
	if (convertGenericToRawType) {
		targetType = (ReferenceBinding) environment.convertUnresolvedBinaryToRawType(targetType);
	}
	return targetType;
}
void setResolvedType(ReferenceBinding targetType, LookupEnvironment environment) {
	if (this.resolvedType == targetType) return; // already resolved

	// targetType may be a source or binary type
	this.resolvedType = targetType;
	// must ensure to update any other type bindings that can contain the resolved type
	// otherwise we could create 2 : 1 for this unresolved type & 1 for the resolved type
	if (this.wrappers != null)
		for (int i = 0, l = this.wrappers.length; i < l; i++)
			this.wrappers[i].swapUnresolved(this, targetType, environment);
	environment.updateCaches(this, targetType);
}

public void swapUnresolved(UnresolvedReferenceBinding unresolvedType, ReferenceBinding unannotatedType, LookupEnvironment environment) {
	if (this.resolvedType != null) return;
	ReferenceBinding annotatedType = (ReferenceBinding) unannotatedType.clone(this.enclosingType != null ? this.enclosingType : unannotatedType.enclosingType());
	
	this.resolvedType = annotatedType;
	annotatedType.setTypeAnnotations(getTypeAnnotations(), environment.globalOptions.isAnnotationBasedNullAnalysisEnabled);
	annotatedType.id = unannotatedType.id = this.id;
	if (this.wrappers != null)
		for (int i = 0, l = this.wrappers.length; i < l; i++)
			this.wrappers[i].swapUnresolved(this, annotatedType, environment);
	environment.updateCaches(this, annotatedType);
}
public String toString() {
	if (this.hasTypeAnnotations())
		return super.annotatedDebugName() + "(unresolved)"; //$NON-NLS-1$
	return "Unresolved type " + ((this.compoundName != null) ? CharOperation.toString(this.compoundName) : "UNNAMED"); //$NON-NLS-1$ //$NON-NLS-2$
}
}
