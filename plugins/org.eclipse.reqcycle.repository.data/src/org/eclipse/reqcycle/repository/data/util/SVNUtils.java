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
package org.eclipse.reqcycle.repository.data.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.reqcycle.repository.data.IDataManager;
import org.eclipse.reqcycle.traceability.model.Link;
import org.eclipse.reqcycle.traceability.storage.IStorageProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.ISVNConnector.Depth;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNEntryReference;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.ziggurat.inject.ZigguratInject;

import RequirementSourceConf.RequirementSource;
import RequirementSourceData.AbstractElement;
import RequirementSourceData.RequirementsContainer;
import RequirementSourceData.Section;

/**
 * The Class SVNUtils
 * FIXME : Replace by two svn plugin, one for traceability and the other for requirements
 */
public class SVNUtils {

	/** The svn connector factory. */
	static ISVNConnectorFactory svnConnectorFactory = CoreExtensionsManager.instance().getSVNConnectorFactory();

	/** The data manager. */
	static IDataManager dataManager = ZigguratInject.make(IDataManager.class);

	/** The storage provider. */
	static IStorageProvider provider = ZigguratInject.make(IStorageProvider.class);

	/**
	 * Commits
	 * 
	 * @param path
	 *        the element to commit path
	 * @param message
	 *        the commit message
	 * @param depth
	 *        the commit depth
	 * @param options
	 *        the commit options
	 * @return long[] commit result
	 * @throws SVNConnectorException
	 *         the SVN connector exception
	 */
	public static long[] commit(String path, String message, int depth, long options) throws SVNConnectorException {
		final ISVNConnector connector = svnConnectorFactory.newInstance();
		long[] l = connector.commit(new String[]{ path }, message, null, depth, options, null, new SVNNullProgressMonitor());
		return l;
	}

	/**
	 * Gets the resource SVN info.
	 * 
	 * @param resource
	 *        the resource
	 * @return the SVN info
	 */
	public static SVNChangeStatus getSVNInfo(IResource resource) {
		return SVNUtility.getSVNInfoForNotConnected(resource);
	}

	/**
	 * Gets the input stream from svn file url.
	 * 
	 * @param SVNFileUrl
	 *        the SVN file url
	 * @param revision
	 *        the svn revision
	 * @return the input stream from svn file url
	 */
	public static InputStream getInputStreamFromSVNFileUrl(String SVNFileUrl, SVNRevision revision) {
		final SVNEntryRevisionReference reference = new SVNEntryRevisionReference(new SVNEntryReference(SVNFileUrl), revision);

		final ISVNConnector connector = svnConnectorFactory.newInstance();

		boolean referenceAvailable = true;
		try {
			SVNUtility.info(connector, reference, ISVNConnector.Depth.EMPTY, new SVNNullProgressMonitor());
		} catch (SVNConnectorException e) {
			referenceAvailable = false;
		}

		if(referenceAvailable) {
			PipedInputStream in = new PipedInputStream(10240);
			try {
				final PipedOutputStream out = new PipedOutputStream(in);
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							connector.streamFileContent(reference, 10240, out, new SVNNullProgressMonitor());
						} catch (SVNConnectorException e) {
							e.printStackTrace();
						}
						try {
							out.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}).start();
				return in;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Synchronize svn requirement source.
	 * 
	 * @param source
	 *        the requirement source to synchronize
	 * @return the long[]
	 * @throws SVNConnectorException
	 *         the sVN connector exception
	 * @throws IOException
	 *         Signals that an I/O exception has occurred.
	 */
	public static long[] synchronizeSVNSource(RequirementSource source) throws SVNConnectorException, IOException {
		Resource mainResource = source.getContents().eResource();
		if(mainResource == null) {
			// TODO : Exception
			return null;
		}
		ResourceSet rs = mainResource.getResourceSet();

		SVNChangeStatus svnInfo = SVNUtils.getSVNInfo(WorkspaceSynchronizer.getFile(mainResource));
		if(svnInfo == null) {
			//TODO : Exception
			return null;
		}

		Resource wResource = getSVNRevisionResource(svnInfo, SVNRevision.WORKING, rs);
		Resource hResource = getSVNRevisionResource(svnInfo, SVNRevision.HEAD, rs);

		RequirementsContainer wRC = getFirstReqContainer(wResource);
		RequirementsContainer hRC = getFirstReqContainer(hResource);

		Resource resource = wRC.eResource();
		Collection<AbstractElement> difference = getNewElement(resource, hRC.getRequirements());

		if(difference != null && !difference.isEmpty()) {
			moveElementToSource(mainResource, difference);
			dataManager.save();
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Synchronize Traceability", difference.size() + " links has been added from svn");
		}

		IFile file = WorkspaceSynchronizer.getFile(mainResource);

		return commit(file.getLocationURI().getPath(), "ReqCycle Commit", Depth.infinityOrFiles(false), ISVNConnector.Options.FORCE);
	}

	/**
	 * Moves elements to the given resource if it doesn't already contain them.
	 * 
	 * @param mainResource
	 *        the main resource where elements are moved
	 * @param elements
	 *        the elements
	 */
	protected static void moveElementToSource(Resource mainResource, Collection<AbstractElement> elements) {
		for(AbstractElement abstractElement : elements) {
			EObject container = abstractElement.eContainer();
			Resource resource = abstractElement.eResource();
			if(container != null && resource != null) {
				String fragment = resource.getURIFragment(container);
				if(fragment != null && !fragment.isEmpty()) {
					EObject eobj = mainResource.getEObject(fragment);
					if(eobj instanceof Section) {
						((Section)eobj).getChildren().add(abstractElement);
					} else if(eobj instanceof RequirementsContainer) {
						((RequirementsContainer)eobj).getRequirements().add(abstractElement);
					}
				}
			}
		}
	}

	/**
	 * Gets the element not contained in the resource.
	 * 
	 * @param resource
	 *        the resource
	 * @param elements
	 *        the elements
	 * @return elements not found in the resource
	 */
	protected static Collection<AbstractElement> getNewElement(Resource resource, Collection<AbstractElement> elements) {
		Collection<AbstractElement> result = new ArrayList<AbstractElement>();
		for(AbstractElement abstractElement : elements) {
			Resource r = abstractElement.eResource();
			String fragment = r.getURIFragment(abstractElement);
			if(resource.getEObject(fragment) == null) {
				result.add(abstractElement);
			} else if(abstractElement instanceof Section) {
				result.addAll(getNewElement(resource, ((Section)abstractElement).getChildren()));
			}
		}
		return result;
	}

	/**
	 * Gets the first requirements container.
	 * 
	 * @param resource
	 *        the resource
	 * @return the first requirement container
	 */
	protected static RequirementsContainer getFirstReqContainer(Resource resource) {
		EList<EObject> contents = resource.getContents();
		if(contents != null && contents.size() > 0) {
			for(EObject eObject : contents) {
				if(eObject instanceof RequirementsContainer) {
					return (RequirementsContainer)eObject;
				}
			}
		}
		return null;
	}

	/**
	 * Gets the SVN revision resource.
	 * 
	 * @param svnChangeStatus
	 *        the svn change status
	 * @param svnRevision
	 *        the svn revision to get
	 * @param rs
	 *        the resource set containing the newly created resource
	 * @return the SVN revision resource
	 * @throws IOException
	 *         Signals that an I/O exception has occurred.
	 */
	public static Resource getSVNRevisionResource(SVNChangeStatus svnChangeStatus, SVNRevision svnRevision, ResourceSet rs) throws IOException {
		InputStream inputStreamWorking = SVNUtils.getInputStreamFromSVNFileUrl(svnChangeStatus.path, svnRevision);
		if(inputStreamWorking == null) {
			return null;
		}
		Resource resource = rs.createResource(URI.createURI(svnChangeStatus.url + "." + svnRevision.toString()));
		resource.load(inputStreamWorking, Collections.emptyMap());
		return resource;
	}

	/**
	 * Synchronize svn traceability.
	 * 
	 * @param source
	 *        the source
	 * @return the long[]
	 * @throws IOException
	 *         Signals that an I/O exception has occurred.
	 * @throws SVNConnectorException
	 *         the sVN connector exception
	 */
	public static long[] synchronizeSVNTraceability(RequirementSource source) throws IOException, SVNConnectorException {

		Resource resource = source.getContents().eResource();
		if(resource == null) {
			//TODO Exception
			return null;
		}

		IFile file = WorkspaceSynchronizer.getFile(resource);
		IProject project = file.getProject();

		TraceabilitySynchronizer t = new TraceabilitySynchronizer(project);
		ZigguratInject.inject(t);

		IFile rdfFile = t.getFile();

		if(rdfFile == null) {
			//TODO Exception
			return null;
		}

		Iterable<Link> links = t.getTraceabilityLinks(project);
		SVNChangeStatus info = getSVNInfo(rdfFile);

		if(info == null) {
			//TODO : Exception
			//Not shared
			return null;
		}

		final InputStream inputStreamHead = SVNUtils.getInputStreamFromSVNFileUrl(info.path, SVNRevision.HEAD);
		Iterable<Link> svnLinks = t.getTraceabilityLinks(inputStreamHead);

		Collection<Link> newLinks = getNewLinks(links, svnLinks);


		if(newLinks != null && !newLinks.isEmpty()) {
			t.addNewLinks(rdfFile, newLinks);
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Synchronize Traceability", newLinks.size() + " links has been added from svn");
		}

		return commit(rdfFile.getLocationURI().getPath(), "Traceability Commit", Depth.infinityOrFiles(false), ISVNConnector.Options.FORCE);

	}

	/**
	 * Gets the new links.
	 * 
	 * @param links
	 *        the links
	 * @param svnLinks
	 *        the svn links
	 * @return the new links
	 */
	private static Collection<Link> getNewLinks(Iterable<Link> links, Iterable<Link> svnLinks) {

		Map<String, Link> idToLinks = new HashMap<String, Link>();
		Collection<Link> newLinks = new ArrayList<Link>();

		for(Link link : links) {
			String schemeSpecificPart = link.getId().getSchemeSpecificPart();
			if(!idToLinks.containsKey(schemeSpecificPart)) {
				idToLinks.put(schemeSpecificPart, link);
			} else {
				//FIXME : Exception
			}
		}

		for(Link link : svnLinks) {
			String schemeSpecificPart = link.getId().getSchemeSpecificPart();
			if(!idToLinks.containsKey(schemeSpecificPart)) {
				newLinks.add(link);
			}
		}

		return newLinks;

	}
}