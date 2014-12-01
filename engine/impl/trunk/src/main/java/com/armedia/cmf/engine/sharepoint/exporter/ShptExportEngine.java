/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.ShptObject;
import com.armedia.cmf.engine.sharepoint.ShptSessionFactory;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.independentsoft.share.Service;

/**
 * @author diego
 *
 */
public class ShptExportEngine<V> extends
	ExportEngine<Service, ShptSessionWrapper, ShptObject<?>, V, ShptExportContext<V>> {

	@Override
	protected String getObjectId(ShptObject<?> sourceObject) {
		return sourceObject.getId();
	}

	@Override
	protected String calculateLabel(ShptObject<?> sourceObject) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Iterator<ExportTarget> findExportResults(Service session, Map<String, ?> settings) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ShptObject<?> getObject(Service session, StoredObjectType type, String id) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Collection<ShptObject<?>> identifyRequirements(Service session, StoredObject<V> marshalled,
		ShptObject<?> object, ShptExportContext<V> ctx) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Collection<ShptObject<?>> identifyDependents(Service session, StoredObject<V> marshalled,
		ShptObject<?> object, ShptExportContext<V> ctx) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ExportTarget getExportTarget(ShptObject<?> object) throws ExportException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected StoredObject<V> marshal(Service session, ShptObject<?> object) throws ExportException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Handle storeContent(Service session, StoredObject<V> marshalled, ShptObject<?> object,
		ContentStore streamStore) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected V getValue(StoredDataType type, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ObjectStorageTranslator<ShptObject<?>, V> getTranslator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ShptSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new ShptSessionFactory(cfg);
	}

	@Override
	protected ShptExportContextFactory<V> newContextFactory(CfgTools cfg) throws Exception {
		return new ShptExportContextFactory<V>(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		// TODO Auto-generated method stub
		return null;
	}
}