/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;

/**
 * @author diego
 *
 */
public abstract class ShptObject<T> extends ShptExportDelegate<T> {

	public static final String TARGET_NAME = "shpt";

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * @param factory
	 * @param objectClass
	 * @param object
	 * @throws Exception
	 */
	protected ShptObject(ShptExportDelegateFactory factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass, object);
	}

	@Override
	protected String calculateSearchKey(T object) throws Exception {
		return calculateObjectId(object);
	}

	@Override
	protected final Collection<? extends ShptExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return findRequirements(ctx.getSession(), marshaled, ctx);
	}

	protected Collection<ShptObject<?>> findRequirements(ShptSession session, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return new ArrayList<ShptObject<?>>();
	}

	@Override
	protected final Collection<? extends ShptExportDelegate<?>> identifyAntecedents(CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return findAntecedents(ctx.getSession(), marshaled, ctx);
	}

	protected Collection<ShptObject<?>> findAntecedents(ShptSession session, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return new ArrayList<ShptObject<?>>();
	}

	@Override
	protected final Collection<? extends ShptExportDelegate<?>> identifySuccessors(CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return findSuccessors(ctx.getSession(), marshaled, ctx);
	}

	protected Collection<ShptObject<?>> findSuccessors(ShptSession session, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return new ArrayList<ShptObject<?>>();
	}

	@Override
	protected final Collection<? extends ShptExportDelegate<?>> identifyDependents(CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return findDependents(ctx.getSession(), marshaled, ctx);
	}

	protected Collection<ShptObject<?>> findDependents(ShptSession service, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return new ArrayList<ShptObject<?>>();
	}

	@Override
	protected List<CmfContentInfo> storeContent(ShptExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}