/**
 *
 */

package com.armedia.caliente.engine.sharepoint.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

/**
 * @author diego
 *
 */
public abstract class ShptObject<T> extends ShptExportDelegate<T> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * @param factory
	 * @param objectClass
	 * @param object
	 * @throws Exception
	 */
	protected ShptObject(ShptExportDelegateFactory factory, ShptSession session, Class<T> objectClass, T object)
		throws Exception {
		super(factory, session, objectClass, object);
	}

	@Override
	protected String calculateSearchKey(ShptSession session, T object) throws Exception {
		return calculateObjectId(session, object);
	}

	@Override
	public int calculateDependencyTier(ShptSession session, T object) {
		return 0;
	}

	@Override
	protected final Collection<? extends ShptExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return findRequirements(ctx.getSession(), marshaled, ctx);
	}

	protected Collection<ShptObject<?>> findRequirements(ShptSession session, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected final Collection<? extends ShptExportDelegate<?>> identifyAntecedents(CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return findAntecedents(ctx.getSession(), marshaled, ctx);
	}

	protected Collection<ShptObject<?>> findAntecedents(ShptSession session, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected final Collection<? extends ShptExportDelegate<?>> identifySuccessors(CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return findSuccessors(ctx.getSession(), marshaled, ctx);
	}

	protected Collection<ShptObject<?>> findSuccessors(ShptSession session, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected final Collection<? extends ShptExportDelegate<?>> identifyDependents(CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return findDependents(ctx.getSession(), marshaled, ctx);
	}

	protected Collection<ShptObject<?>> findDependents(ShptSession service, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected List<CmfContentStream> storeContent(ShptExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore,
		boolean includeRenditions) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}