/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.engine.sharepoint.exporter;

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

import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.sharepoint.IncompleteDataException;
import com.armedia.caliente.engine.sharepoint.ShptAttributes;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionException;
import com.armedia.caliente.engine.sharepoint.ShptVersionNumber;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.tools.MimeTools;
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

	public ShptFile(ShptExportDelegateFactory factory, ShptSession session, File file) throws Exception {
		this(factory, session, new ShptVersion(file), null);
	}

	protected ShptFile(ShptExportDelegateFactory factory, ShptSession session, File file, FileVersion version)
		throws Exception {
		this(factory, session, new ShptVersion(file, version), null);
	}

	protected ShptFile(ShptExportDelegateFactory factory, ShptSession session, ShptVersion object) throws Exception {
		this(factory, session, object, null);
	}

	protected ShptFile(ShptExportDelegateFactory factory, ShptSession session, ShptVersion object, ShptFile antecedent)
		throws Exception {
		super(factory, session, ShptVersion.class, object);
		this.version = object.getVersion();
		this.versionNumber = object.getVersionNumber();
		this.antecedentId = (antecedent != null ? antecedent.getObjectId() : null);
	}

	@Override
	public String calculateObjectId(ShptSession session, ShptVersion object) {
		return String.format("%s-%s", super.calculateObjectId(session, object), object.getVersionNumber().toString());
	}

	@Override
	public String calculateHistoryId(ShptSession session, ShptVersion file) {
		// This only takes into account the path, so it'll be shared by all versions of the file
		return super.calculateObjectId(session, file);
	}

	@Override
	public String calculateLabel(ShptSession session, ShptVersion file) {
		return String.format("%s#%s", file.getServerRelativeUrl(), file.getVersionNumber().toString());
	}

	@Override
	public String calculateSearchKey(ShptSession session, ShptVersion object) {
		return String.format(
			String.format("%s#%s", object.getFile().getServerRelativeUrl(), object.getVersionNumber().toString()));
	}

	@Override
	public String calculateServerRelativeUrl(ShptSession session, ShptVersion file) {
		return file.getFile().getServerRelativeUrl();
	}

	public ShptVersionNumber getVersionNumber() {
		return this.object.getVersionNumber();
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
	protected boolean marshal(ShptExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		if (!super.marshal(ctx, object)) { return false; }
		final ShptSession service = ctx.getSession();
		List<CmfValue> versionNames = new ArrayList<>();

		if (this.version != null) {
			Date d = this.version.getCreatedTime();
			if (d != null) {
				Collection<CmfValue> c = Collections.singleton(new CmfValue(d));
				object.setAttribute(
					new CmfAttribute<>(ShptAttributes.CREATE_DATE.name, CmfValue.Type.DATETIME, false, c));
				object.setAttribute(
					new CmfAttribute<>(ShptAttributes.MODIFICATION_DATE.name, CmfValue.Type.DATETIME, false, c));
			}
		}

		versionNames.add(new CmfValue(this.versionNumber.toString()));

		CmfAttribute<CmfValue> current = new CmfAttribute<>(IntermediateAttribute.IS_LATEST_VERSION,
			CmfValue.Type.BOOLEAN, false);
		current.setValue(new CmfValue((this.version == null) || this.version.isCurrentVersion()));
		object.setAttribute(current);

		final boolean isRoot;
		if (this.version != null) {
			this.predecessors = Collections.emptyList();
			this.successors = Collections.emptyList();
			isRoot = (this.antecedentId == null);
			if (this.antecedentId != null) {
				object.setAttribute(new CmfAttribute<>(ShptAttributes.VERSION_PRIOR.name, CmfValue.Type.ID, false,
					Collections.singleton(new CmfValue(this.antecedentId))));
			}
		} else {
			String antecedentId = this.antecedentId;
			try {
				List<FileVersion> l = service.getFileVersions(this.object.getServerRelativeUrl());
				Map<ShptVersionNumber, FileVersion> versions = new TreeMap<>();
				for (FileVersion v : l) {
					final ShptVersionNumber n = new ShptVersionNumber(v.getLabel());
					versions.put(n, v);
				}

				List<ShptFile> pred = new ArrayList<>(versions.size());
				List<ShptFile> succ = new ArrayList<>(versions.size());
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
						this.log.debug("FILE [{}] - version {} is anteceded by {}", this.object.getServerRelativeUrl(),
							v.getLabel(), antecedent != null ? antecedent.getObjectId() : "none");
					}
					ShptFile f;
					try {
						f = new ShptFile(this.factory, service, new ShptVersion(this.object.getFile(), v), antecedent);
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
					this.log.debug("FILE [{}] - version {} is anteceded by {}", this.object.getServerRelativeUrl(),
						this.versionNumber, antecedentId != null ? antecedentId : "none");
				}
				if (antecedentId != null) {
					object.setAttribute(new CmfAttribute<>(ShptAttributes.VERSION_PRIOR.name, CmfValue.Type.ID, false,
						Collections.singleton(new CmfValue(antecedentId))));
				}
				isRoot = (antecedentId == null);
			} catch (ShptSessionException e) {
				throw new ExportException(
					String.format("Failed to retrieve file versions for [%s]", this.object.getServerRelativeUrl()), e);
			}
		}

		CmfProperty<CmfValue> versionTreeRoot = new CmfProperty<>(IntermediateProperty.VERSION_TREE_ROOT,
			CmfValue.Type.BOOLEAN, false);
		versionTreeRoot.setValue(new CmfValue(isRoot || ctx.getSettings().getBoolean(TransferSetting.LATEST_ONLY)));
		object.setProperty(versionTreeRoot);

		object.setAttribute(new CmfAttribute<>(ShptAttributes.VERSION.name, CmfValue.Type.STRING, true, versionNames));
		object.setAttribute(new CmfAttribute<>(ShptAttributes.VERSION_TREE.name, CmfValue.Type.ID, false,
			Collections.singleton(new CmfValue(getHistoryId()))));
		return true;
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(ShptSession service, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(service, marshaled, ctx);
		ShptUser author = null;
		try {
			author = new ShptUser(this.factory, service, service.getFileAuthor(this.object.getServerRelativeUrl()));
			ret.add(author);
			marshaled.setAttribute(new CmfAttribute<>(ShptAttributes.OWNER.name, CmfValue.Type.STRING, false,
				Collections.singleton(new CmfValue(author.getName()))));
		} catch (IncompleteDataException e) {
			this.log.warn(e.getMessage());
		}

		ShptUser modifier = null;
		ShptUser creator = author;
		try {
			if (this.version == null) {
				modifier = new ShptUser(this.factory, service,
					service.getModifiedByUser(this.object.getServerRelativeUrl()));
			} else {
				// TODO: How in the hell can we get the version's creator via JShare?
				modifier = new ShptUser(this.factory, service,
					service.getModifiedByUser(this.object.getServerRelativeUrl()));
				// creator = modifier;
			}
		} catch (IncompleteDataException e) {
			this.log.warn(e.getMessage());
		}

		if (creator != null) {
			ret.add(creator);
			marshaled.setAttribute(new CmfAttribute<>(ShptAttributes.CREATOR.name, CmfValue.Type.STRING, false,
				Collections.singleton(new CmfValue(creator.getName()))));

		}

		if (modifier != null) {
			ret.add(modifier);
			marshaled.setAttribute(new CmfAttribute<>(ShptAttributes.MODIFIER.name, CmfValue.Type.STRING, false,
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
			if (f != null) { return new ShptFile(factory, service, f); }
			return null;
		}
		final String url = m.group(1);
		File f = service.getFile(url);
		if (f == null) { return null; }
		String version = m.group(2);
		if (Tools.equals(version, String.format("%d.%d", f.getMajorVersion(), f.getMinorVersion()))) {
			return new ShptFile(factory, service, f);
		}
		for (FileVersion v : service.getFileVersions(url)) {
			if (Tools.equals(version, v.getLabel())) { return new ShptFile(factory, service, f, v); }
		}
		// Nothing found...
		return null;
	}

	@Override
	protected List<CmfContentStream> storeContent(ShptExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshaled, ExportTarget referrent, CmfContentStore<?, ?> streamStore,
		boolean includeRenditions) {
		final ShptSession session = ctx.getSession();
		CmfContentStream info = new CmfContentStream(0);
		final String name = this.object.getName();
		info.setFileName(name);
		info.setExtension(FilenameUtils.getExtension(name));
		CmfContentStore<?, ?>.Handle h = streamStore.getHandle(translator, marshaled, info);
		// TODO: sadly, this is not memory efficient for larger files...
		BinaryMemoryBuffer buf = new BinaryMemoryBuffer(10240);
		try (InputStream in = (this.version == null) ? session.getFileStream(this.object.getServerRelativeUrl())
			: session.getInputStream(this.version.getUrl())) {
			IOUtils.copy(in, buf);
			buf.close();
		} catch (Exception e) {
			this.log.error("Failed to read the content stream for {}", marshaled.getDescription(), e);
		}

		try {
			h.setContents(buf.getInputStream());
		} catch (CmfStorageException e) {
			this.log.error("Failed to store the content stream for {} into the content store",
				marshaled.getDescription(), e);
		}

		// Now, try to identify the content type...
		MimeType type = null;
		try (InputStream in = buf.getInputStream()) {
			type = MimeTools.determineMimeType(in);
		} catch (Exception e) {
			type = MimeTools.DEFAULT_MIME_TYPE;
		}

		marshaled.setAttribute(new CmfAttribute<>(ShptAttributes.CONTENT_TYPE.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(type.getBaseType()))));
		info.setMimeType(MimeTools.resolveMimeType(type.getBaseType()));
		info.setLength(buf.getCurrentSize());
		List<CmfContentStream> ret = new ArrayList<>();
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
	protected String calculateName(ShptSession session, ShptVersion version) throws Exception {
		return version.getName();
	}

	@Override
	protected boolean calculateHistoryCurrent(ShptSession session, ShptVersion version) throws Exception {
		return version.isCurrentVersion();
	}
}