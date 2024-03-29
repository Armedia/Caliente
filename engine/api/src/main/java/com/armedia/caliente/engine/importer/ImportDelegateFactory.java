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
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.armedia.caliente.store.CmfStorageException;
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
		return getFixedPaths(cmfObject, ctx, pathFix) //
			.stream() //
			.findFirst() //
			.orElse(null) //
		;
	}

	public final Collection<String> getFixedPaths(CmfObject<VALUE> cmfObject, CONTEXT ctx) throws ImportException {
		return getFixedPaths(cmfObject, ctx, null);
	}

	public final Collection<String> getFixedPaths(CmfObject<VALUE> cmfObject, CONTEXT ctx,
		UnaryOperator<String> pathFix) throws ImportException {

		// Always work with the head version's paths
		if (!cmfObject.isHistoryCurrent()) {
			try {
				return getFixedPaths(ctx.getHeadObject(cmfObject), ctx, pathFix);
			} catch (CmfStorageException e) {
				throw new ImportException(
					String.format("Failed to locate the HEAD object for %s", cmfObject.getDescription()), e);
			}
		}

		// We're the current version, so we do the actual searching

		List<String> fixedPaths = computeFixedPaths(cmfObject, ctx, pathFix);

		if (!fixedPaths.isEmpty()) {
			// Apply truncations and remove any null values
			fixedPaths.replaceAll(ctx::getTargetPath);
			fixedPaths.removeIf(Objects::isNull);
		}

		if (!fixedPaths.isEmpty() && (pathFix != null)) {
			// Apply any additional fixes ...
			fixedPaths.replaceAll(pathFix);
		}

		// Return whatever's left ...
		return fixedPaths;
	}

	private List<String> computeFixedPaths(CmfObject<VALUE> cmfObject, CONTEXT ctx, UnaryOperator<String> pathFix)
		throws ImportException {
		final CmfAttributeTranslator<VALUE> translator = cmfObject.getTranslator();
		List<String> paths = new ArrayList<>();

		CmfProperty<VALUE> prop = cmfObject.getProperty(IntermediateProperty.FIXED_PATH);
		if ((prop != null) && prop.hasValues()) {
			for (CmfValue v : translator.encodeProperty(prop)) {
				String targetPath = v.asString();
				// Ensure we have only one leading slash ... these paths need to be
				// absolute
				targetPath = targetPath.replaceAll("^(/+)?", "/");

				// Ensure the path is fixed ...
				if (pathFix != null) {
					List<String> l = Tools.splitEscaped('/', targetPath);
					l.replaceAll(pathFix);
					targetPath = Tools.joinEscaped('/', l);
				}

				paths.add(targetPath);
			}

			// If we resolved paths from the FIXED_PATHS attribute, we stick with
			// those
			if (!paths.isEmpty()) { return paths; }
		}

		// If we got here, then FIXED_PATHS was empty, so we try these other possibilities
		prop = cmfObject.getProperty(IntermediateProperty.LATEST_PARENT_TREE_IDS);
		if ((prop == null) || !prop.hasValues()) {
			prop = cmfObject.getProperty(IntermediateProperty.PARENT_TREE_IDS);
			if ((prop == null) || !prop.hasValues()) {
				// No parents ... this means the ROOT must be the parent.
				paths.add(StringUtils.EMPTY);
				return paths;
			}
		}

		// If either of the above are viable, then we resolve the paths, and
		// apply the truncation, filtering out those who fail it...
		for (CmfValue p : translator.encodeProperty(prop)) {
			paths.add(resolveTreeIds(ctx, p.asString(), pathFix));
		}
		return paths;
	}

	protected abstract ImportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ENGINE> newImportDelegate(
		CmfObject<VALUE> storedObject) throws Exception;
}