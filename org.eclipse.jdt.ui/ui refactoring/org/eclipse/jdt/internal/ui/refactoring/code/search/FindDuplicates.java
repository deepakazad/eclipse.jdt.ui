/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [extract method] Does not replace similar code in parent class of anonymous class - https://bugs.eclipse.org/bugs/show_bug.cgi?id=160853
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [extract method] Extract method and continue https://bugs.eclipse.org/bugs/show_bug.cgi?id=48056
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [extract method] should declare method static if extracted from anonymous in static method - https://bugs.eclipse.org/bugs/show_bug.cgi?id=152004
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.code.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.refactoring.code.search.DuplicateSnippetFinder.Match;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;

/**
 * TODO: This file can be deleted now....
 */
public class FindDuplicates {

	private SelectionAnalyzer fAnalyzer;

	private Match[] fDuplicates;

	private final ICompilationUnit fCu;

	private final int fSelectionOffset;

	private final int fSelectionLength;

	private CompilationUnit fRoot;

	public FindDuplicates(ICompilationUnit cu, int offset, int length) {
		fCu= cu;
		fSelectionOffset= offset;
		fSelectionLength= length;

		fAnalyzer= new SelectionAnalyzer(Selection.createFromStartLength(fSelectionOffset, fSelectionLength), true);
		findDuplicates();
	}

	private void findDuplicates() {
		if (fRoot == null) {
			fRoot= RefactoringASTParser.parseWithASTProvider(fCu, true, null);
		}
		fRoot.accept(fAnalyzer);

		String name= ""; //$NON-NLS-1$
		try {
			IPackageDeclaration[] packageDeclarations= fCu.getPackageDeclarations();
			name= packageDeclarations[0].getElementName();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<IType> types= findTypes(name, null);
		for (Iterator<IType> iterator= types.iterator(); iterator.hasNext();) {
			IType type= iterator.next();
			ITypeRoot typeRoot= type.getTypeRoot();
			CompilationUnit compilationUnit= new RefactoringASTParser(ASTProvider.SHARED_AST_LEVEL).parse(typeRoot, null, true, ASTProvider.SHARED_AST_STATEMENT_RECOVERY,
					ASTProvider.SHARED_BINDING_RECOVERY, null);
			List<AbstractTypeDeclaration> types2= compilationUnit.types();

			for (Iterator<AbstractTypeDeclaration> iterator2= types2.iterator(); iterator2.hasNext();) {
				AbstractTypeDeclaration typeDeclaration= iterator2.next();
				ITypeBinding binding= typeDeclaration.resolveBinding();
				if (binding.getKey().equals(type.getKey())) {
					fDuplicates= DuplicateSnippetFinder.perform(typeDeclaration, fAnalyzer.getSelectedNodes());
					System.out.println(fDuplicates.length + " Matches found in " + typeDeclaration.getName());
					System.out.println(Arrays.toString(fDuplicates));
				}
			}
		}
	}

	private List<IType> findTypes(String name, IProgressMonitor monitor) {
		final List<IType> matches= new ArrayList<IType>();


		/*try {
			IJavaProject javaProject= fCu.getJavaProject();
			IPackageFragmentRoot[] packageFragmentRoots= javaProject.getPackageFragmentRoots();
			packageFragmentRoots[0].getChildren();
		} catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/


		//SearchEngine.createJavaSearchScope(new IJavaElement[] { fCu.getJavaProject() }, IJavaSearchScope.SOURCES);
		JavaSearchScopeFactory factory= JavaSearchScopeFactory.getInstance();
		IJavaSearchScope scope= factory.createWorkspaceScope(IJavaSearchScope.SOURCES);

		SearchEngine searchEngine= new SearchEngine();

		String packageName= "*"; //$NON-NLS-1$
		String typeName= "*"; //$NON-NLS-1$
		
		try {
			searchEngine.searchAllTypeNames(packageName.toCharArray(), getSearchFlags(), typeName.toCharArray(),
					getSearchFlags(), IJavaSearchConstants.TYPE, scope, new TypeNameMatchRequestor() {
						@Override
						public void acceptTypeNameMatch(TypeNameMatch match) {
							matches.add(match.getType());
						}
					}, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return matches;
	}

	private static int getSearchFlags() {
		return SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_ERASURE_MATCH;
	}


}
