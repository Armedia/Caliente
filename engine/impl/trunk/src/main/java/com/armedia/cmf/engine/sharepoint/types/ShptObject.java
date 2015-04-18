/**
 *
 */

package com.armedia.cmf.engine.sharepoint.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportDelegate;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportEngine;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;

/**
 * @author diego
 *
 */
public abstract class ShptObject<T> extends ShptExportDelegate<T> {

	public static final String TARGET_NAME = "shpt";

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * @param engine
	 * @param objectClass
	 * @param object
	 * @throws Exception
	 */
	protected ShptObject(ShptExportEngine engine, Class<T> objectClass, T object) throws Exception {
		super(engine, objectClass, object);
	}

	public abstract String getName();

	@Override
	protected String calculateSearchKey(T object) throws Exception {
		return calculateObjectId(object);
	}

	@Override
	protected Collection<? extends ShptExportDelegate<?>> identifyRequirements(StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return findRequirements(ctx.getSession(), marshaled, ctx);
	}

	protected Collection<ShptObject<?>> findRequirements(ShptSession session, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return new ArrayList<ShptObject<?>>();
	}

	@Override
	protected Collection<? extends ShptExportDelegate<?>> identifyDependents(StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return findDependents(ctx.getSession(), marshaled, ctx);
	}

	protected Collection<ShptObject<?>> findDependents(ShptSession service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return new ArrayList<ShptObject<?>>();
	}

	@Override
	protected List<ContentInfo> storeContent(ShptSession session, StoredObject<StoredValue> marshalled,
		ExportTarget referrent, ContentStore streamStore) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}