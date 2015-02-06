/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;

import javax.activation.MimeType;

import org.apache.commons.io.IOUtils;

import com.armedia.cmf.engine.sharepoint.common.MimeTools;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.BinaryMemoryBuffer;
import com.independentsoft.share.File;
import com.independentsoft.share.Service;

/**
 * @author diego
 *
 */
public class ShptContent extends ShptFSObject<File> {

	public ShptContent(Service service, File wrapped) {
		super(service, wrapped, StoredObjectType.CONTENT);
	}

	@Override
	public String getSearchKey() {
		return this.wrapped.getServerRelativeUrl();
	}

	@Override
	public String getName() {
		return this.wrapped.getName();
	}

	@Override
	public String getServerRelativeUrl() {
		return this.wrapped.getServerRelativeUrl();
	}

	@Override
	public Date getCreatedTime() {
		return this.wrapped.getCreatedTime();
	}

	@Override
	public Date getLastModifiedTime() {
		return this.wrapped.getLastModifiedTime();
	}

	@Override
	public String getBatchId() {
		return this.wrapped.getUniqueId();
	}

	@Override
	public String getLabel() {
		return this.wrapped.getName();
	}

	@Override
	public Handle storeContent(Service session, StoredObject<StoredValue> marshaled, ContentStore streamStore)
		throws Exception {
		// TODO: We NEED to use something other than the object ID here...
		Handle h = streamStore.getHandle(marshaled, "");
		InputStream in = session.getFileStream(this.wrapped.getServerRelativeUrl());
		// TODO: sadly, this is not memory efficient for larger files...
		BinaryMemoryBuffer buf = new BinaryMemoryBuffer(10240);
		OutputStream out = h.openOutput();
		try {
			IOUtils.copy(in, buf);
			buf.close();
			IOUtils.copy(buf.getInputStream(), out);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
		// Now, try to identify the content type...
		in = buf.getInputStream();
		MimeType type = null;
		try {
			type = MimeTools.determineMimeType(in);
		} catch (Exception e) {
			type = MimeTools.DEFAULT_MIME_TYPE;
		}
		marshaled.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.CONTENT_TYPE.name,
			StoredDataType.STRING, false, Collections.singleton(new StoredValue(type.getBaseType()))));
		return h;
	}
}