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
package com.armedia.caliente.engine.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.TransferDelegate;
import com.armedia.caliente.engine.common.SessionWrapper;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.converter.PathIdHelper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public abstract class ImportDelegate< //
	ECM_OBJECT, //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ImportContext<SESSION, VALUE, ?>, //
	DELEGATE_FACTORY extends ImportDelegateFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ENGINE>, //
	ENGINE extends ImportEngine<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, DELEGATE_FACTORY, ?> //
> extends TransferDelegate<ECM_OBJECT, SESSION, VALUE, CONTEXT, DELEGATE_FACTORY, ENGINE> {

	protected final CmfObject<VALUE> cmfObject;
	protected final ImportStrategy strategy;

	protected ImportDelegate(DELEGATE_FACTORY factory, Class<ECM_OBJECT> objectClass, CmfObject<VALUE> storedObject)
		throws Exception {
		super(factory, objectClass);
		this.cmfObject = storedObject;
		this.strategy = factory.getEngine().getImportStrategy(storedObject.getType());
	}

	protected abstract Collection<ImportOutcome> importObject(CmfAttributeTranslator<VALUE> translator, CONTEXT ctx)
		throws ImportException, CmfStorageException;

	protected final VALUE getAttributeValue(CmfEncodeableName attribute) {
		return this.factory.getAttributeValue(this.cmfObject, attribute);
	}

	protected final VALUE getAttributeValue(String attribute) {
		return this.factory.getAttributeValue(this.cmfObject, attribute);
	}

	protected final List<VALUE> getAttributeValues(CmfEncodeableName attribute) {
		return this.factory.getAttributeValues(this.cmfObject, attribute);
	}

	protected final List<VALUE> getAttributeValues(String attribute) {
		return this.factory.getAttributeValues(this.cmfObject, attribute);
	}

	protected final VALUE getPropertyValue(CmfEncodeableName property) {
		return this.factory.getPropertyValue(this.cmfObject, property);
	}

	protected final VALUE getPropertyValue(String property) {
		return this.factory.getPropertyValue(this.cmfObject, property);
	}

	protected final List<VALUE> getPropertyValues(CmfEncodeableName property) {
		return this.factory.getPropertyValues(this.cmfObject, property);
	}

	protected final List<VALUE> getPropertyValues(String property) {
		return this.factory.getPropertyValues(this.cmfObject, property);
	}

	private final String resolveTreeIds(CONTEXT ctx, String cmsIdPath) throws ImportException {
		List<CmfObjectRef> refs = new ArrayList<>();

		// Convert to a CMFValue to get the string
		for (String id : PathIdHelper.decodePaths(cmsIdPath)) {
			// They're all known to be folders, so...
			refs.add(new CmfObjectRef(CmfObject.Archetype.FOLDER, id));
		}
		Map<CmfObjectRef, String> names = ctx.getObjectNames(refs, true);
		StringBuilder path = new StringBuilder();
		for (CmfObjectRef ref : refs) {
			final String name = names.get(ref);
			if (name == null) {
				// WTF?!?!?
				throw new ImportException(String.format("Failed to resolve the name for %s", ref));
			}

			if (StringUtils.isEmpty(name)) {
				continue;
			}
			if (path.length() > 0) {
				path.append('/');
			}
			path.append(name);
		}
		return path.toString();
	}

	public final String getFixedPath(CONTEXT ctx) throws ImportException {
		CmfProperty<VALUE> prop = this.cmfObject.getProperty(IntermediateProperty.LATEST_PARENT_TREE_IDS);
		if ((prop == null) || !prop.hasValues()) {
			prop = this.cmfObject.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		}

		if ((prop == null) || !prop.hasValues()) {
			throw new ImportException(String.format("Failed to find the required property [%s] in %s",
				IntermediateProperty.PARENT_TREE_IDS.encode(), this.cmfObject.getDescription()));
		}
		CmfAttributeTranslator<VALUE> translator = this.cmfObject.getTranslator();
		CmfValue sourcePath = translator.encodeProperty(prop).getValue();

		String targetPath = null;

		prop = this.cmfObject.getProperty(IntermediateProperty.FIXED_PATH);
		if ((prop != null) && prop.hasValues()) {
			targetPath = translator.encodeProperty(prop).getValue().asString();
			if (!StringUtils.isEmpty(targetPath)) {
				// The FIXED_PATH property is always absolute, but we need to generate
				// a relative path here, so we do just that: remove any leading slashes
				targetPath = targetPath.replaceAll("^/+", "");
			}
		}

		if (targetPath == null) {
			targetPath = resolveTreeIds(ctx, sourcePath.asString());
		}

		return targetPath;
	}
}