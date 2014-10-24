/**
 *
 */

package com.armedia.cmf.exporter;

import com.armedia.cmf.storage.CmsObjectStore;
import com.armedia.cmf.storage.CmsStorageException;

/**
 * @author diego
 *
 */
public interface CmsExporter {

	public void runExport(CmsObjectStore objectStore, Object nu) throws CmsExportException, CmsStorageException;

}