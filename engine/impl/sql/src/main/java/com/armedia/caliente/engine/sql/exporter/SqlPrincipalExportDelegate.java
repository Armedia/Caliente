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
package com.armedia.caliente.engine.sql.exporter;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.codec.digest.DigestUtils;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.sql.common.SqlRoot;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public class SqlPrincipalExportDelegate extends SqlExportDelegate<Principal> {

	protected SqlPrincipalExportDelegate(SqlExportDelegateFactory factory, SqlRoot root, Principal object)
		throws Exception {
		super(factory, root, Principal.class, object);
	}

	@Override
	protected Collection<SqlPrincipalExportDelegate> identifyRequirements(CmfObject<CmfValue> marshalled,
		SqlExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected int calculateDependencyTier(SqlRoot root, Principal p) throws Exception {
		return 0;
	}

	@Override
	protected boolean marshal(SqlExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		CmfAttribute<CmfValue> att = null;
		att = new CmfAttribute<>(IntermediateAttribute.NAME, CmfValue.Type.STRING, false);
		att.setValue(CmfValue.of(this.object.getName()));
		object.setAttribute(att);
		if (object.getType() == CmfObject.Archetype.USER) {
			att = new CmfAttribute<>(IntermediateAttribute.LOGIN_NAME, CmfValue.Type.STRING, false);
			att.setValue(CmfValue.of(this.object.getName()));
			object.setAttribute(att);
		}
		return true;
	}

	@Override
	protected Collection<SqlPrincipalExportDelegate> identifyDependents(CmfObject<CmfValue> marshalled,
		SqlExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected CmfObject.Archetype calculateType(SqlRoot root, Principal p) throws Exception {
		if (GroupPrincipal.class.isInstance(p)) { return CmfObject.Archetype.GROUP; }
		if (UserPrincipal.class.isInstance(p)) { return CmfObject.Archetype.USER; }
		throw new ExportException(String.format("Principal object [%s] is of an unknown type or doesn't exist", p));
	}

	@Override
	protected String calculateLabel(SqlRoot root, Principal object) throws Exception {
		return object.getName();
	}

	@Override
	protected String calculateObjectId(SqlRoot root, Principal object) throws Exception {
		return DigestUtils.sha256Hex(String.format("%s:%s", object.getClass().getCanonicalName(), object.getName()));
	}

	@Override
	protected String calculateSearchKey(SqlRoot root, Principal object) throws Exception {
		return object.getName();
	}

	@Override
	protected String calculateName(SqlRoot root, Principal object) throws Exception {
		return object.getName();
	}

	@Override
	protected boolean calculateHistoryCurrent(SqlRoot root, Principal object) throws Exception {
		// Always true
		return true;
	}
}