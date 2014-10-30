package com.delta.cmsmf.cms;

import com.delta.cmsmf.cms.DctmPersistentObject.SaveResult;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;

public interface DctmInterface<T extends IDfPersistentObject> {

	/**
	 * <p>
	 * Loads the object's attributes and properties from the given CMS object, and returns
	 * {@code true} if the load was successful or {@code false} if the object should not be
	 * processed at all.
	 * </p>
	 *
	 * @param object
	 * @return {@code true} if the load was successful or {@code false} if the object should not be
	 *         processed at all
	 * @throws DfException
	 * @throws CMSMFException
	 */
	public boolean loadFromCMS(IDfPersistentObject object) throws DfException, CMSMFException;

	public void persistRequirements(IDfPersistentObject object, DctmTransferContext ctx,
		DctmDependencyManager dependencyManager) throws DfException, CMSMFException;

	public void persistDependents(IDfPersistentObject object, DctmTransferContext ctx,
		DctmDependencyManager dependencyManager) throws DfException, CMSMFException;

	public SaveResult saveToCMS(DctmTransferContext context) throws DfException, CMSMFException;

}