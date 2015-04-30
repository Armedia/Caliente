package com.armedia.cmf.engine.sharepoint.types;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.MimeType;

import org.apache.commons.io.IOUtils;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.IncompleteDataException;
import com.armedia.cmf.engine.sharepoint.ShptAttributes;
import com.armedia.cmf.engine.sharepoint.ShptProperties;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionException;
import com.armedia.cmf.engine.sharepoint.ShptVersionNumber;
import com.armedia.cmf.engine.sharepoint.common.MimeTools;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportEngine;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.BinaryMemoryBuffer;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.CheckOutType;
import com.independentsoft.share.CustomizedPageStatus;
import com.independentsoft.share.File;
import com.independentsoft.share.FileLevel;
import com.independentsoft.share.FileVersion;

public class ShptFile extends ShptFSObject<ShptVersion> {

	private static final Pattern SEARCH_KEY_PARSER = Pattern.compile("^([^#]*)#(\\d+\\.\\d+)?$");

	private final String antecedentId;
	private final FileVersion version;
	private final ShptVersionNumber versionNumber;

	private List<ShptFile> predecessors = Collections.emptyList();
	private List<ShptFile> successors = Collections.emptyList();

	public ShptFile(ShptExportEngine engine, File file) throws Exception {
		this(engine, new ShptVersion(file), null);
	}

	protected ShptFile(ShptExportEngine engine, File file, FileVersion version) throws Exception {
		this(engine, new ShptVersion(file, version), null);
	}

	protected ShptFile(ShptExportEngine engine, ShptVersion object) throws Exception {
		this(engine, object, null);
	}

	protected ShptFile(ShptExportEngine engine, ShptVersion object, ShptFile antecedent) throws Exception {
		super(engine, ShptVersion.class, object);
		this.version = object.getVersion();
		this.versionNumber = object.getVersionNumber();
		this.antecedentId = (antecedent != null ? antecedent.getObjectId() : null);
	}

	@Override
	public String calculateObjectId(ShptVersion object) {
		return String.format("%s-%s", super.calculateObjectId(object), object.getVersionNumber().toString());
	}

	public ShptVersionNumber getVersionNumber() {
		return this.object.getVersionNumber();
	}

	@Override
	public String calculateSearchKey(ShptVersion object) {
		return String.format(String.format("%s#%s", object.getFile().getServerRelativeUrl(), object.getVersionNumber()
			.toString()));
	}

	@Override
	public String getName() {
		return this.object.getName();
	}

	@Override
	public String calculateServerRelativeUrl(ShptVersion file) {
		return file.getFile().getServerRelativeUrl();
	}

	@Override
	public Date getCreatedTime() {
		return this.object.getCreatedTime();
	}

	@Override
	public Date getLastModifiedTime() {
		return this.object.getLastModifiedTime();
	}

	public String getCheckInComment() {
		return this.object.getCheckInComment();
	}

	public CheckOutType getCheckOutType() {
		return this.object.getCheckOutType();
	}

	public String getContentTag() {
		return this.object.getContentTag();
	}

	public CustomizedPageStatus getCustomizedPageStatus() {
		return this.object.getCustomizedPageStatus();
	}

	public String getETag() {
		return this.object.getETag();
	}

	public boolean exists() {
		return this.object.exists();
	}

	public long getLength() {
		return this.object.getLength();
	}

	public String getLinkingUrl() {
		return this.object.getLinkingUrl();
	}

	public FileLevel getLevel() {
		return this.object.getLevel();
	}

	public int getMajorVersion() {
		return this.object.getMajorVersion();
	}

	public int getMinorVersion() {
		return this.object.getMinorVersion();
	}

	public String getTitle() {
		return this.object.getTitle();
	}

	public int getUIVersion() {
		return this.object.getUIVersion();
	}

	public String getUIVersionLabel() {
		return this.object.getUIVersionLabel();
	}

	@Override
	public String calculateBatchId(ShptVersion file) {
		// This only takes into account the path, so it'll be shared by all versions of the file
		return super.calculateObjectId(file);
	}

	@Override
	public String calculateLabel(ShptVersion file) {
		return String.format("%s#%s", file.getName(), file.getVersionNumber().toString());
	}

	@Override
	protected void marshal(ShptExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		super.marshal(ctx, object);
		final ShptSession service = ctx.getSession();
		List<StoredValue> versionNames = new ArrayList<StoredValue>();

		if (this.version != null) {
			Date d = this.version.getCreatedTime();
			if (d != null) {
				Collection<StoredValue> c = Collections.singleton(new StoredValue(d));
				object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.CREATE_DATE.name,
					StoredDataType.DATETIME, false, c));
				object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.MODIFICATION_DATE.name,
					StoredDataType.DATETIME, false, c));
			}
		}

		versionNames.add(new StoredValue(this.versionNumber.toString()));

		StoredProperty<StoredValue> current = new StoredProperty<StoredValue>(ShptProperties.CURRENT_VERSION.name,
			StoredDataType.BOOLEAN, false);
		current.setValue(new StoredValue((this.version == null) || this.version.isCurrentVersion()));
		object.setProperty(current);

		if (this.version != null) {
			this.predecessors = Collections.emptyList();
			this.successors = Collections.emptyList();
			if (this.antecedentId != null) {
				object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.VERSION_PRIOR.name,
					StoredDataType.ID, false, Collections.singleton(new StoredValue(this.antecedentId))));
			}
		} else {
			String antecedentId = this.antecedentId;
			try {
				List<FileVersion> l = service.getFileVersions(this.object.getServerRelativeUrl());
				Map<ShptVersionNumber, FileVersion> versions = new TreeMap<ShptVersionNumber, FileVersion>();
				for (FileVersion v : l) {
					final ShptVersionNumber n = new ShptVersionNumber(v.getLabel());
					versions.put(n, v);
				}

				List<ShptFile> pred = new ArrayList<ShptFile>(versions.size());
				List<ShptFile> succ = new ArrayList<ShptFile>(versions.size());
				ShptFile antecedent = null;
				List<ShptFile> tgt = pred;
				for (Map.Entry<ShptVersionNumber, FileVersion> e : versions.entrySet()) {
					final ShptVersionNumber vn = e.getKey();
					final FileVersion v = e.getValue();
					final int c = this.versionNumber.compareTo(vn);
					if (c > 0) {
						tgt = pred;
					} else if (c < 0) {
						tgt = succ;
						// If there is no antecedent, or the antecedent is prior to this version,
						// then this should be the new antecedent, and our antecedent is the
						// antecedent.
						if ((antecedent == null) || (this.versionNumber.compareTo(antecedent.getVersionNumber()) < 0)) {
							antecedentId = antecedent.getObjectId();
							antecedent = this;
						}
					} else if (c == 0) {
						tgt = succ;
						antecedent = this;
						continue;
					}
					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("FILE [%s] - version %s is anteceded by %s",
							this.object.getServerRelativeUrl(), v.getLabel(),
							antecedent != null ? antecedent.getObjectId() : "none"));
					}
					ShptFile f;
					try {
						f = new ShptFile(getEngine(), new ShptVersion(this.object.getFile(), v), antecedent);
					} catch (Exception ex) {
						throw new ExportException(String.format(
							"Failed to construct a new ShptVersion instance for [%s](%s)", getLabel(), v.getId()), ex);
					}
					tgt.add(f);
					antecedent = f;
				}

				if ((antecedentId == null) && (antecedent != null)) {
					antecedentId = antecedent.getObjectId();
				}

				this.predecessors = Tools.freezeList(pred);
				this.successors = Tools.freezeList(succ);
				// TODO: See previous note about version detection/antecedent detection
				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("FILE [%s] - version %s is anteceded by %s", this.object
						.getServerRelativeUrl(), this.versionNumber, antecedentId != null ? antecedentId : "none"));
				}
				if (antecedentId != null) {
					object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.VERSION_PRIOR.name,
						StoredDataType.ID, false, Collections.singleton(new StoredValue(antecedentId))));
				}
			} catch (ShptSessionException e) {
				throw new ExportException(String.format("Failed to retrieve file versions for [%s]",
					this.object.getServerRelativeUrl()), e);
			}
		}

		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.VERSION.name, StoredDataType.STRING, true,
			versionNames));
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.VERSION_TREE.name, StoredDataType.ID,
			false, Collections.singleton(new StoredValue(getBatchId()))));

	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(ShptSession service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(service, marshaled, ctx);
		ShptUser author = null;
		try {
			author = new ShptUser(getEngine(), service.getFileAuthor(this.object.getServerRelativeUrl()));
			ret.add(author);
			marshaled.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OWNER.name, StoredDataType.STRING,
				false, Collections.singleton(new StoredValue(author.getName()))));
		} catch (IncompleteDataException e) {
			this.log.warn(e.getMessage());
		}

		ShptUser modifier = null;
		ShptUser creator = author;
		try {
			if (this.version == null) {
				modifier = new ShptUser(getEngine(), service.getModifiedByUser(this.object.getServerRelativeUrl()));
			} else {
				// TODO: How in the hell can we get the version's creator via JShare?
				modifier = new ShptUser(getEngine(), service.getModifiedByUser(this.object.getServerRelativeUrl()));
				// creator = modifier;
			}
		} catch (IncompleteDataException e) {
			this.log.warn(e.getMessage());
		}

		if (creator != null) {
			ret.add(creator);
			marshaled.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.CREATOR.name, StoredDataType.STRING,
				false, Collections.singleton(new StoredValue(creator.getName()))));

		}

		if (modifier != null) {
			ret.add(modifier);
			marshaled.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.MODIFIER.name,
				StoredDataType.STRING, false, Collections.singleton(new StoredValue(modifier.getName()))));
		}

		for (ShptFile f : this.predecessors) {
			ret.add(f);
		}

		return ret;
	}

	@Override
	protected Collection<ShptObject<?>> findDependents(ShptSession service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findDependents(service, marshaled, ctx);

		for (ShptFile f : this.successors) {
			ret.add(f);
		}

		return ret;
	}

	public static ShptFile locateFile(ShptExportEngine engine, ShptSession service, String searchKey) throws Exception {
		Matcher m = ShptFile.SEARCH_KEY_PARSER.matcher(searchKey);
		if (!m.matches()) {
			File f = service.getFile(searchKey);
			if (f != null) { return new ShptFile(engine, f); }
			return null;
		}
		final String url = m.group(1);
		File f = service.getFile(url);
		if (f == null) { return null; }
		String version = m.group(2);
		if (Tools.equals(version, String.format("%d.%d", f.getMajorVersion(), f.getMinorVersion()))) { return new ShptFile(
			engine, f); }
		for (FileVersion v : service.getFileVersions(url)) {
			if (Tools.equals(version, v.getLabel())) { return new ShptFile(engine, f, v); }
		}
		// Nothing found...
		return null;
	}

	@Override
	protected List<ContentInfo> storeContent(ShptSession session, StoredObject<StoredValue> marshaled,
		ExportTarget referrent, ContentStore streamStore) throws Exception {
		// TODO: We NEED to use something other than the object ID here...
		Handle h = streamStore.getHandle(marshaled, "");
		InputStream in = null;
		if (this.version == null) {
			in = session.getFileStream(this.object.getServerRelativeUrl());
		} else {
			in = session.getInputStream(this.version.getUrl());
		}
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
		List<ContentInfo> ret = new ArrayList<ContentInfo>();
		ret.add(new ContentInfo(h.getQualifier()));
		return ret;
	}
}