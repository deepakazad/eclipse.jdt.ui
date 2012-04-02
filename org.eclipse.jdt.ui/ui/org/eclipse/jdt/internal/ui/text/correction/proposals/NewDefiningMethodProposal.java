/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class NewDefiningMethodProposal extends AbstractMethodCorrectionProposal {

	private final IMethodBinding fMethodBinding;
	private final String[] fParamNames;

	private final MethodDeclaration fMethodDecl;

	public NewDefiningMethodProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode, ITypeBinding binding, IMethodBinding method, MethodDeclaration decl, String[] paramNames,
			int relevance) {
		super(label,targetCU,invocationNode,binding,relevance,null);
		fMethodBinding= method;
		fMethodDecl= decl;
		fParamNames= paramNames;

		ImageDescriptor desc= JavaElementImageProvider.getMethodImageDescriptor(binding.isInterface() || binding.isAnnotation(), method.getModifiers());
		setImage(JavaPlugin.getImageDescriptorRegistry().get(desc));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.proposals.AbstractMethodCorrectionProposal#isConstructor()
	 */
	@Override
	protected boolean isConstructor() {
		return fMethodBinding.isConstructor();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.proposals.AbstractMethodCorrectionProposal#addNewParameters(org.eclipse.jdt.core.dom.rewrite.ASTRewrite, java.util.List, java.util.List)
	 */
	@Override
	protected void addNewParameters(ASTRewrite rewrite, List<String> takenNames, List<SingleVariableDeclaration> params) throws CoreException {
		AST ast= rewrite.getAST();
		ImportRewrite importRewrite= getImportRewrite();
		ITypeBinding[] bindings= fMethodBinding.getParameterTypes();

		IJavaProject project= getCompilationUnit().getJavaProject();
		String[][] paramNames= StubUtility.suggestArgumentNamesWithProposals(project, fParamNames);

		List parameters= fMethodDecl.parameters();
		for (int i= 0; i < bindings.length; i++) {
			ITypeBinding curr= bindings[i];
			SingleVariableDeclaration var= (SingleVariableDeclaration) parameters.get(i);
			var.isVarargs();

			String[] proposedNames= paramNames[i];

			SingleVariableDeclaration newParam= ast.newSingleVariableDeclaration();

			newParam.setType(importRewrite.addImport(curr, ast));
			newParam.setName(ast.newSimpleName(proposedNames[0]));

			params.add(newParam);

			String groupId= "arg_name_" + i; //$NON-NLS-1$
			addLinkedPosition(rewrite.track(newParam.getName()), false, groupId);

			for (int k= 0; k < proposedNames.length; k++) {
				addLinkedPositionProposal(groupId, proposedNames[k], null);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.proposals.AbstractMethodCorrectionProposal#getNewName(org.eclipse.jdt.core.dom.rewrite.ASTRewrite)
	 */
	@Override
	protected SimpleName getNewName(ASTRewrite rewrite) {
		AST ast= rewrite.getAST();
		SimpleName nameNode= ast.newSimpleName(fMethodBinding.getName());
		return nameNode;
	}

	private int evaluateModifiers() {
		if (getSenderBinding().isInterface()) {
			return 0;
		} else {
			int modifiers= fMethodBinding.getModifiers();
			if (Modifier.isPrivate(modifiers)) {
				modifiers |= Modifier.PROTECTED;
			}
			return modifiers & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.ABSTRACT | Modifier.STRICTFP);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.proposals.AbstractMethodCorrectionProposal#addNewModifiers(org.eclipse.jdt.core.dom.rewrite.ASTRewrite, org.eclipse.jdt.core.dom.ASTNode, java.util.List)
	 */
	@Override
	protected void addNewModifiers(ASTRewrite rewrite, ASTNode targetTypeDecl, List<IExtendedModifier> modifiers) {
		modifiers.addAll(rewrite.getAST().newModifiers(evaluateModifiers()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.proposals.AbstractMethodCorrectionProposal#getNewMethodType(org.eclipse.jdt.core.dom.rewrite.ASTRewrite)
	 */
	@Override
	protected Type getNewMethodType(ASTRewrite rewrite) throws CoreException {
		return getImportRewrite().addImport(fMethodBinding.getReturnType(), rewrite.getAST());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.proposals.AbstractMethodCorrectionProposal#addNewExceptions(org.eclipse.jdt.core.dom.rewrite.ASTRewrite, java.util.List)
	 */
	@Override
	protected void addNewExceptions(ASTRewrite rewrite, List<Name> exceptions) throws CoreException {
		AST ast= rewrite.getAST();
		ImportRewrite importRewrite= getImportRewrite();
		ITypeBinding[] bindings= fMethodBinding.getExceptionTypes();
		for (int i= 0; i < bindings.length; i++) {
			String typeName= importRewrite.addImport(bindings[i]);
			Name newNode= ASTNodeFactory.newName(ast, typeName);
			exceptions.add(newNode);

			addLinkedPosition(rewrite.track(newNode), false, "exc_type_" + i); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.proposals.AbstractMethodCorrectionProposal#addNewTypeParameters(org.eclipse.jdt.core.dom.rewrite.ASTRewrite, java.util.List, java.util.List)
	 */
	@Override
	protected void addNewTypeParameters(ASTRewrite rewrite, List<String> takenNames, List<TypeParameter> params) throws CoreException {

	}

}
