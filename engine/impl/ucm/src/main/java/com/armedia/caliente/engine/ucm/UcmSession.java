/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.ucm;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.TrackedUse;

import com.armedia.caliente.engine.ucm.model.UcmFSObject;
import com.armedia.caliente.engine.ucm.model.UcmFile;
import com.armedia.caliente.engine.ucm.model.UcmFileHistory;
import com.armedia.caliente.engine.ucm.model.UcmFileNotFoundException;
import com.armedia.caliente.engine.ucm.model.UcmFileRevisionNotFoundException;
import com.armedia.caliente.engine.ucm.model.UcmFolder;
import com.armedia.caliente.engine.ucm.model.UcmFolderNotFoundException;
import com.armedia.caliente.engine.ucm.model.UcmModel;
import com.armedia.caliente.engine.ucm.model.UcmModel.ObjectHandler;
import com.armedia.caliente.engine.ucm.model.UcmObjectNotFoundException;
import com.armedia.caliente.engine.ucm.model.UcmRenditionInfo;
import com.armedia.caliente.engine.ucm.model.UcmRenditionNotFoundException;
import com.armedia.caliente.engine.ucm.model.UcmRevision;
import com.armedia.caliente.engine.ucm.model.UcmServiceException;
import com.armedia.commons.utilities.function.CheckedConsumer;
import com.armedia.commons.utilities.function.CheckedTriConsumer;

import oracle.stellent.ridc.IdcClientConfig;
import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.IdcClientManager;
import oracle.stellent.ridc.IdcContext;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataFactory;
import oracle.stellent.ridc.protocol.ServiceResponse;
import oracle.stellent.ridc.protocol.intradoc.IntradocClient;

public class UcmSession implements TrackedUse {

	private final IntradocClient client;
	private final IdcContext userContext;
	private final UcmModel model;
	private long lastUsed = 0;

	public UcmSession(UcmModel model, IntradocClient client, IdcContext userContext) {
		this.model = model;
		this.client = client;
		this.userContext = userContext;
	}

	public IntradocClient getClient() {
		return this.client;
	}

	public IdcClientConfig getConfig() {
		return this.client.getConfig();
	}

	public IdcContext getUserContext() {
		return this.userContext;
	}

	public DataBinder createBinder() {
		return this.client.createBinder();
	}

	public IdcClientManager getClientManager() {
		return this.client.getClientManager();
	}

	public DataFactory getDataFactory() {
		return this.client.getDataFactory();
	}

	public String getVersion() {
		return this.client.getVersion();
	}

	public void initialize() throws IdcClientException {
		this.client.initialize();
	}

	public boolean isCompatible(String version) {
		return this.client.isCompatible(version);
	}

	public boolean isInitialized() {
		return this.client.isInitialized();
	}

	public void logout() throws IdcClientException {
		this.client.logout(this.userContext);
	}

	private ServiceResponse sendRequest(DataBinder dataBinder) throws IdcClientException {
		try {
			return this.client.sendRequest(this.userContext, dataBinder);
		} finally {
			this.lastUsed = System.currentTimeMillis();
		}
	}

	public void setDataFactory(DataFactory dataFactory) {
		this.client.setDataFactory(dataFactory);
	}

	public void setInitialized(boolean initialized) {
		this.client.setInitialized(initialized);
	}

	@Override
	public long getLastUsed() {
		return this.lastUsed;
	}

	public ServiceResponse callService(String service) throws UcmServiceException {
		return callService(service, null);
	}

	public ServiceResponse callService(String service, CheckedConsumer<DataBinder, UcmServiceException> prep)
		throws UcmServiceException {
		if (StringUtils.isEmpty(service)) {
			throw new IllegalArgumentException(String.format("Illegal service name [%s]", service));
		}
		DataBinder binder = createBinder();
		if (prep != null) {
			prep.accept(binder);
		}
		// Do this last to override anything that prep might do
		binder.putLocal("IdcService", service.toUpperCase());
		try {
			return sendRequest(binder);
		} catch (IdcClientException e) {
			throw new UcmServiceException(String.format("Exception caught while invoking the service [%s]", service),
				e);
		}
	}

	public UcmFSObject getObject(String path) throws UcmServiceException, UcmObjectNotFoundException {
		return this.model.getObject(this, path);
	}

	public UcmFolder getParentFolder(UcmFSObject object) throws UcmFolderNotFoundException, UcmServiceException {
		return this.model.getFolder(this, object.getParentURI());
	}

	public UcmFolder getFolder(String path) throws UcmServiceException, UcmFolderNotFoundException {
		return this.model.getFolder(this, path);
	}

	public UcmFolder getFolder(URI uri) throws UcmServiceException, UcmFolderNotFoundException {
		return this.model.getFolder(this, uri);
	}

	public UcmFile getFile(String path) throws UcmServiceException, UcmFileNotFoundException {
		return this.model.getFile(this, path);
	}

	public UcmFile getFile(URI uri) throws UcmServiceException, UcmFileNotFoundException {
		return this.model.getFile(this, uri);
	}

	public UcmFile getFileRevision(UcmRevision revision)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return this.model.getFileRevision(this, revision);
	}

	public UcmFile getFileRevision(UcmFile file)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return this.model.getFileRevision(this, file);
	}

	public UcmFileHistory getFileHistory(String path)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return this.model.getFileHistory(this, path);
	}

	public UcmFileHistory getFileHistory(UcmFile file)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return this.model.getFileHistory(this, file);
	}

	public int iterateFolderContents(UcmFolder folder, ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		return this.model.iterateFolderContents(this, folder, handler);
	}

	public Map<String, UcmFSObject> getFolderContents(UcmFolder folder)
		throws UcmServiceException, UcmFolderNotFoundException {
		return this.model.getFolderContents(this, folder);
	}

	public int iterateFolderContentsRecursive(UcmFolder folder, boolean recurseShortcuts, ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		return this.model.iterateFolderContentsRecursive(this, folder, recurseShortcuts, handler);
	}

	public Collection<URI> getURISearchResults(String query) throws UcmServiceException {
		return this.model.getURISearchResults(this, query);
	}

	public Collection<URI> getURISearchResults(String query, int pageSize) throws UcmServiceException {
		return this.model.getURISearchResults(this, query, pageSize);
	}

	public long iterateURISearchResults(String query,
		CheckedTriConsumer<UcmSession, Long, URI, UcmServiceException> handler) throws UcmServiceException {
		return this.model.iterateURISearchResults(this, query, handler);
	}

	public long iterateURISearchResults(String query, int pageSize,
		CheckedTriConsumer<UcmSession, Long, URI, UcmServiceException> handler) throws UcmServiceException {
		return this.model.iterateURISearchResults(this, query, pageSize, handler);
	}

	public long iterateDocumentSearchResults(String query, ObjectHandler handler) throws UcmServiceException {
		return this.model.iterateDocumentSearchResults(this, query, handler);
	}

	public long iterateDocumentSearchResults(String query, int pageSize, ObjectHandler handler)
		throws UcmServiceException {
		return this.model.iterateDocumentSearchResults(this, query, pageSize, handler);
	}

	public Collection<UcmFile> getDocumentSearchResults(String query) throws UcmServiceException {
		return this.model.getDocumentSearchResults(this, query);
	}

	public Collection<UcmFile> getDocumentSearchResults(String query, int pageSize) throws UcmServiceException {
		return this.model.getDocumentSearchResults(this, query, pageSize);
	}

	public Collection<UcmFSObject> getFolderContentsRecursive(UcmFolder folder, boolean followShortCuts)
		throws UcmServiceException, UcmFolderNotFoundException {
		return this.model.getFolderContentsRecursive(this, folder, followShortCuts);
	}

	public InputStream getInputStream(UcmFile file)
		throws UcmServiceException, UcmFileRevisionNotFoundException, UcmRenditionNotFoundException {
		return getInputStream(file, null);
	}

	public InputStream getInputStream(UcmFile file, String rendition)
		throws UcmServiceException, UcmFileRevisionNotFoundException, UcmRenditionNotFoundException {
		return this.model.getInputStream(this, file, rendition);
	}

	public Map<String, UcmRenditionInfo> getRenditions(UcmFile file)
		throws UcmServiceException, UcmFileRevisionNotFoundException {
		return this.model.getRenditions(this, file);
	}

	public UcmFile getFileByGUID(String guid) throws UcmServiceException, UcmFileNotFoundException {
		return this.model.getFileByGUID(this, guid);
	}

	public UcmFolder getFolderByGUID(String guid) throws UcmServiceException, UcmFolderNotFoundException {
		return this.model.getFolderByGUID(this, guid);
	}
}