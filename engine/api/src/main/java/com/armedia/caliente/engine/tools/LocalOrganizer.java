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
package com.armedia.caliente.engine.tools;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import javax.activation.MimeType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentOrganizer;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.tools.MimeTools;
import com.armedia.caliente.tools.FilenameFixer;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.ResourceLoader;
import com.armedia.commons.utilities.ResourceLoaderException;
import com.armedia.commons.utilities.Tools;

public class LocalOrganizer extends CmfContentOrganizer {

	private static final String MAPPINGS_FILE = "-mime-extensions.xml";
	private static final List<Function<MimeType, String>> KEY_GENERATORS;
	static {
		List<Function<MimeType, String>> keyGenerators = new LinkedList<>();
		keyGenerators.add(MimeType::toString);
		keyGenerators.add(MimeType::getBaseType);
		keyGenerators.add(MimeType::getPrimaryType);
		KEY_GENERATORS = Tools.freezeList(keyGenerators);
	}

	private static final String REMOVE_LEADING = "remove.leading";

	private static URL getMimeMappings(String name) throws ResourceLoaderException {
		return ResourceLoader.getResourceOrFile(name + LocalOrganizer.MAPPINGS_FILE);
	}

	public static final String NAME = "localfs";

	private final FilenameFixer fixer = new FilenameFixer(true);

	private final Map<String, String> mimeMap;

	private int removeLeading = 0;

	public LocalOrganizer() {
		this(LocalOrganizer.NAME);
	}

	protected LocalOrganizer(String name) {
		super(name);

		final Map<String, String> mimeMap = new HashMap<>();
		URL mappings;
		try {
			mappings = LocalOrganizer.getMimeMappings(getName());
		} catch (ResourceLoaderException e) {
			mappings = null;
		}
		if (mappings != null) {
			try (InputStream in = mappings.openStream()) {
				Properties props = XmlProperties.loadFromXML(in);
				for (String s : props.stringPropertyNames()) {
					final String S = s;
					int at = s.indexOf('@');
					String ext = null;
					if (at > 0) {
						// Split at the @
						s = s.substring(0, at - 1);
						ext = s.substring(at + 1);
					} else if (at == 0) {
						// For those that have no type...?
						s = StringUtils.EMPTY;
						ext = s.substring(1);
					}

					final MimeType mt = MimeTools.resolveMimeType(s);
					final String str = (mt != null ? mt.toString() : S);
					mimeMap.put(mt.toString(), props.getProperty(S));
					if (StringUtils.isNotBlank(ext)) {
						mimeMap.put(str + "@" + ext, props.getProperty(S));
					}
				}
			} catch (Exception e) {
				this.log.error("Failed to load the MIME extension mappings file for {}", getClass().getName(), e);
				mimeMap.clear();
			}
		}
		this.mimeMap = Tools.freezeMap(mimeMap);
	}

	@Override
	public void configure(CfgTools settings) {
		this.removeLeading = Math.max(0, settings.getInteger(LocalOrganizer.REMOVE_LEADING, 0));
		super.configure(settings);
	}

	@Override
	protected <VALUE> Location calculateLocation(CmfAttributeTranslator<VALUE> translator, CmfObject<VALUE> object,
		CmfContentStream info) {
		final List<String> containerSpec = calculateContainerSpec(translator, object, info);

		if (this.removeLeading >= containerSpec.size()) {
			containerSpec.clear();
		} else {
			for (int i = 0; i < this.removeLeading; i++) {
				containerSpec.remove(0);
			}
		}

		final String baseName = calculateBaseName(translator, object, info);
		final String descriptor = calculateDescriptor(translator, object, info);
		final String extension = calculateExtension(translator, object, info);
		final String appendix = calculateAppendix(translator, object, info);
		return newLocation(containerSpec, baseName, extension, descriptor, appendix);
	}

	protected <VALUE> List<String> calculateContainerSpec(CmfAttributeTranslator<VALUE> translator,
		CmfObject<VALUE> object, CmfContentStream info) {
		// Put it in the same path as it was in CMIS, but ensure each path component is
		// of a "universally-valid" format.
		CmfProperty<VALUE> paths = object.getProperty(IntermediateProperty.FIXED_PATH);

		List<String> ret = new ArrayList<>();
		if ((paths != null) && paths.hasValues()) {
			for (String p : FileNameTools.tokenize(paths.getValue().toString(), '/')) {
				ret.add(p);
			}
		}
		return ret;
	}

	protected <VALUE> String getLeafName(CmfAttributeTranslator<VALUE> translator, CmfObject<VALUE> object,
		CmfContentStream info) {
		String objectName = null;
		CmfProperty<?> name = null;

		/*
		if (StringUtils.isEmpty(objectName) && info.hasProperty(IntermediateProperty.FULL_PATH)) {
			objectName = info.getProperty(IntermediateProperty.FULL_PATH);
			if (!StringUtils.isEmpty(objectName)) {
				// Split on the last slash
				objectName = FileNameTools.basename(objectName, '/');
				// Un-fix the name
				objectName = PathTools.makeUnsafe(objectName);
			}
		}
		*/

		if (StringUtils.isEmpty(objectName)) {
			name = object.getProperty(IntermediateProperty.FIXED_NAME);
			if ((name != null) && name.hasValues()) {
				objectName = name.getValue().toString();
			}
		}

		if (StringUtils.isEmpty(objectName)) {
			name = object.getProperty(IntermediateProperty.PRESERVED_NAME);
			if ((name != null) && name.hasValues()) {
				objectName = name.getValue().toString();
			}
		}

		if (StringUtils.isEmpty(objectName)) {
			name = object.getProperty(IntermediateProperty.HEAD_NAME);
			if ((name != null) && name.hasValues()) {
				objectName = name.getValue().toString();
			}
		}

		if (StringUtils.isEmpty(objectName)) {
			name = object.getAttribute(
				translator.getAttributeNameMapper().decodeAttributeName(object.getType(), IntermediateAttribute.NAME));
			if ((name != null) && name.hasValues()) {
				objectName = name.getValue().toString();
			}
		}

		if (StringUtils.isEmpty(objectName)) {
			objectName = object.getName();
		}

		if (StringUtils.isEmpty(objectName)) {
			// Uh-oh ... an empty filename!!! Can't have that!!
			objectName = String.format("[history-%s]", object.getHistoryId());
		}

		return this.fixer.fixName(objectName);
	}

	protected <VALUE> String calculateBaseName(CmfAttributeTranslator<VALUE> translator, CmfObject<VALUE> object,
		CmfContentStream info) {
		return getLeafName(translator, object, info);
	}

	protected <VALUE> String calculateExtension(CmfAttributeTranslator<VALUE> translator, CmfObject<VALUE> object,
		CmfContentStream info) {
		MimeType mimeType = info.getMimeType();
		String ext = info.getExtension();
		if (mimeType == null) { return ext; }

		final boolean hasExt = StringUtils.isNotBlank(ext);
		String alt = null;
		for (Function<MimeType, String> kg : LocalOrganizer.KEY_GENERATORS) {
			final String baseKey = kg.apply(mimeType);

			// Try with the extension appended...
			if (hasExt) {
				alt = this.mimeMap.get(baseKey + "@" + ext);
				if (!StringUtils.isBlank(alt)) { return alt; }
			}

			// Now try without extension...
			alt = this.mimeMap.get(baseKey);
			if (!StringUtils.isBlank(alt)) { return alt; }
		}

		// Nothing was found in the map...so we move on
		return ext;
	}

	protected <VALUE> String calculateDescriptor(CmfAttributeTranslator<VALUE> translator, CmfObject<VALUE> object,
		CmfContentStream info) {
		final String attName = translator.getAttributeNameMapper().decodeAttributeName(object.getType(),
			IntermediateAttribute.VERSION_LABEL);
		final CmfAttribute<?> versionLabelAtt = object.getAttribute(attName);
		String oldFrag = String.format("%s.%08x", info.getRenditionIdentifier(), info.getRenditionPage());
		if ((versionLabelAtt != null) && versionLabelAtt.hasValues()) {
			final String versionLabel = versionLabelAtt.getValue().toString();
			if (StringUtils.isBlank(versionLabel)) { return oldFrag; }
			if (StringUtils.isBlank(oldFrag)) { return versionLabel; }
			return String.format("%s@%s", oldFrag, versionLabel);
		}
		return oldFrag;
	}

	protected <VALUE> String calculateAppendix(CmfAttributeTranslator<VALUE> translator, CmfObject<VALUE> object,
		CmfContentStream info) {
		return "";
	}
}