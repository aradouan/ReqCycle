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
package org.eclipse.reqcycle.repository.connector.local.editor.provider;

import java.util.Collection;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.reqcycle.repository.data.IDataModelManager;
import org.eclipse.reqcycle.repository.data.types.IRequirementType;
import org.eclipse.ziggurat.inject.ZigguratInject;

import DataModel.DataModelFactory;
import DataModel.DataModelPackage;
import DataModel.Requirement;
import DataModel.provider.RequirementSectionItemProvider;


/**
 * The Class CustomRequirementSectionItemProvider.
 */
public class CustomRequirementSectionItemProvider extends RequirementSectionItemProvider {

	IDataModelManager manager = ZigguratInject.make(IDataModelManager.class);

	/**
	 * Instantiates a new custom requirement section item provider.
	 * 
	 * @param adapterFactory
	 *        the adapter factory
	 */
	public CustomRequirementSectionItemProvider(AdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	@Override
	public String getText(Object object) {
		String text = "";

		String id = ((Requirement)object).getId();
		String name = ((Requirement)object).getName();

		if(id != null && !id.isEmpty()) {
			text += "[ id : " + id;
		}

		if(name != null && !name.isEmpty()) {
			text += text.isEmpty() ? "[ " : " | ";
			text += "name : " + name;
		}

		if(!text.isEmpty()) {
			text += " ]";
		}

		return "Requirement " + text;
	}

	@Override
	protected void collectNewChildDescriptors(Collection<Object> newChildDescriptors, Object object) {
		//FIXME : Use element Data Model to get possible children
		//Gets Dynamic Data Model possible children
		for(IRequirementType type : manager.getAllRequirementTypes()) {
			newChildDescriptors.add(createChildParameter(DataModelPackage.Literals.SECTION__CHILDREN, type.createInstance()));
		}
		newChildDescriptors.add(createChildParameter(DataModelPackage.Literals.SECTION__CHILDREN, DataModelFactory.eINSTANCE.createSection()));
	}
}
