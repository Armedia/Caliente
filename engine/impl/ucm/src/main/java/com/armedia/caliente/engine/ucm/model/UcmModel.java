package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;

import com.armedia.caliente.engine.ucm.UcmSessionFactory;

import oracle.stellent.ridc.IdcClientException;

@SuppressWarnings("unused")
public class UcmModel {
	private static final String NULLID = "idcnull";

	// Object's GUID pointing to its object
	private static CacheAccess<String, UcmFSObject> OBJECTS_BY_GUID = JCS.getInstance("Objects");

	// Object's GUID pointing to its parent's GUID
	private static CacheAccess<String, String> PARENTAGE_BY_GUID = JCS.getInstance("Parentage");

	// File's content ID (which doesn't change per versions) pointing to their UcmFileHistory object
	private static CacheAccess<String, UcmFileHistory> HISTORY = JCS.getInstance("History");

	private final UcmSessionFactory sessionFactory;

	public UcmModel(UcmSessionFactory sessionFactory) {
		Objects.requireNonNull(sessionFactory, "Must provide a non-null session factory");
		this.sessionFactory = sessionFactory;
	}

	public UcmFile getFile(String path) throws IdcClientException {
		return null;
	}

	public UcmFile getFile(String path, int revision) throws IdcClientException {
		return null;
	}

	public UcmFolder getFolder(String path) throws IdcClientException {
		return null;
	}

	public UcmFileHistory getFileHistory(String path) throws IdcClientException {
		return null;
	}

	public UcmFileHistory getFileHistory(UcmFile file) throws IdcClientException {
		return null;
	}

	List<UcmFSObject> getFolderContents(UcmFolder folder) throws IdcClientException {
		return null;
	}

	UcmFolder getFolderByGUID(String guid) throws IdcClientException {
		return null;
	}

	InputStream getInputStream(UcmFile file, String rendition) throws IdcClientException {
		return null;
	}

	void refresh(UcmFile file) throws IdcClientException {

	}

	void refresh(UcmFolder folder) throws IdcClientException {

	}

	void refresh(UcmFileHistory history) throws IdcClientException {

	}
}