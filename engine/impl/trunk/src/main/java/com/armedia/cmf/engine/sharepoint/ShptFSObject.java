/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;

/**
 * @author diego
 *
 */
public abstract class ShptFSObject<T> extends ShptObject<T> {

	private final String id;

	protected ShptFSObject(Service service, T wrapped, StoredObjectType type) {
		super(service, wrapped, type);
		this.id = String.format("%08X", Tools.hashTool(this, null, type, getSearchKey()));
	}

	@Override
	public final String getId() {
		return this.id;
	}

	public abstract String getServerRelativeUrl();

	public abstract Date getCreatedTime();

	public abstract Date getLastModifiedTime();

	@Override
	protected void marshal(StoredObject<StoredValue> object) throws ExportException {
		// Name
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OBJECT_NAME.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(getName()))));

		Date d = getCreatedTime();
		if (d != null) {
			object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.CREATE_DATE.name, StoredDataType.TIME,
				false, Collections.singleton(new StoredValue(d))));
		}

		d = getLastModifiedTime();
		if (d != null) {
			object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.MODIFICATION_DATE.name,
				StoredDataType.TIME, false, Collections.singleton(new StoredValue(d))));
		}

		// Target Paths
		final String path = FileNameTools.dirname(getServerRelativeUrl());
		object.setProperty(new StoredProperty<StoredValue>(ShptProperties.TARGET_PATHS.name, StoredDataType.STRING,
			true, Collections.singleton(new StoredValue(path))));
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(Service session, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(session, marshaled, ctx);
		String parentPath = FileNameTools.dirname(getServerRelativeUrl());
		try {
			ret.add(new ShptFolder(session, session.getFolder(parentPath)));
		} catch (ServiceException e) {
			// TODO: We need to be clearer on the errors being returned... but the API ties our
			// hands and thus we will eventually have to replace it with something better.
			// No parent...
		}
		return ret;
	}
}