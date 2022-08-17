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
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.TransferDelegateFactory;
import com.armedia.caliente.engine.common.SessionWrapper;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.converter.PathIdHelper;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public abstract class ImportDelegateFactory< //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ImportContext<SESSION, VALUE, ?>, //
	ENGINE extends ImportEngine<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?, ?>//
> extends TransferDelegateFactory<SESSION, VALUE, CONTEXT, ENGINE> {

	private static final UnaryOperator<String> IDENTITY = UnaryOperator.identity();

	private final Map<CmfValue.Type, VALUE> nullValues;

	protected ImportDelegateFactory(ENGINE engine, CfgTools configuration) {
		super(engine, configuration);

		final CmfAttributeTranslator<VALUE> translator = getTranslator();
		final Map<CmfValue.Type, VALUE> nullValues = new EnumMap<>(CmfValue.Type.class);
		for (CmfValue.Type t : CmfValue.Type.values()) {
			CmfValueCodec<VALUE> codec = translator.getCodec(t);
			VALUE v = codec.decode(t.getNull());
			nullValues.put(t, v);
		}
		this.nullValues = Tools.freezeMap(nullValues);
	}

	public final int getRetryCount() {
		return this.engine.getRetryCount();
	}

	public final boolean isRequireAllParents() {
		return this.engine.isRequireAllParents();
	}

	protected final VALUE getAttributeValue(CmfObject<VALUE> cmfObject, CmfEncodeableName attribute) {
		return getAttributeValue(cmfObject, attribute.encode());
	}

	protected final VALUE getAttributeValue(CmfObject<VALUE> cmfObject, String attribute) {
		CmfAttribute<VALUE> att = cmfObject.getAttribute(attribute);
		if (att == null) { return this.nullValues.get(CmfValue.Type.OTHER); }
		if (att.hasValues()) { return att.getValue(); }
		return this.nullValues.get(att.getType());
	}

	protected final List<VALUE> getAttributeValues(CmfObject<VALUE> cmfObject, CmfEncodeableName attribute) {
		return getAttributeValues(cmfObject, attribute.encode());
	}

	protected final List<VALUE> getAttributeValues(CmfObject<VALUE> cmfObject, String attribute) {
		CmfAttribute<VALUE> att = cmfObject.getAttribute(attribute);
		if (att == null) { return Collections.emptyList(); }
		return att.getValues();
	}

	protected final VALUE getPropertyValue(CmfObject<VALUE> cmfObject, CmfEncodeableName attribute) {
		return getPropertyValue(cmfObject, attribute.encode());
	}

	protected final VALUE getPropertyValue(CmfObject<VALUE> cmfObject, String attribute) {
		CmfProperty<VALUE> prop = cmfObject.getProperty(attribute);
		if (prop == null) { return this.nullValues.get(CmfValue.Type.OTHER); }
		if (prop.hasValues()) { return prop.getValue(); }
		return this.nullValues.get(prop.getType());
	}

	protected final List<VALUE> getPropertyValues(CmfObject<VALUE> cmfObject, CmfEncodeableName attribute) {
		return getPropertyValues(cmfObject, attribute.encode());
	}

	protected final List<VALUE> getPropertyValues(CmfObject<VALUE> cmfObject, String attribute) {
		CmfProperty<VALUE> att = cmfObject.getProperty(attribute);
		if (att == null) { return Collections.emptyList(); }
		return att.getValues();
	}

	private final String resolveTreeIds(CONTEXT ctx, String cmsIdPath, UnaryOperator<String> pathFix)
		throws ImportException {
		List<CmfObjectRef> refs = new ArrayList<>();

		// Convert to a CMFValue to get the string
		for (String id : PathIdHelper.decodePaths(cmsIdPath)) {
			// They're all known to be folders, so...
			refs.add(new CmfObjectRef(CmfObject.Archetype.FOLDER, id));
		}
		Map<CmfObjectRef, String> names = ctx.getObjectNames(refs, true);
		StringBuilder path = new StringBuilder();
		if (pathFix == null) {
			pathFix = ImportDelegateFactory.IDENTITY;
		}
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

			path.append(pathFix.apply(name));
		}
		return path.toString();
	}

	public final String getFixedPath(CmfObject<VALUE> cmfObject, CONTEXT ctx) throws ImportException {
		return getFixedPath(cmfObject, ctx, null);
	}

	public final String getFixedPath(CmfObject<VALUE> cmfObject, CONTEXT ctx, UnaryOperator<String> pathFix)
		throws ImportException {
		CmfProperty<VALUE> prop = cmfObject.getProperty(IntermediateProperty.LATEST_PARENT_TREE_IDS);
		if ((prop == null) || !prop.hasValues()) {
			prop = cmfObject.getProperty(IntermediateProperty.PARENT_TREE_IDS);
			if ((prop == null) || !prop.hasValues()) { return StringUtils.EMPTY; }
		}

		CmfAttributeTranslator<VALUE> translator = cmfObject.getTranslator();
		CmfValue sourcePath = translator.encodeProperty(prop).getValue();

		String targetPath = null;

		prop = cmfObject.getProperty(IntermediateProperty.FIXED_PATH);
		if ((prop != null) && prop.hasValues()) {
			targetPath = translator.encodeProperty(prop).getValue().asString();
			if (!StringUtils.isEmpty(targetPath)) {
				// The FIXED_PATH property is always absolute, but we need to generate
				// a relative path here, so we do just that: remove any leading slashes
				targetPath = targetPath.replaceAll("^/+", "");
			}
		}

		if (targetPath == null) {
			targetPath = resolveTreeIds(ctx, sourcePath.asString(), pathFix);
		} else if (pathFix != null) {
			// Split, and fix each path
			List<String> l = Tools.splitEscaped('/', targetPath);
			l.replaceAll(pathFix);
			targetPath = Tools.joinEscaped('/', l);
		}

		// Fixed paths are paths whose individual components have been "fixed" (i.e.
		// character-corrected, length-corrected, or somesuch), so we still may have
		// to truncate them accordingly
		return ctx.getTargetPath(targetPath);
	}

	protected abstract ImportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ENGINE> newImportDelegate(
		CmfObject<VALUE> storedObject) throws Exception;
}