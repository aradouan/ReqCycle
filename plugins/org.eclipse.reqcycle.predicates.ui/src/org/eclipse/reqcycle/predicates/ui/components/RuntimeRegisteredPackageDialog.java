package org.eclipse.reqcycle.predicates.ui.components;

import java.util.Arrays;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.presentation.EcoreEditorPlugin;
import org.eclipse.emf.ecore.provider.EcoreEditPlugin;
import org.eclipse.emf.edit.ui.provider.ExtendedImageRegistry;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class RuntimeRegisteredPackageDialog extends ElementListSelectionDialog {

	public RuntimeRegisteredPackageDialog(Shell parent) {
		super(parent, new LabelProvider() {

			@Override
			public Image getImage(Object element) {
				return ExtendedImageRegistry.getInstance().getImage(EcoreEditPlugin.INSTANCE.getImage("full/obj16/EPackage"));
			}
		});

		setMultipleSelection(true);
		setMessage(EcoreEditorPlugin.INSTANCE.getString("_UI_SelectRegisteredPackageURI"));
		setFilter("*");
		setTitle(EcoreEditorPlugin.INSTANCE.getString("_UI_PackageSelection_label"));
	}

	protected void updateElements() {
		Object[] result = EPackage.Registry.INSTANCE.keySet().toArray(new Object[EPackage.Registry.INSTANCE.size()]);
		Arrays.sort(result);
		setListElements(result);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite result = (Composite)super.createDialogArea(parent);

		updateElements();

		return result;
	}

}
