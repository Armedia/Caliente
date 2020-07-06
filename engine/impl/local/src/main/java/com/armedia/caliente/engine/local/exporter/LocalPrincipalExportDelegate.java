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
package com.armedia.caliente.engine.local.exporter;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.codec.digest.DigestUtils;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public class LocalPrincipalExportDelegate extends LocalExportDelegate<Principal> {

	protected LocalPrincipalExportDelegate(LocalExportDelegateFactory factory, LocalRoot root, Principal object)
		throws Exception {
		super(factory, root, Principal.class, object);
	}

	@Override
	protected Collection<LocalPrincipalExportDelegate> identifyRequirements(CmfObject<CmfValue> marshalled,
		LocalExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected int calculateDependencyTier(LocalRoot root, Principal p) throws Exception {
		return 0;
	}

	@Override
	protected boolean marshal(LocalExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
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
	protected Collection<LocalPrincipalExportDelegate> identifyDependents(CmfObject<CmfValue> marshalled,
		LocalExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected CmfObject.Archetype calculateType(LocalRoot root, Principal p) throws Exception {
		if (GroupPrincipal.class.isInstance(p)) { return CmfObject.Archetype.GROUP; }
		if (UserPrincipal.class.isInstance(p)) { return CmfObject.Archetype.USER; }
		throw new ExportException(String.format("Principal object [%s] is of an unknown type or doesn't exist", p));
	}

	@Override
	protected String calculateLabel(LocalRoot root, Principal object) throws Exception {
		return object.getName();
	}

	@Override
	protected String calculateObjectId(LocalRoot root, Principal object) throws Exception {
		return DigestUtils.sha256Hex(String.format("%s:%s", object.getClass().getCanonicalName(), object.getName()))
			.toUpperCase();
	}

	@Override
	protected String calculateSearchKey(LocalRoot root, Principal object) throws Exception {
		return object.getName();
	}

	@Override
	protected String calculateName(LocalRoot root, Principal object) throws Exception {
		return object.getName();
	}

	@Override
	protected boolean calculateHistoryCurrent(LocalRoot root, Principal object) throws Exception {
		// Always true
		return true;
	}
}