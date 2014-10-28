/**
 *
 */

package com.armedia.cmf.exporter;

import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;

/**
 * @author diego
 *
 */
public interface CmsExporter {

	public void runExport(ObjectStore objectStore, Object nu) throws ExportException, StorageException;

}