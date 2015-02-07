package com.armedia.cmf.engine.sharepoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.CheckOutType;
import com.independentsoft.share.CustomizedPageStatus;
import com.independentsoft.share.File;
import com.independentsoft.share.FileLevel;
import com.independentsoft.share.FileVersion;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;

public class ShptFile extends ShptFSObject<File> {

	private static final Pattern SEARCH_KEY_PARSER = Pattern.compile("^([^#]*)#(\\d+\\.\\d+)?$");

	private final FileVersion version;
	private final ShptVersionNumber versionNumber;
	private final String antecedentId;

	private List<ShptFile> predecessors = Collections.emptyList();
	private List<ShptFile> successors = Collections.emptyList();

	public ShptFile(Service service, File file) {
		this(service, file, null, null);
	}

	private ShptFile(Service service, File file, FileVersion version) {
		this(service, file, version, null);
	}

	private ShptFile(Service service, File file, FileVersion version, ShptFile antecedent) {
		super(service, file, StoredObjectType.DOCUMENT);
		this.version = version;
		if (version == null) {
			this.versionNumber = new ShptVersionNumber(file.getMajorVersion(), file.getMinorVersion());
		} else {
			this.versionNumber = new ShptVersionNumber(version.getLabel());
		}
		this.antecedentId = (antecedent != null ? antecedent.getId() : null);
	}

	@Override
	public String getId() {
		return String.format("%s-%s", super.getId(), this.versionNumber.toString());
	}

	public ShptVersionNumber getVersionNumber() {
		return this.versionNumber;
	}

	@Override
	public String getSearchKey() {
		return String
			.format(String.format("%s#%s", this.wrapped.getServerRelativeUrl(), this.versionNumber.toString()));
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

	public String getCheckInComment() {
		return this.wrapped.getCheckInComment();
	}

	public CheckOutType getCheckOutType() {
		return this.wrapped.getCheckOutType();
	}

	public String getContentTag() {
		return this.wrapped.getContentTag();
	}

	public CustomizedPageStatus getCustomizedPageStatus() {
		return this.wrapped.getCustomizedPageStatus();
	}

	public String getETag() {
		return this.wrapped.getETag();
	}

	public boolean exists() {
		return this.wrapped.exists();
	}

	public long getLength() {
		return this.wrapped.getLength();
	}

	public String getLinkingUrl() {
		return this.wrapped.getLinkingUrl();
	}

	public FileLevel getLevel() {
		return this.wrapped.getLevel();
	}

	public int getMajorVersion() {
		return this.wrapped.getMajorVersion();
	}

	public int getMinorVersion() {
		return this.wrapped.getMinorVersion();
	}

	public String getTitle() {
		return this.wrapped.getTitle();
	}

	public int getUIVersion() {
		return this.wrapped.getUIVersion();
	}

	public String getUIVersionLabel() {
		return this.wrapped.getUIVersionLabel();
	}

	@Override
	public String getBatchId() {
		// This only takes into account the path, so it'll be shared by all versions of the file
		return super.getId();
	}

	@Override
	public String getLabel() {
		return String.format("%s#%s", this.wrapped.getName(), this.versionNumber.toString());
	}

	@Override
	protected void marshal(StoredObject<StoredValue> object) throws ExportException {
		super.marshal(object);
		List<StoredValue> versionNames = new ArrayList<StoredValue>();

		versionNames.add(new StoredValue(this.versionNumber.toString()));
		if (this.version != null) {
			this.predecessors = Collections.emptyList();
			this.successors = Collections.emptyList();
			if (this.antecedentId != null) {
				object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.VERSION_PRIOR.name,
					StoredDataType.ID, false, Collections.singleton(new StoredValue(this.antecedentId))));
			}
		} else {
			String antecedentId = this.antecedentId;
			versionNames.add(new StoredValue("CURRENT"));
			try {
				List<FileVersion> l = this.service.getFileVersions(this.wrapped.getServerRelativeUrl());
				// TODO: Temporarily disable traversing the version results
				// l = Collections.emptyList();
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
							antecedentId = antecedent.getId();
							antecedent = this;
						}
					} else if (c == 0) {
						tgt = succ;
						antecedent = this;
						continue;
					}
					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("FILE [%s] - version %s is anteceded by %s", this.wrapped
							.getServerRelativeUrl(), v.getLabel(), antecedent != null ? antecedent.getId() : "none"));
					}
					ShptFile f = new ShptFile(this.service, this.wrapped, v, antecedent);
					tgt.add(f);
					antecedent = f;
				}

				if ((antecedentId == null) && (antecedent != null)) {
					antecedentId = antecedent.getId();
				}

				this.predecessors = Tools.freezeList(pred);
				this.successors = Tools.freezeList(succ);
				// TODO: See previous note about version detection/antecedent detection
				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("FILE [%s] - version %s is anteceded by %s", this.wrapped
						.getServerRelativeUrl(), this.versionNumber, antecedentId != null ? antecedentId : "none"));
				}
				if (antecedentId != null) {
					object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.VERSION_PRIOR.name,
						StoredDataType.ID, false, Collections.singleton(new StoredValue(antecedentId))));
				}
			} catch (ServiceException e) {
				throw new ExportException(String.format("Failed to retrieve file versions for [%s]",
					this.wrapped.getServerRelativeUrl()), e);
			}
		}

		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.VERSION.name, StoredDataType.STRING, true,
			versionNames));
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.VERSION_TREE.name, StoredDataType.ID,
			false, Collections.singleton(new StoredValue(getBatchId()))));

	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(Service service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(service, marshaled, ctx);
		ShptUser author = new ShptUser(service, service.getFileAuthor(this.wrapped.getServerRelativeUrl()));
		ret.add(author);
		marshaled.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OWNER.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(author.getName()))));

		for (ShptFile f : this.predecessors) {
			ret.add(f);
		}

		ShptContent content = new ShptContent(service, this.wrapped, this.version);
		ret.add(content);
		marshaled.setProperty(new StoredProperty<StoredValue>(ShptProperties.CONTENTS.name, StoredDataType.ID, false,
			new StoredValue(StoredDataType.ID, content.getId())));

		return ret;
	}

	@Override
	protected Collection<ShptObject<?>> findDependents(Service service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findDependents(service, marshaled, ctx);

		for (ShptFile f : this.successors) {
			ret.add(f);
		}

		return ret;
	}

	public static ShptFile locateFile(Service service, String searchKey) throws ServiceException {
		Matcher m = ShptFile.SEARCH_KEY_PARSER.matcher(searchKey);
		if (!m.matches()) {
			File f = service.getFile(searchKey);
			if (f != null) { return new ShptFile(service, f); }
			return null;
		}
		final String url = m.group(1);
		File f = service.getFile(url);
		if (f == null) { return null; }
		String version = m.group(2);
		if (Tools.equals(version, String.format("%d.%d", f.getMajorVersion(), f.getMinorVersion()))) { return new ShptFile(
			service, f); }
		for (FileVersion v : service.getFileVersions(url)) {
			if (Tools.equals(version, v.getLabel())) { return new ShptFile(service, f, v); }
		}
		// Nothing found...
		return null;
	}
}