package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.storage.StoredObject;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;

public class DctmExporter<T extends IDfPersistentObject> extends Exporter<T> {

	public StoredObject<?> loadObject(IDfPersistentObject object) throws ExporterException;

	public void persistRequirements(IDfPersistentObject object, DctmTransferContext ctx,
		DctmDependencyManager dependencyManager) throws DfException, CMSMFException;

	public void persistDependents(IDfPersistentObject object, DctmTransferContext ctx,
		DctmDependencyManager dependencyManager) throws DfException, CMSMFException;

	public SaveResult saveToCMS(DctmTransferContext context) throws DfException, CMSMFException;

}
