/*****************************************************************************
 * Copyright (c) 2013 AtoS.
 *
 *    
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Anass RADOUANI (AtoS) anass.radouani@atos.net - Initial API and implementation
 *
 *****************************************************************************/
package org.eclipse.reqcycle.repository.ui.views;


import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.reqcycle.dnd.DragRequirementSourceAdapter;
import org.eclipse.reqcycle.repository.data.IDataModelManager;
import org.eclipse.reqcycle.repository.data.types.DataModel;
import org.eclipse.reqcycle.repository.data.util.DataUtil;
import org.eclipse.reqcycle.repository.ui.Activator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ziggurat.inject.ZigguratInject;

import DataModel.Contained;
import DataModel.Scope;

public class ScopeView extends ViewPart {

	private static final String VIEW_ID = "org.eclipse.reqcycle.repository.ui.views.requirement.scope";

	private IDataModelManager dataManager = ZigguratInject.make(IDataModelManager.class);

	private Collection<Scope> scopes = new ArrayList<Scope>();

	private Collection<DataModel> dataModels = new ArrayList<DataModel>();
	
	/** Selected Scope */
	private Scope scope;
	
	private DataModel dataModel;

	private Collection<Contained> requirements = new ArrayList<Contained>();

	/** Requirement Viewer */
	private TreeViewer viewer;

	/** Scope combo viewer */
	private ComboViewer cvScope;

	private ComboViewer cvDataModel;

	/** refresh view button */
//	private Button refreshBtn;

	protected Action newInstanceAction;

	private Action refreshAction;

	public ScopeView() {
	}

	@Override
	public void createPartControl(Composite parent) {

		initInputs();
		
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label lblDataModel = new Label(composite, SWT.NONE);
		lblDataModel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblDataModel.setText("Data Model :");

		cvDataModel = new ComboViewer(composite, SWT.NONE);
		cvDataModel.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		cvDataModel.setContentProvider(ArrayContentProvider.getInstance());

		//TODO : use scope generated label provider 
		cvDataModel.setLabelProvider(new LabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof DataModel) {
					return ((DataModel) element).getName();
				}
				return "";
			}
		});

		cvDataModel.setInput(dataManager.getAllDataModels());
		
//		refreshBtn = new Button(composite, SWT.PUSH);
//		refreshBtn.setImage(Activator.getImageDescriptor("icons/refresh.gif").createImage());
//		
//		refreshBtn.setToolTipText("Refresh View");

		Label lblScope = new Label(composite, SWT.NONE);
		lblScope.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblScope.setText("Scope :");

		cvScope = new ComboViewer(composite, SWT.NONE);
		cvScope.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		cvScope.setContentProvider(ArrayContentProvider.getInstance());

		//TODO : use scope generated label provider 
		cvScope.setLabelProvider(new LabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof Scope) {
					return ((Scope) element).getName();
				}
				return "";
			}
		});

		cvScope.setInput(scopes);


		viewer = new TreeViewer(composite, SWT.BORDER);
		viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		viewer.setContentProvider(new ITreeContentProvider() {

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public boolean hasChildren(Object element) {
//				if (element instanceof Contained) {
//					 EList<EAttribute> eAllAttributes = ((Contained) element).eClass().getEAllAttributes();
//					for (EAttribute eAttribute : eAllAttributes) {
//						if	(((Contained) element).eGet(eAttribute, true) != null) {
//							return true;
//						}
//					}
//				}
				return false;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return ArrayContentProvider.getInstance().getElements(inputElement);
			}

			@Override
			public Object[] getChildren(Object parentElement) {
//				Collection<Object> result = new ArrayList<Object>();
//				if (parentElement instanceof Contained) {
//					EList<EAttribute> eAllAttributes = ((Contained) parentElement).eClass().getEAllAttributes();
//					for (EAttribute eAttribute : eAllAttributes) {
//						value = ((Contained) parentElement).eGet(eAttribute, true);
//					}
//				}
				return null;
			}
		});
		viewer.setLabelProvider(new LabelProvider() {

			@Override
			public String getText(Object element) {
				return DataUtil.getLabel(element);
			}
		});
		viewer.setInput(requirements);

		int dndOperations = DND.DROP_COPY | DND.DROP_MOVE;

		Transfer[] transfers;
		transfers = new Transfer[]{ PluginTransfer.getInstance() };

		DragRequirementSourceAdapter listener = new DragRequirementSourceAdapter(viewer);
		ZigguratInject.inject(listener);
		viewer.addDragSupport(dndOperations, transfers, listener);
		getViewSite().setSelectionProvider(viewer);

		makeActions();
		contributeToActionBars();
		hookListeners();
	}

	private void initInputs() {
		dataModels.clear();
		scopes.clear();
		dataModels.addAll(dataManager.getAllDataModels());
	}

	private void hookListeners() {
		
		cvDataModel.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection) {
					Object firstElement = ((IStructuredSelection) selection).getFirstElement();
					if (firstElement instanceof DataModel) {
						DataModel selectedDataModel = (DataModel) firstElement;
						if (selectedDataModel != dataModel) {
							dataModel = selectedDataModel;
							scope = null;
							setScopes(dataModel.getScopes());
						}
					}
				}
			}
		});
		
		cvScope.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if(selection instanceof IStructuredSelection) {
					Object element = ((IStructuredSelection)selection).getFirstElement();
					if(element instanceof Scope) {
						Scope selectedScope = (Scope)element;
						if (selectedScope != scope) {
							scope = selectedScope;
							setRequirements(scope.getRequirements());
						}
					}
				}
			}
		});

//		refreshBtn.addSelectionListener(new SelectionAdapter() {
//
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				if(viewer != null) {
//					refresh();
//				}
//			}
//		});
	}

	protected void refresh() {
		setDataModels(dataManager.getAllDataModels());
		if (dataModel != null) {
			setScopes(dataModel.getScopes());
		}
		if (scope != null) {
			setRequirements(scope.getRequirements());
		}
	}

	private void setDataModels(Collection<DataModel> allDataModels) {
		dataModels.clear();
		dataModels.addAll(allDataModels);
		if (cvDataModel != null) {
			cvDataModel.refresh();
			if (dataModel != null) {
				cvDataModel.setSelection(new StructuredSelection(dataModel));
			}
		}
	}

	protected void setRequirements(EList<Contained> requirements) {
		this.requirements.clear();
		this.requirements.addAll(scope.getRequirements());
		if (viewer != null) {
			viewer.refresh();
		}
	}

	protected void setScopes(Collection<Scope> scopes) {
		this.scopes.clear();
		this.scopes.addAll(scopes);
		if (cvScope != null) {
			cvScope.refresh();
			if (scope != null) {
				cvScope.setSelection(new StructuredSelection(scopes));
			}
		}
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	/**
	 * Fills the action Bars
	 */
	protected void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	/**
	 * Fills the local ToolBar
	 * 
	 * @param manager
	 *        The tool Bar manager
	 */
	protected void fillLocalToolBar(IToolBarManager manager) {
		manager.add(refreshAction);
		manager.add(newInstanceAction);
		manager.add(new Separator());
	}

	protected void makeActions() {

		refreshAction = new Action("Refresh") {

			@Override
			public void run() {
				refresh();
			}
		};
		refreshAction.setImageDescriptor(Activator.getImageDescriptor("icons/refresh.gif"));
		
		newInstanceAction = new Action("New Instance") {

			@Override
			public void run() {
				createNewView();
			}
		};
		newInstanceAction.setImageDescriptor(Activator.getImageDescriptor("icons/newView.gif"));
	}

	protected static IViewPart createNewView() {
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		int nbView = 0;
		for(IViewReference ref : activePage.getViewReferences()) {
			if(ref.getId().startsWith(VIEW_ID)) {
				nbView++;
			}
		}
		// increment to have the second view named #2
		nbView++;
		IViewPart view = null;
		try {
			view = activePage.showView(VIEW_ID, VIEW_ID + "_" + nbView, IWorkbenchPage.VIEW_ACTIVATE);
			// view.
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return view;

	}
	
}