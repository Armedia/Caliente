/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
/**
 *
 */

package com.armedia.caliente.engine.sharepoint.exporter;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

/**
 *
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
}