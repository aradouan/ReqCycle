package org.eclipse.reqcycle.ocl.ui;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.ui.dialogs.WorkspaceResourceDialog;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ocl.examples.pivot.utilities.BaseResource;
import org.eclipse.reqcycle.ocl.ReqcycleOCLPlugin;
import org.eclipse.reqcycle.ocl.ui.OCLConnector.SettingBean;
import org.eclipse.reqcycle.ocl.utils.OCLEvaluationUtilities;
import org.eclipse.reqcycle.ocl.utils.OCLSourceUtilities;
import org.eclipse.reqcycle.repository.data.types.DataType;
import org.eclipse.reqcycle.repository.data.types.DataTypeAttribute;
import org.eclipse.reqcycle.repository.data.types.DataTypePackage;
import org.eclipse.reqcycle.repository.data.types.EnumeratorType;
import org.eclipse.reqcycle.repository.data.types.RequirementType;
import org.eclipse.reqcycle.repository.data.types.RequirementTypeAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class OCLPage extends WizardPage implements Listener {


	protected OCLPage(SettingBean bean) {
		super("OCL queries");
		setDescription("An OCL file, from which operations will be used from model analysis, has to be provided. " + "You can check whether all the operations needed by ReqCycle have been implemented, or if some are missing");
		this.bean = bean;
	}

	private SettingBean bean;

	/** Types Viewer Elements */
	private TableViewer tvTypes;

	private Table tTypes;

	private TableViewerColumn tvcTypesNames;

	private TableViewerColumn tvcTypesTypes;

	/** Attributes Viewer Elements */
	private TableViewer tvAttributes;

	private Table tAttributes;

	private TableViewerColumn tvcAttributesNames;

	private TableViewerColumn tvcAttributesTypes;

	/** Viewers Inputs */
	private Collection<DataType> inputTypes = new ArrayList<DataType>();

	private Collection<DataTypeAttribute> inputAttributes = new ArrayList<DataTypeAttribute>();

	/** Viewers Selected Items */
	protected DataTypePackage selectedModel;

	protected DataType selectedType;

	private Text tFile;

	private Button browseButton;

	private BaseResource resource;

	@Override
	public void createControl(Composite parent) {
		Composite containerComposite = new Composite(parent, SWT.NONE);
		containerComposite.setLayout(new GridLayout(3, false));
		Label fileLabel = new Label(containerComposite, SWT.NONE);
		fileLabel.setText("Complete OCL file :");

		tFile = new Text(containerComposite, SWT.BORDER | SWT.READ_ONLY);
		tFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		tFile.setEnabled(false);
		browseButton = new Button(containerComposite, SWT.NONE);
		browseButton.setText("Browse");

		Label lblSeparator = new Label(containerComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblSeparator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));

		createTypesUi(containerComposite);
		createAttribuesUi(containerComposite);

		hookListeners();
		initDataBindings();
		setControl(containerComposite);
	}

	private void createTypesUi(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false, 3, 1));
		label.setText("Requirement types");

		//Table viewer
		Composite viewerComposite = new Composite(parent, SWT.NONE);
		viewerComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

		TableColumnLayout typesTVLayout = new TableColumnLayout();
		viewerComposite.setLayout(typesTVLayout);

		tvTypes = new TableViewer(viewerComposite, SWT.BORDER);
		tvTypes.setContentProvider(ArrayContentProvider.getInstance());

		tTypes = tvTypes.getTable();
		tTypes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		tTypes.setHeaderVisible(true);
		tTypes.setLinesVisible(true);

		//Columns
		tvcTypesNames = createTableViewerColumn(tvTypes, "Name", SWT.None);
		tvcTypesNames.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if(element instanceof DataType) {
					return ((DataType)element).getName();
				}
				return super.getText(element);
			}
		});
		typesTVLayout.setColumnData(tvcTypesNames.getColumn(), new ColumnWeightData(20, 100, true));

		tvcTypesTypes = createTableViewerColumn(tvTypes, "Operation check", SWT.None);
		tvcTypesTypes.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if(element instanceof RequirementType) {
					if(resource == null) {
						return "An ocl file has to be selected and must compile";
					}
					IStatus testPresentForDataType = OCLSourceUtilities.isOperationPresent((RequirementType)element, resource);
					return testPresentForDataType.getMessage();
				}
				return "No need for an operation";
			}
		});
		typesTVLayout.setColumnData(tvcTypesTypes.getColumn(), new ColumnWeightData(20, 200, true));

		updateDataTypes();
		tvTypes.setInput(inputTypes);

	}

	@Override
	public boolean isPageComplete() {
		if (resource == null){
			return false;
		}
		try {
			OCLEvaluationUtilities.compileOCL(resource);
		} catch (Exception e) {
			setErrorMessage("Could not compile ocl file : " + e.getMessage());
			return false;
		}
		setErrorMessage("");
		return true;
	}
	
	private void createAttribuesUi(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false, 3, 1));
		label.setText("Requirement attributes");

		//Table Viewer
		Composite viewerComposite = new Composite(parent, SWT.NONE);
		viewerComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

		TableColumnLayout attributeTVLayout = new TableColumnLayout();
		viewerComposite.setLayout(attributeTVLayout);

		tvAttributes = new TableViewer(viewerComposite);
		tvAttributes.setContentProvider(ArrayContentProvider.getInstance());
		tAttributes = tvAttributes.getTable();
		tAttributes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		tAttributes.setHeaderVisible(true);
		tAttributes.setLinesVisible(true);

		//Columns
		tvcAttributesNames = createTableViewerColumn(tvAttributes, "Name", SWT.None);
		tvcAttributesNames.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if(element instanceof RequirementTypeAttribute) {
					return ((RequirementTypeAttribute)element).getName();
				} else if(element instanceof EnumeratorType) {
					return ((EnumeratorType)element).getName();
				}
				return super.getText(element);
			}
		});
		attributeTVLayout.setColumnData(tvcAttributesNames.getColumn(), new ColumnWeightData(20, 100, true));

		tvcAttributesTypes = createTableViewerColumn(tvAttributes, "Operation check", SWT.None);
		tvcAttributesTypes.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if(element instanceof RequirementTypeAttribute) {
					if(resource == null) {
						return "An ocl file has to be selected and must compile";
					}
					RequirementTypeAttribute attribute = (RequirementTypeAttribute)element;
					IStatus testPresentForDataType = OCLSourceUtilities.isOperationPresent(attribute, resource);
					return testPresentForDataType.getMessage();
				}
				if(element instanceof EnumeratorType) {
					return "-";
				}
				return super.getText(element);
			}
		});
		attributeTVLayout.setColumnData(tvcAttributesTypes.getColumn(), new ColumnWeightData(20, 100, true));

		tvAttributes.setInput(inputAttributes);

	}


	public void hookListeners() {

		tvTypes.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				inputAttributes.clear();

				ISelection selection = event.getSelection();
				if(selection instanceof IStructuredSelection) {
					Object obj = ((IStructuredSelection)selection).getFirstElement();
					if(obj instanceof DataType) {
						selectedType = (DataType)obj;
						Collection<? extends DataTypeAttribute> attributes = selectedType.getAttributes();
						Iterable<? extends DataTypeAttribute> filtered = Iterables.filter(attributes, new Predicate<DataTypeAttribute>() {

							@Override
							public boolean apply(DataTypeAttribute arg0) {
								if(arg0 instanceof RequirementTypeAttribute) {
									if("uri".equals(((RequirementTypeAttribute)arg0).getName())) {
										return false;
									}
								}
								return true;
							}

						});

						Iterables.addAll(inputAttributes, filtered);
					}
				}
				tvAttributes.refresh();
			}
		});

		browseButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				BaseWorkbenchContentProvider contentProvider = new BaseWorkbenchContentProvider();
				WorkspaceResourceDialog dialog = new WorkspaceResourceDialog(Display.getCurrent().getActiveShell(), labelProvider, contentProvider);
				dialog.addFilter(filter);
				dialog.setAllowMultiple(false);
				dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
				dialog.setValidator(validator);
				int open = dialog.open();
				if(open == 0) {
					IFile iFile = dialog.getSelectedFiles()[0];
					String location = iFile.getFullPath().toOSString();
					URI oclURI = URI.createPlatformResourceURI(location, true);
					tFile.setText(location);

					ResourceSet resourceSet = new ResourceSetImpl();
					OCLPage.this.resource = OCLSourceUtilities.loadOCLResource(resourceSet, oclURI);
					tvTypes.refresh();
					tvAttributes.refresh();
				}
			}
		});
	}

	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeTextFileURITextObserveWidget = WidgetProperties.text(SWT.Modify).observe(tFile);
		IObservableValue uriBeanObserveValue = PojoProperties.value("oclUri").observe(bean);
		bindingContext.bindValue(observeTextFileURITextObserveWidget, uriBeanObserveValue, null, null);
		//
		return bindingContext;
	}

	public TableViewerColumn createTableViewerColumn(TableViewer viewer, String title, int style) {
		TableViewerColumn viewerColumn = new TableViewerColumn(viewer, style);
		TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	@Override
	public void handleEvent(Event event) {
		updateDataTypes();
		if(tvTypes != null) {
			tvTypes.refresh();
		}
	}

	private void updateDataTypes() {
		DataTypePackage dataPackage = bean.getDataPackage();
		if(dataPackage != null) {
			Collection<RequirementType> dataTypes = dataPackage.getDataTypes();
			inputTypes.clear();
			inputTypes.addAll(dataTypes);
		}
	}

	protected static ViewerFilter filter = new ViewerFilter() {

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return true;
		}
	};

	protected static ISelectionStatusValidator validator = new ISelectionStatusValidator() {

		@Override
		public IStatus validate(Object[] selection) {
			if(selection.length == 1) {
				Object o = selection[0];
				if(o instanceof IFile && "ocl".equals(((IFile)o).getFileExtension())) {
					return Status.OK_STATUS;
				}
			}
			return new Status(IStatus.ERROR, ReqcycleOCLPlugin.PLUGIN_ID, "Select a single OCL file");
		}
	};

	/**
	 * Label provider for the workspace resource dialog.
	 */
	protected static ILabelProvider labelProvider = new WorkbenchLabelProvider() {

		@Override
		public Color getForeground(Object element) {
			return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
		}

		@Override
		public Color getBackground(Object element) {
			return Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
		}
	};

}