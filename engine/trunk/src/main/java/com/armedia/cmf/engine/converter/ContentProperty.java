/**
 *
 */

package com.armedia.cmf.engine.converter;

import java.util.Map;

import org.apache.chemistry.opencmis.commons.PropertyIds;

import com.armedia.cmf.engine.converter.MappingManager.Mappable;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfEncodeableName;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public enum ContentProperty implements Mappable, CmfEncodeableName {
	//
	MIME_TYPE(PropertyIds.CONTENT_STREAM_MIME_TYPE, CmfDataType.STRING),
	SIZE(PropertyIds.CONTENT_STREAM_LENGTH, CmfDataType.INTEGER),
	FILE_NAME(PropertyIds.CONTENT_STREAM_FILE_NAME, CmfDataType.STRING),
	//
	;

	private final String name;
	public final CmfDataType type;
	public final boolean repeating;

	private ContentProperty(CmfDataType type) {
		this(null, type, false);
	}

	private ContentProperty(String propertyId, CmfDataType type) {
		this(propertyId, type, false);
	}

	private ContentProperty(CmfDataType type, boolean repeating) {
		this(null, type, repeating);
	}

	private ContentProperty(String propertyId, CmfDataType type, boolean repeating) {
		this.name = MappingManager.generateMapping(propertyId, name());
		this.type = type;
		this.repeating = repeating;
	}

	@Override
	public final String getMapping() {
		return this.name;
	}

	@Override
	public final String encode() {
		return this.name;
	}

	private static volatile Map<String, ContentProperty> MAPPINGS = null;

	private static void initMappings() {
		if (ContentProperty.MAPPINGS == null) {
			synchronized (ContentProperty.class) {
				if (ContentProperty.MAPPINGS == null) {
					ContentProperty.MAPPINGS = Tools.freezeMap(MappingManager.createMappings(ContentProperty.class,
						ContentProperty.values()));
				}
			}
		}
	}

	public static ContentProperty decode(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a name to decode"); }
		ContentProperty.initMappings();
		ContentProperty ret = ContentProperty.MAPPINGS.get(name);
		if (ret == null) { throw new IllegalArgumentException(String.format(
			"Failed to decode [%s] into a valid intermediate property", name)); }
		return ret;
	}
}