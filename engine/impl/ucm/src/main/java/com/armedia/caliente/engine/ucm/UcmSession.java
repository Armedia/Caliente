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
import com.armedia.caliente.engine.ucm.model.UcmRenditionInfo;
import com.armedia.caliente.engine.ucm.model.UcmRenditionNotFoundException;
import com.armedia.caliente.engine.ucm.model.UcmRevision;
import com.armedia.caliente.engine.ucm.model.UcmServiceException;

import oracle.stellent.ridc.IdcClientConfig;
import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.IdcClientManager;
import oracle.stellent.ridc.IdcContext;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataFactory;
import oracle.stellent.ridc.protocol.ServiceResponse;
import oracle.stellent.ridc.protocol.intradoc.IntradocClient;

public class UcmSession implements TrackedUse {

	public static interface RequestPreparation {
		public void prepareRequest(DataBinder request) throws UcmServiceException;
	}

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

	public ServiceResponse callService(String service, RequestPreparation prep) throws UcmServiceException {
		if (StringUtils.isEmpty(
			service)) { throw new IllegalArgumentException(String.format("Illegal service name [%s]", service)); }
		DataBinder binder = createBinder();
		if (prep != null) {
			prep.prepareRequest(binder);
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

	public UcmFolder getParentFolder(UcmFSObject object) throws UcmFolderNotFoundException, UcmServiceException {
		return this.model.getFolder(this, object.getParentURI());
	}

	public UcmFolder getFolder(String path) throws UcmServiceException, UcmFolderNotFoundException {
		return this.model.getFolder(this, path);
	}

	public UcmFile getFile(String path) throws UcmServiceException, UcmFileNotFoundException {
		return this.model.getFile(this, path);
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

	public int iterateFolderContents(URI uri, ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		return this.model.iterateFolderContents(this, uri, handler);
	}

	public Map<String, UcmFSObject> getFolderContents(UcmFolder folder)
		throws UcmServiceException, UcmFolderNotFoundException {
		return this.model.getFolderContents(this, folder);
	}

	public int iterateFolderContentsRecursive(URI uri, boolean recurseShortcuts, ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		return this.model.iterateFolderContentsRecursive(this, uri, recurseShortcuts, handler);
	}

	public Collection<UcmFSObject> getFolderContentsRecursive(UcmFolder folder, boolean recurseShortcuts)
		throws UcmServiceException, UcmFolderNotFoundException {
		return this.model.getFolderContentsRecursive(this, folder, recurseShortcuts);
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
}