/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved.   This program and the accompanying materials
are made available under the terms of the Common Public License v0.5
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v05.html
 
Contributors:
	Daniel Megert - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Displays an IWorkingSetSelectionDialog and sets the selected 
 * working set in the action group's view.
 * 
 * @since 2.0
 */
public class SelectWorkingSetAction extends Action {
	private Shell fShell;
	private IWorkingSet fWorkingSet;
	private WorkingSetFilterActionGroup fActionGroup;

	/**
	 * Creates an instance of this class.
	 * 
	 * @param window the workbench window to use to determine 
	 * 	the active workbench page
	 */
	public SelectWorkingSetAction(WorkingSetFilterActionGroup actionGroup, Shell shell) {
		super(WorkingSetMessages.getString("SelectWorkingSetAction.text")); //$NON-NLS-1$
		Assert.isNotNull(actionGroup);
		setToolTipText(WorkingSetMessages.getString("SelectWorkingSetAction.toolTip")); //$NON-NLS-1$
		
		fShell= shell;
		fActionGroup= actionGroup;
	}
	
	/*
	 * Overrides method from Action
	 */
	public void run() {
		if (fShell == null)
			fShell= JavaPlugin.getActiveWorkbenchShell();
		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog= manager.createWorkingSetSelectionDialog(fShell, false);
		IWorkingSet workingSet= fActionGroup.getWorkingSet();
		if (workingSet != null)
			dialog.setSelection(new IWorkingSet[]{workingSet});

		if (dialog.open() == Window.OK) {
			IWorkingSet[] result= dialog.getSelection();
			if (result != null && result.length > 0) {
				fActionGroup.setWorkingSet(result[0], true);
				manager.addRecentWorkingSet(result[0]);
			}
			else
				fActionGroup.setWorkingSet(null, true);
		}
	}
}
