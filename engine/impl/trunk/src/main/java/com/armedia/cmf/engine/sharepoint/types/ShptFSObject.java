/**
 *
 */

package com.armedia.cmf.engine.sharepoint.types;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.sharepoint.ShptAttributes;
import com.armedia.cmf.engine.sharepoint.ShptProperties;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportEngine;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public abstract class ShptFSObject<T> extends ShptObject<T> {

	private final String url;

	protected ShptFSObject(ShptExportEngine engine, Class<T> objectClass, T object) throws Exception {
		super(engine, objectClass, object);
		this.url = calculateServerRelativeUrl(object);
	}

	@Override
	protected String calculateObjectId(T object) {
		String searchKey = calculateServerRelativeUrl(object);
		return String.format("%08X", Tools.hashTool(searchKey, null, searchKey));
	}

	@Override
	protected String calculateSearchKey(T object) {
		return calculateServerRelativeUrl(object);
	}

	protected abstract String calculateServerRelativeUrl(T object);

	public final String getServerRelativeUrl() {
		return this.url;
	}

	public abstract Date getCreatedTime();

	public abstract Date getLastModifiedTime();

	@Override
	protected void marshal(ShptExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		// Name
		String name = getName();
		final boolean root = StringUtils.isEmpty(name);
		if (root) {
			name = getServerRelativeUrl();
			name = FileNameTools.removeEdgeSeparators(name, '/');
			name = name.replaceFirst("/", "_");
		}
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OBJECT_NAME.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(name))));

		Date d = getCreatedTime();
		if (d != null) {
			object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.CREATE_DATE.name, StoredDataType.DATETIME,
				false, Collections.singleton(new StoredValue(d))));
		}

		d = getLastModifiedTime();
		if (d != null) {
			object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.MODIFICATION_DATE.name,
				StoredDataType.DATETIME, false, Collections.singleton(new StoredValue(d))));
		}

		// Target Paths
		if (!root) {
			// TODO: is this safe? What if we have a "3-level root"? i.e. /sites/blabla/root
			String path = getServerRelativeUrl();
			path = FileNameTools.dirname(path, '/');
			path = FileNameTools.removeEdgeSeparators(path, '/').replaceFirst("/", "_");
			path = String.format("/%s", path);
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Setting target path [%s] from source path [%s] for %s [ID=%s/L=%s]",
					path, getServerRelativeUrl(), getType(), getObjectId(), getLabel()));
			}
			object.setProperty(new StoredProperty<StoredValue>(ShptProperties.TARGET_PATHS.name, StoredDataType.STRING,
				true, Collections.singleton(new StoredValue(path))));
		}
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(ShptSession session, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(session, marshaled, ctx);
		if (!StringUtils.isEmpty(getName())) {
			String parentPath = getServerRelativeUrl();
			parentPath = FileNameTools.dirname(parentPath, '/');
			ShptFolder parent = new ShptFolder(getEngine(), session.getFolder(parentPath));
			marshaled.setProperty(new StoredProperty<StoredValue>(ShptProperties.TARGET_PARENTS.name,
				StoredDataType.ID, true,
				Collections.singleton(new StoredValue(StoredDataType.ID, parent.getObjectId()))));
			ret.add(parent);
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format(
					"Adding parent dependency to [%s] from source path [%s] for %s [ID=%s/L=%s]", parentPath,
					getServerRelativeUrl(), getType(), getObjectId(), getLabel()));
			}
		}
		return ret;
	}
}