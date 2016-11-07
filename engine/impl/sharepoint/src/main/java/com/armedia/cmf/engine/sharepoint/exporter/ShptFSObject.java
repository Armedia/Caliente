/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.sharepoint.ShptAttributes;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public abstract class ShptFSObject<T> extends ShptObject<T> {

	private final String url;

	protected ShptFSObject(ShptExportDelegateFactory factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass, object);
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
	protected boolean marshal(ShptExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		// Name
		String name = getName();
		final boolean root = StringUtils.isEmpty(name);
		if (root) {
			name = getServerRelativeUrl();
			name = FileNameTools.removeEdgeSeparators(name, '/');
			name = name.replaceFirst("/", "_");
		}
		object.setAttribute(new CmfAttribute<CmfValue>(ShptAttributes.OBJECT_NAME.name, CmfDataType.STRING, false,
			Collections.singleton(new CmfValue(name))));

		Date d = getCreatedTime();
		if (d != null) {
			object.setAttribute(new CmfAttribute<CmfValue>(ShptAttributes.CREATE_DATE.name, CmfDataType.DATETIME, false,
				Collections.singleton(new CmfValue(d))));
		}

		d = getLastModifiedTime();
		if (d != null) {
			object.setAttribute(new CmfAttribute<CmfValue>(ShptAttributes.MODIFICATION_DATE.name, CmfDataType.DATETIME,
				false, Collections.singleton(new CmfValue(d))));
		}

		// Target Paths
		if (!root) {
			// TODO: is this safe? What if we have a "3-level root"? i.e. /sites/blabla/root
			String path = getServerRelativeUrl();
			path = FileNameTools.dirname(path, '/');
			path = FileNameTools.removeEdgeSeparators(path, '/').replaceFirst("/", "_");
			path = String.format("/%s", path);
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Setting target path [%s] from source path [%s] for %s [ID=%s/L=%s]", path,
					getServerRelativeUrl(), getType(), getObjectId(), getLabel()));
			}
			object.setProperty(new CmfProperty<CmfValue>(IntermediateProperty.PATH, CmfDataType.STRING, true,
				Collections.singleton(new CmfValue(path))));
		}
		return true;
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(ShptSession session, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(session, marshaled, ctx);
		if (!StringUtils.isEmpty(getName())) {
			String parentPath = getServerRelativeUrl();
			parentPath = FileNameTools.dirname(parentPath, '/');
			ShptFolder parent = new ShptFolder(this.factory, session.getFolder(parentPath));
			marshaled.setProperty(new CmfProperty<CmfValue>(IntermediateProperty.PARENT_ID, CmfDataType.ID, true,
				Collections.singleton(new CmfValue(CmfDataType.ID, parent.getObjectId()))));
			ret.add(parent);
			if (this.log.isDebugEnabled()) {
				this.log
					.debug(String.format("Adding parent dependency to [%s] from source path [%s] for %s [ID=%s/L=%s]",
						parentPath, getServerRelativeUrl(), getType(), getObjectId(), getLabel()));
			}
		}
		return ret;
	}
}