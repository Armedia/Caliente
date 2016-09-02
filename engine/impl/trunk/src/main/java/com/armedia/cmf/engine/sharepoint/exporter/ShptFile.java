package com.armedia.cmf.engine.sharepoint.exporter;

import java.io.InputStream;
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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.armedia.cmf.engine.TransferSetting;
import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.IncompleteDataException;
import com.armedia.cmf.engine.sharepoint.ShptAttributes;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionException;
import com.armedia.cmf.engine.sharepoint.ShptVersionNumber;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.tools.MimeTools;
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

	public ShptFile(ShptExportDelegateFactory factory, File file) throws Exception {
		this(factory, new ShptVersion(file), null);
	}

	protected ShptFile(ShptExportDelegateFactory factory, File file, FileVersion version) throws Exception {
		this(factory, new ShptVersion(file, version), null);
	}

	protected ShptFile(ShptExportDelegateFactory factory, ShptVersion object) throws Exception {
		this(factory, object, null);
	}

	protected ShptFile(ShptExportDelegateFactory factory, ShptVersion object, ShptFile antecedent) throws Exception {
		super(factory, ShptVersion.class, object);
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
		return String.format(
			String.format("%s#%s", object.getFile().getServerRelativeUrl(), object.getVersionNumber().toString()));
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
		return String.format("%s#%s", this.factory.getRelativePath(file.getServerRelativeUrl()),
			file.getVersionNumber().toString());
	}

	@Override
	protected boolean marshal(ShptExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		if (!super.marshal(ctx, object)) { return false; }
		final ShptSession service = ctx.getSession();
		List<CmfValue> versionNames = new ArrayList<CmfValue>();

		if (this.version != null) {
			Date d = this.version.getCreatedTime();
			if (d != null) {
				Collection<CmfValue> c = Collections.singleton(new CmfValue(d));
				object.setAttribute(
					new CmfAttribute<CmfValue>(ShptAttributes.CREATE_DATE.name, CmfDataType.DATETIME, false, c));
				object.setAttribute(
					new CmfAttribute<CmfValue>(ShptAttributes.MODIFICATION_DATE.name, CmfDataType.DATETIME, false, c));
			}
		}

		versionNames.add(new CmfValue(this.versionNumber.toString()));

		CmfAttribute<CmfValue> current = new CmfAttribute<CmfValue>(IntermediateAttribute.IS_LATEST_VERSION,
			CmfDataType.BOOLEAN, false);
		current.setValue(new CmfValue((this.version == null) || this.version.isCurrentVersion()));
		object.setAttribute(current);

		final boolean isRoot;
		if (this.version != null) {
			this.predecessors = Collections.emptyList();
			this.successors = Collections.emptyList();
			isRoot = (this.antecedentId == null);
			if (this.antecedentId != null) {
				object.setAttribute(new CmfAttribute<CmfValue>(ShptAttributes.VERSION_PRIOR.name, CmfDataType.ID, false,
					Collections.singleton(new CmfValue(this.antecedentId))));
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
							if (antecedent == null) {
								antecedent = this;
							}
							antecedentId = antecedent.getObjectId();
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
						f = new ShptFile(this.factory, new ShptVersion(this.object.getFile(), v), antecedent);
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
					this.log.debug(
						String.format("FILE [%s] - version %s is anteceded by %s", this.object.getServerRelativeUrl(),
							this.versionNumber, antecedentId != null ? antecedentId : "none"));
				}
				if (antecedentId != null) {
					object.setAttribute(new CmfAttribute<CmfValue>(ShptAttributes.VERSION_PRIOR.name, CmfDataType.ID,
						false, Collections.singleton(new CmfValue(antecedentId))));
				}
				isRoot = (antecedentId == null);
			} catch (ShptSessionException e) {
				throw new ExportException(
					String.format("Failed to retrieve file versions for [%s]", this.object.getServerRelativeUrl()), e);
			}
		}

		CmfProperty<CmfValue> versionTreeRoot = new CmfProperty<CmfValue>(IntermediateProperty.VERSION_TREE_ROOT,
			CmfDataType.BOOLEAN, false);
		versionTreeRoot.setValue(new CmfValue(isRoot || ctx.getSettings().getBoolean(TransferSetting.LATEST_ONLY)));
		object.setProperty(versionTreeRoot);

		object.setAttribute(
			new CmfAttribute<CmfValue>(ShptAttributes.VERSION.name, CmfDataType.STRING, true, versionNames));
		object.setAttribute(new CmfAttribute<CmfValue>(ShptAttributes.VERSION_TREE.name, CmfDataType.ID, false,
			Collections.singleton(new CmfValue(getBatchId()))));
		return true;
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(ShptSession service, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(service, marshaled, ctx);
		ShptUser author = null;
		try {
			author = new ShptUser(this.factory, service.getFileAuthor(this.object.getServerRelativeUrl()));
			ret.add(author);
			marshaled.setAttribute(new CmfAttribute<CmfValue>(ShptAttributes.OWNER.name, CmfDataType.STRING, false,
				Collections.singleton(new CmfValue(author.getName()))));
		} catch (IncompleteDataException e) {
			this.log.warn(e.getMessage());
		}

		ShptUser modifier = null;
		ShptUser creator = author;
		try {
			if (this.version == null) {
				modifier = new ShptUser(this.factory, service.getModifiedByUser(this.object.getServerRelativeUrl()));
			} else {
				// TODO: How in the hell can we get the version's creator via JShare?
				modifier = new ShptUser(this.factory, service.getModifiedByUser(this.object.getServerRelativeUrl()));
				// creator = modifier;
			}
		} catch (IncompleteDataException e) {
			this.log.warn(e.getMessage());
		}

		if (creator != null) {
			ret.add(creator);
			marshaled.setAttribute(new CmfAttribute<CmfValue>(ShptAttributes.CREATOR.name, CmfDataType.STRING, false,
				Collections.singleton(new CmfValue(creator.getName()))));

		}

		if (modifier != null) {
			ret.add(modifier);
			marshaled.setAttribute(new CmfAttribute<CmfValue>(ShptAttributes.MODIFIER.name, CmfDataType.STRING, false,
				Collections.singleton(new CmfValue(modifier.getName()))));
		}

		return ret;
	}

	@Override
	protected Collection<ShptObject<?>> findAntecedents(ShptSession service, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findAntecedents(service, marshaled, ctx);
		for (ShptFile f : this.predecessors) {
			ret.add(f);
		}
		return ret;
	}

	@Override
	protected Collection<ShptObject<?>> findSuccessors(ShptSession service, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findSuccessors(service, marshaled, ctx);
		for (ShptFile f : this.successors) {
			ret.add(f);
		}
		return ret;
	}

	public static ShptFile locateFile(ShptExportDelegateFactory factory, ShptSession service, String searchKey)
		throws Exception {
		Matcher m = ShptFile.SEARCH_KEY_PARSER.matcher(searchKey);
		if (!m.matches()) {
			File f = service.getFile(searchKey);
			if (f != null) { return new ShptFile(factory, f); }
			return null;
		}
		final String url = m.group(1);
		File f = service.getFile(url);
		if (f == null) { return null; }
		String version = m.group(2);
		if (Tools.equals(version,
			String.format("%d.%d", f.getMajorVersion(), f.getMinorVersion()))) { return new ShptFile(factory, f); }
		for (FileVersion v : service.getFileVersions(url)) {
			if (Tools.equals(version, v.getLabel())) { return new ShptFile(factory, f, v); }
		}
		// Nothing found...
		return null;
	}

	@Override
	protected List<CmfContentInfo> storeContent(ShptExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshaled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore,
		boolean includeRenditions) throws Exception {
		final ShptSession session = ctx.getSession();
		CmfContentInfo info = new CmfContentInfo();
		final String name = this.object.getName();
		info.setFileName(name);
		info.setExtension(FilenameUtils.getExtension(name));
		CmfContentStore<?, ?, ?>.Handle h = streamStore.getHandle(translator, marshaled, info);
		InputStream in = null;
		if (this.version == null) {
			in = session.getFileStream(this.object.getServerRelativeUrl());
		} else {
			in = session.getInputStream(this.version.getUrl());
		}
		// TODO: sadly, this is not memory efficient for larger files...
		BinaryMemoryBuffer buf = new BinaryMemoryBuffer(10240);
		try {
			IOUtils.copy(in, buf);
			buf.close();
			h.setContents(buf.getInputStream());
		} finally {
			IOUtils.closeQuietly(in);
		}
		// Now, try to identify the content type...
		in = buf.getInputStream();
		MimeType type = null;
		try {
			type = MimeTools.determineMimeType(in);
		} catch (Exception e) {
			type = MimeTools.DEFAULT_MIME_TYPE;
		}

		marshaled.setAttribute(new CmfAttribute<CmfValue>(ShptAttributes.CONTENT_TYPE.name, CmfDataType.STRING, false,
			Collections.singleton(new CmfValue(type.getBaseType()))));
		info.setMimeType(MimeTools.resolveMimeType(type.getBaseType()));
		info.setLength(buf.getCurrentSize());
		List<CmfContentInfo> ret = new ArrayList<CmfContentInfo>();
		ret.add(info);
		return ret;
	}

	static String doCalculateObjectId(File object) {
		String searchKey = object.getServerRelativeUrl();
		return String.format("%08X", Tools.hashTool(searchKey, null, searchKey));
	}

	static String doCalculateSearchKey(File object) {
		return object.getServerRelativeUrl();
	}

	@Override
	protected String calculateName(ShptVersion version) throws Exception {
		return version.getName();
	}

	@Override
	protected boolean calculateBatchHead(ShptVersion version) throws Exception {
		return version.isCurrentVersion();
	}
}