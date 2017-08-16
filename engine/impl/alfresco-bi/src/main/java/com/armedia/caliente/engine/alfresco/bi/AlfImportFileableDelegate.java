package com.armedia.caliente.engine.alfresco.bi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.alfresco.bi.cache.CacheItemMarker.MarkerType;
import com.armedia.caliente.engine.alfresco.bi.model.AlfrescoType;
import com.armedia.caliente.engine.alfresco.bi.model.SchemaAttribute;
import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.tools.AclTools.AccessorType;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectHandler;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueSerializer;
import com.armedia.caliente.store.tools.DefaultCmfObjectHandler;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.Tools;

abstract class AlfImportFileableDelegate extends AlfImportDelegate {

	protected static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	private static final String METADATA_SUFFIX = ".metadata.properties.xml";

	public static final String REFERENCE_TYPE = "arm:reference";
	public static final String VDOC_REFERENCE_TYPE = "arm:vdocReference";
	public static final String STATUS_ASPECT = "arm:calienteStatus";
	public static final String CALIENTE_ASPECT = "arm:caliente";

	private static final String TYPE_PROPERTY = "type";
	private static final String ASPECT_PROPERTY = "aspects";

	private static final Map<String, String> ATTRIBUTE_MAPPER;
	private static final Map<String, String> ATTRIBUTE_SPECIAL_COPIES;
	private static final Set<String> USER_CONVERSIONS;

	private static final Pattern VDOC_MEMBER_PARSER = Pattern.compile("^\\[(.+)\\]\\{(.+)\\}$");

	private static enum PermitValue {
		//
		NONE, BROWSE, READ, RELATE('L'), VERSION, WRITE, DELETE,
		//
		;

		private final char tag;

		private PermitValue() {
			this(null);
		}

		private PermitValue(Character tag) {
			if (tag == null) {
				tag = name().charAt(0);
			}
			this.tag = tag;
		}
	}

	static {
		Map<String, String> m = new HashMap<>();

		// Attribute X "gets populated from" Y
		String[][] unmappings = {
			{
				"dctm:acl_name", "cmf:acl_name"
			}, {
				"dctm:group_name", "cmf:group"
			}, {
				"dctm:group_permit", "cmf:group_permission"
			}, {
				"dctm:r_access_date", "cmf:last_access_date"
			}, {
				"dctm:acl_domain", "cmf:login_realm"
			}, {
				"dctm:owner_name", "cmf:owner"
			}, {
				"dctm:owner_permit", "cmf:owner_permission"
			}, {
				"dctm:i_antecedent_id", "cmf:version_antecedent_id"
			}, {
				"dctm:i_vstamp", "cmis:changeToken"
			}, {
				"dctm:log_entry", "cmis:checkinComment"
			}, {
				"dctm:r_full_content_size", "cmis:contentStreamLength"
			}, {
				"dctm:a_content_type", "cmis:contentStreamMimeType"
			}, {
				"dctm:r_creator_name", "cmis:createdBy"
			}, {
				"dctm:r_creation_date", "cmis:creationDate"
			}, {
				"dctm:title", "cmis:description"
			}, {
				"dctm:r_immutable_flag", "cmis:isImmutable"
			}, {
				"dctm:i_has_folder", "cmis:isLatestVersion"
			}, {
				"dctm:r_modify_date", "cmis:lastModificationDate"
			}, {
				"dctm:r_modifier", "cmis:lastModifiedBy"
			}, {
				"dctm:object_name", "cmis:name"
			}, {
				"dctm:r_object_type", "cmis:objectTypeId"
			}, {
				"dctm:i_folder_id", "cmis:parentId"
			}, {
				"dctm:r_version_label", "cmis:versionLabel"
			}, {
				"dctm:r_lock_owner", "cmis:versionSeriesCheckedOutBy"
			}, {
				"dctm:i_chronicle_id", "cmis:versionSeriesId"
			}, {
				"dctm:r_folder_path", "cmis:path"
			}, {
				"dctm:binding_condition", "binding_condition"
			}, {
				"dctm:binding_label", "binding_label"
			}, {
				"dctm:reference_by_id", "reference_by_id"
			},
		};

		for (String[] s : unmappings) {
			m.put(s[0], s[1]);
		}
		ATTRIBUTE_MAPPER = Tools.freezeMap(m);

		// Attribute X "gets populated from" Y, Y may need re-mapping through ATTRIBUTE_MAPPER
		m = new HashMap<>();
		String[][] copies = {
			{
				"cm:author", "dctm:authors"
			}, {
				"cm:description", "dctm:log_entry"
			}, {
				"cm:subjectline", "dctm:message_subject"
			}, {
				"cm:name", "dctm:object_name"
			}, {
				"cm:owner", "dctm:owner_name"
			}, {
				"cm:accessed", "dctm:r_access_date"
			}, {
				"cm:created", "dctm:r_creation_date"
			}, {
				"cm:creator", "dctm:r_creator_name"
			}, {
				"cm:modifier", "dctm:r_modifier"
			}, {
				"cm:modified", "dctm:r_modify_date"
			}, {
				"cm:sentdate", "dctm:sent_date"
			}, {
				"cm:title", "dctm:title"
			},
		};
		for (String[] s : copies) {
			m.put(s[0], s[1]);
		}
		ATTRIBUTE_SPECIAL_COPIES = Tools.freezeMap(m);

		m = new HashMap<>();
		String[] conversions = {
			"cm:owner", "cm:creator", "cm:modifier"
		};
		for (String s : conversions) {
			m.put(s, s);
		}
		USER_CONVERSIONS = Tools.freezeSet(new HashSet<>(m.keySet()));
	}

	private final boolean reference;
	private final boolean virtual;
	private final AlfrescoType defaultType;
	private final AlfrescoType referenceType;
	private final AlfrescoType vdocRoot;
	private final AlfrescoType vdocVersion;
	private final AlfrescoType vdocReference;

	public AlfImportFileableDelegate(String defaultType, AlfImportDelegateFactory factory,
		CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, storedObject);
		CmfValue reference = getAttributeValue("dctm:i_is_reference");
		this.reference = ((reference != null) && !reference.isNull() && reference.asBoolean());
		CmfValue virtual = getPropertyValue(IntermediateProperty.VDOC_HISTORY);
		this.virtual = ((virtual != null) && !virtual.isNull() && virtual.asBoolean());
		this.defaultType = this.factory.getType(defaultType);
		this.referenceType = this.factory.getType(AlfImportFileableDelegate.REFERENCE_TYPE);
		this.vdocRoot = this.factory.getType("cm:folder", "arm:vdocRoot");
		this.vdocVersion = this.factory.getType("cm:folder", "arm:vdocVersion");
		this.vdocReference = this.factory.getType(AlfImportFileableDelegate.REFERENCE_TYPE,
			AlfImportFileableDelegate.VDOC_REFERENCE_TYPE);
	}

	protected final boolean isVirtual() {
		return this.virtual;
	}

	protected final boolean isReference() {
		return this.reference;
	}

	protected AlfrescoType calculateTargetType(CmfContentInfo content) throws ImportException {
		AlfrescoType type = this.factory.mapType(this.cmfObject.getSubtype().toLowerCase());
		if ((type == null) && (this.defaultType != null)) {
			type = this.defaultType;
		}
		return type;
	}

	protected final AlfrescoType getTargetType(CmfContentInfo content) throws ImportException {
		AlfrescoType type = null;
		if (isReference()) {
			// If this is a reference - folder or document, doesn't matter...
			type = this.referenceType;
		} else {
			type = calculateTargetType(content);
		}

		// No match? Error!
		if (type == null) { throw new ImportException(String.format(
			"Failed to find a proper type mapping for %s of type [%s] (%s rendition, page #%d)",
			this.cmfObject.getType(), this.cmfObject.getSubtype(),
			content.isDefaultRendition() ? "default" : content.getRenditionIdentifier(), content.getRenditionPage())); }

		return type;
	}

	protected final CmfAttribute<CmfValue> mapAttributeName(String attributeName) {
		String target = AlfImportFileableDelegate.ATTRIBUTE_MAPPER.get(attributeName);
		if (target == null) { return null; }
		return this.cmfObject.getAttribute(target);
	}

	protected final void storeValue(AlfImportContext ctx, CmfProperty<CmfValue> srcAtt, SchemaAttribute tgtAtt,
		Properties p, boolean concatenateFallback) throws ImportException {
		final String value;
		// If the source attribute is repeating, but the target isn't, we'll concatenate
		if (!srcAtt.hasValues()) { return; }

		CmfValueSerializer serializer = CmfValueSerializer.get(srcAtt.getType());
		switch (srcAtt.getType()) {
			case DATETIME:
				break;
			default:
				// Use the default one for everyone else
				serializer = CmfValueSerializer.get(CmfDataType.STRING);
				break;
		}

		try {
			if (srcAtt.isRepeating()) {
				if (tgtAtt.multiple || concatenateFallback) {
					// Concatenate using the separator
					StringBuilder sb = new StringBuilder();
					char separator = ',';
					// TODO: Detect separator - if it exists, try another
					int i = 0;
					for (CmfValue v : srcAtt) {
						if (i > 0) {
							sb.append(separator);
						}
						sb.append(serializer.serialize(v));
						i++;
					}
					value = sb.toString();
				} else {
					value = serializer.serialize(srcAtt.getValue());
				}
			} else if (!srcAtt.getValue().isNull()) {
				// Write out the single value
				value = serializer.serialize(srcAtt.getValue());
			} else {
				value = null;
			}
		} catch (ParseException e) {
			throw new ImportException(String.format(
				"Failed to serialize the values from source attribute [%s] for %s [%s](%s)", srcAtt.getName(),
				this.cmfObject.getType(), this.cmfObject.getLabel(), ctx.getObjectName(this.cmfObject)), e);
		}

		if (!StringUtils.isEmpty(value)) {
			p.setProperty(tgtAtt.name, value);
		}
	}

	protected abstract boolean createStub(AlfImportContext ctx, File target, String content) throws ImportException;

	protected final void populatePrimaryAttributes(AlfImportContext ctx, Properties p, AlfrescoType targetType,
		CmfContentInfo content) throws ImportException {

		Set<String> tgtNames = targetType.getAttributeNames();
		Set<String> srcNames = this.cmfObject.getAttributeNames();

		Set<String> common = new HashSet<>();
		common.addAll(tgtNames);
		common.retainAll(srcNames);
		// Copy everything that exists with the same name at the source and at the target
		for (String s : common) {
			final CmfProperty<CmfValue> src = this.cmfObject.getAttribute(s);
			final SchemaAttribute tgt = targetType.getAttribute(s);
			storeValue(ctx, src, tgt, p, true);
		}

		Set<String> tgtOnly = new HashSet<>();
		tgtOnly.addAll(tgtNames);
		tgtOnly.removeAll(srcNames);
		// Copy everything that exists only at the target
		for (final String tgtAttName : tgtOnly) {
			final SchemaAttribute tgtAtt = targetType.getAttribute(tgtAttName);

			// Easy path: is this already mapped?
			String srcAttName = AlfImportFileableDelegate.ATTRIBUTE_MAPPER.get(tgtAttName);
			if (srcAttName == null) {
				// This is because the source attributes all come with either "dctm:", "cmf:", or
				// "cmis:" as a prefix, so we first try the happy path (prefix "dctm:")...
				// attributes with the prefixes "cmis:" and "cmf:" are dealt with separately,
				// below (via ATTRIBUTE_SPECIAL_COPIES)
				srcAttName = tgtAttName.replaceAll("^[^:]+:", "dctm:");
			}

			CmfProperty<CmfValue> srcAtt = this.cmfObject.getAttribute(srcAttName);
			if (srcAtt == null) {
				srcAtt = this.cmfObject.getProperty(srcAttName);
				if (srcAtt == null) {
					if (this.log.isDebugEnabled()) {
						this.log.warn(String.format(
							"Target attribute [%s] in target type [%s] with aspects %s mapped to source attribute [%s] from source type [%s], but no such attribute or property was found",
							tgtAttName, targetType.getName(), targetType.getAspects(), srcAttName,
							this.cmfObject.getSubtype()));
					}
					continue;
				}
			}

			// We're set, do it!!
			storeValue(ctx, srcAtt, tgtAtt, p, true);
		}

		// Now, do the mappings as copies of what has already been copied over, except when the
		// source attribute is repeating.
		for (final String specialName : AlfImportFileableDelegate.ATTRIBUTE_SPECIAL_COPIES.keySet()) {
			// First get the attribute the special copy must go to
			final SchemaAttribute specialAtt = targetType.getAttribute(specialName);
			if (specialAtt == null) {
				continue;
			}

			String srcName = AlfImportFileableDelegate.ATTRIBUTE_SPECIAL_COPIES.get(specialName);
			// If it didn't need mapping, stick to the original name.
			srcName = Tools.coalesce(AlfImportFileableDelegate.ATTRIBUTE_MAPPER.get(srcName), srcName);

			CmfAttribute<CmfValue> srcAtt = this.cmfObject.getAttribute(srcName);
			if ((srcAtt == null) || !srcAtt.hasValues()) {
				continue;
			}

			storeValue(ctx, srcAtt, specialAtt, p, false);
		}

		// Now handle the special properties
		Set<String> values = new LinkedHashSet<>();
		for (CmfValue v : getPropertyValues(IntermediateProperty.PARENT_TREE_IDS)) {
			String s = v.asString();
			if (StringUtils.isEmpty(s)) {
				continue;
			}
			values.add(s);
		}
		p.setProperty("dctm:r_parent_path_ids", StringUtils.join(values, ','));

		values.clear();
		for (CmfValue v : getPropertyValues(IntermediateProperty.PATH)) {
			String s = v.asString();
			if (StringUtils.isEmpty(s)) {
				continue;
			}
			try {
				values.add(URLEncoder.encode(s, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new ImportException("Unsupported encoding UTF-8...what?!?!?", e);
			}
		}
		p.setProperty("dctm:r_parent_paths", StringUtils.join(values, ','));

		values.clear();
		for (CmfValue v : getPropertyValues(IntermediateProperty.USERS_WITH_DEFAULT_FOLDER)) {
			String s = v.asString();
			if (StringUtils.isEmpty(s)) {
				continue;
			}
			values.add(this.factory.mapUser(s));
		}
		if (!values.isEmpty()) {
			p.setProperty("arm:usersWithDefaultFolder", StringUtils.join(values, ','));
		}

		values.clear();
		for (CmfValue v : getPropertyValues(IntermediateProperty.GROUPS_WITH_DEFAULT_FOLDER)) {
			String s = v.asString();
			if (StringUtils.isEmpty(s)) {
				continue;
			}
			values.add(this.factory.mapGroup(s));
		}
		if (!values.isEmpty()) {
			p.setProperty("arm:groupsWithDefaultFolder", StringUtils.join(values, ','));
		}

		// Set the type property
		p.setProperty(AlfImportFileableDelegate.TYPE_PROPERTY, targetType.getName());

		p.setProperty("dctm:r_object_id", this.cmfObject.getId());

		values.clear();
		values.add(AlfImportFileableDelegate.STATUS_ASPECT);
		if (!isReference()) {
			p.setProperty("dctm:i_chronicle_id", this.cmfObject.getHistoryId());

			// Finally, perform user mappings for special user-relative attributes
			for (String s : AlfImportFileableDelegate.USER_CONVERSIONS) {
				final String src = AlfImportFileableDelegate.ATTRIBUTE_SPECIAL_COPIES.get(s);
				if (src == null) {
					continue;
				}
				String v = this.factory.mapUser(p.getProperty(src));
				if (v == null) {
					continue;
				}
				p.setProperty(s, v);
			}

			// Map the group attribute
			String group = null;
			CmfValue groupValue = getAttributeValue(IntermediateAttribute.GROUP);
			if (groupValue != null) {
				group = this.factory.mapGroup(groupValue.asString());
			}

			p.setProperty("arm:dmAcl", Tools.coalesce(generateAcl(ctx, p.getProperty("cm:owner"), group), ""));

			CmfValue aclInherit = getPropertyValue(IntermediateProperty.ACL_INHERITANCE);
			if (aclInherit != null) {
				p.setProperty("arm:aclInheritance", aclInherit.asString());
			}

			// Not a reference? Add the caliente aspect
			values.add(AlfImportFileableDelegate.CALIENTE_ASPECT);
		}

		for (String s : targetType.getAspects()) {
			if (StringUtils.isEmpty(s)) {
				continue;
			}
			values.add(s);
		}
		String aspectList = StringUtils.join(values, ',');
		p.setProperty(AlfImportFileableDelegate.ASPECT_PROPERTY, aspectList);
		p.setProperty("arm:aspects", aspectList);

		// Now, get the head object
		CmfObject<CmfValue> head = this.cmfObject;
		try {
			head = ctx.getHeadObject(this.cmfObject);
		} catch (CmfStorageException e) {
			this.log.warn(String.format("Failed to load the HEAD object for %s batch [%s]",
				this.cmfObject.getType().name(), this.cmfObject.getHistoryId()), e);
		}
		p.setProperty("cm:name", ctx.getObjectName(head));
	}

	protected final String generateAcl(final AlfImportContext ctx, final String owner, final String group)
		throws ImportException {
		// Make sure that if ACL processing is disabled, we don't process it
		// if (!ctx.isSupported(CmfType.ACL)) { return null; }
		CmfValue aclId = getPropertyValue(IntermediateProperty.ACL_ID);
		if ((aclId == null) || aclId.isNull()) { return null; }

		final StringBuilder ret = new StringBuilder();
		CmfObjectHandler<CmfValue> handler = new DefaultCmfObjectHandler<CmfValue>() {

			@Override
			public boolean handleObject(CmfObject<CmfValue> dataObject) throws CmfStorageException {
				CmfProperty<CmfValue> accessors = dataObject.getAttribute("dctm:r_accessor_name");
				CmfProperty<CmfValue> accessorTypes = dataObject.getAttribute("dctm:r_is_group");
				CmfProperty<CmfValue> permits = dataObject.getAttribute("dctm:r_accessor_permit");
				CmfProperty<CmfValue> permitTypes = dataObject.getAttribute("dctm:r_permit_type");

				final int count = Tools.min(accessors.getValueCount(), accessors.getValueCount(),
					accessorTypes.getValueCount(), permits.getValueCount());

				for (int i = 0; i < count; i++) {
					String accessor = accessors.getValue(i).asString();
					final boolean is_group = accessorTypes.getValue(i).asBoolean();
					final int permit = permits.getValue(i).asInteger();
					final int permitType = permitTypes.getValue(i).asInteger();

					if (permitType != 0) {
						// We can't handle other permit types yet...
						continue;
					}

					if (Tools.equals(accessor, "dm_owner")) {
						accessor = owner;
					} else if (Tools.equals(accessor, "dm_group")) {
						accessor = group;
					}

					if (StringUtils.isEmpty(accessor)) {
						continue;
					}

					final AccessorType type;
					if (is_group) {
						type = AccessorType.GROUP;
						accessor = AlfImportFileableDelegate.this.factory.mapGroup(accessor);
					} else {
						type = AccessorType.USER;
						accessor = AlfImportFileableDelegate.this.factory.mapUser(accessor);
					}

					char permitChar = '?';
					try {
						permitChar = PermitValue.values()[permit - 1].tag;
					} catch (Exception e) {
						// Unknown value, use "?"
						permitChar = '?';
					}

					if (i > 0) {
						ret.append(',');
					}
					ret.append(String.format("%1.1s:%s:%s", type.name().toLowerCase(), accessor, permitChar));
				}

				return false;
			}

		};
		try {
			int count = ctx.loadObjects(CmfType.ACL, Collections.singleton(aclId.asString()), handler);
			if (count > 0) { return ret.toString(); }
		} catch (CmfStorageException e) {
			throw new ImportException(String.format("Failed to load the ACL [%s] associated with %s [%s](%s)",
				aclId.asString(), this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}
		return null;
	}

	protected final void populateRenditionAttributes(Properties p, AlfrescoType targetType, CmfContentInfo content)
		throws ImportException {
		// Set the type property
		p.setProperty(AlfImportFileableDelegate.TYPE_PROPERTY, targetType.getName());
		p.setProperty("cm:name", AlfrescoBaseBulkOrganizationStrategy.generateRenditionName(this.cmfObject, content));
		p.setProperty("arm:renditionObjectId", this.cmfObject.getId());
		p.setProperty("arm:renditionName", content.getRenditionIdentifier());
		p.setProperty("arm:renditionPage", String.valueOf(content.getRenditionPage()));
		p.setProperty("arm:renditionModifier", String.valueOf(content.getModifier()));
		p.setProperty("arm:renditionFormat", content.getMimeType().toString());
	}

	protected final void populateVdocReference(Properties p, String referenceId, String targetName, String targetId,
		String label) throws ImportException {
		// Set the type property
		AlfrescoType type = this.vdocReference;
		p.setProperty(AlfImportFileableDelegate.TYPE_PROPERTY, type.getName());
		Collection<String> aspects = new LinkedHashSet<>();
		for (String aspect : type.getAspects()) {
			aspects.add(aspect);
		}
		aspects.add(AlfImportFileableDelegate.CALIENTE_ASPECT);
		aspects.add(AlfImportFileableDelegate.STATUS_ASPECT);
		p.setProperty(AlfImportFileableDelegate.ASPECT_PROPERTY, StringUtils.join(aspects, ','));
		p.setProperty("arm:aspects", StringUtils.join(aspects, ','));
		p.setProperty("arm:aclInheritance", "NONE[]");
		p.setProperty("arm:vdocReferenceId", referenceId);
		p.setProperty("cm:name", targetName);
		p.setProperty("dctm:binding_condition", "VERSION_LABEL");
		p.setProperty("dctm:reference_by_id", targetId);
		p.setProperty("dctm:binding_label", StringUtils.isEmpty(label) ? "CURRENT" : label);
	}

	protected final File generateMetadataFile(final AlfImportContext ctx, final Properties p, final File main)
		throws ImportException {
		String mainName = main.getName();
		final String suffix = AlfImportDelegateFactory.parseVersionSuffix(mainName);
		mainName = mainName.substring(0, mainName.length() - suffix.length());
		final File meta = new File(main.getParentFile(),
			String.format("%s%s%s", mainName, AlfImportFileableDelegate.METADATA_SUFFIX, suffix));

		final OutputStream out;
		try {
			out = new FileOutputStream(meta);
		} catch (FileNotFoundException e) {
			throw new ImportException(
				String.format("Failed to open the properties file at [%s]", main.getAbsolutePath()), e);
		}
		try {
			XmlProperties.saveToXML(p, out,
				String.format("Properties for [%s](%s)", this.cmfObject.getLabel(), this.cmfObject.getId()));
		} catch (IOException e) {
			meta.delete();
			throw new ImportException(
				String.format("Failed to write to the properties file at [%s]", main.getAbsolutePath()), e);
		} finally {
			IOUtils.closeQuietly(out);
		}
		return meta;
	}

	@Override
	protected final Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator,
		AlfImportContext ctx) throws ImportException, CmfStorageException {

		if (!ctx.getContentStore()
			.isSupportsFileAccess()) { throw new ImportException("This engine requires filesystem access"); }

		String path = null;
		CmfValue pathProp = getPropertyValue(IntermediateProperty.LATEST_PARENT_TREE_IDS);
		if ((pathProp == null) || pathProp.isNull()) {
			pathProp = getPropertyValue(IntermediateProperty.PARENT_TREE_IDS);
		}
		if (pathProp == null) { throw new ImportException(String.format(
			"Failed to find the required property [%s] in %s [%s](%s)", IntermediateProperty.PARENT_TREE_IDS.encode(),
			this.cmfObject.getSubtype(), this.cmfObject.getLabel(), this.cmfObject.getId())); }

		String prefix = (!pathProp.isNull() ? pathProp.asString() : "");
		path = String.format("%s%s%s", prefix, StringUtils.isEmpty(prefix) ? "" : "/", this.cmfObject.getId());

		// Step 1: copy over all the attributes that need copying over, un-mapping them as needed
		Collection<CmfContentInfo> contents = ctx.getContentInfo(this.cmfObject);
		if (contents.isEmpty()) {
			// No content streams, so make one up so we can build the properties file
			contents = Collections.singleton(new CmfContentInfo());
		}

		boolean vdocRootIndexed = false;
		Set<String> vdocVersionsIndexed = new HashSet<>();
		boolean renditionsRootIndexed = false;
		Set<String> renditionTypesIndexed = new HashSet<>();
		for (CmfContentInfo content : contents) {
			CmfContentStore<?, ?, ?>.Handle h = ctx.getContentStore().getHandle(this.factory.getTranslator(),
				this.cmfObject, content);

			// First things first: identify the type we're going to store into
			AlfrescoType targetType = getTargetType(content);

			final File main;
			try {
				main = h.getFile();
			} catch (IOException e) {
				throw new ImportException("Failure!", e);
			}

			if (!main.exists()) {
				ctx.printf("Creating a stub for %s [%s](%s)", this.cmfObject.getType(), this.cmfObject.getLabel(),
					this.cmfObject.getId());
				if (!createStub(ctx, main,
					this.cmfObject.getLabel())) { return Collections.singleton(ImportOutcome.SKIPPED); }
			}

			// Ok...so...now that we know where the metadata properties must go, we write them
			// out
			Properties p = new Properties();
			final boolean primaryRendition = content.isDefaultRendition() && (content.getRenditionPage() == 0);
			final MarkerType markerType = (primaryRendition ? MarkerType.NORMAL : MarkerType.RENDITION_ENTRY);

			if (this.virtual && !primaryRendition) {
				// This is a VDoc rendition...and is currently not supported
				this.log.warn("VDoc renditions aren't yet supported for [{}]({})", this.cmfObject.getLabel(),
					this.cmfObject.getId());
				continue;
			}

			if (primaryRendition) {
				// First page of the default rendition gets ALL the metadata. Everything
				// else only gets supplementary metadata
				populatePrimaryAttributes(ctx, p, targetType, content);
			} else {
				// This is a supplementary rendition, and thus will need some minimal
				// metadata set on it
				populateRenditionAttributes(p, targetType, content);
				if (!renditionsRootIndexed) {
					this.factory.storeToIndex(ctx, this.cmfObject, main, null, MarkerType.RENDITION_ROOT);
					renditionsRootIndexed = true;
				}
				if (renditionTypesIndexed.add(content.getRenditionIdentifier())) {
					this.factory.storeToIndex(ctx, this.cmfObject, main, null, MarkerType.RENDITION_TYPE);
				}
			}

			final File meta = generateMetadataFile(ctx, p, main);
			if (this.virtual) {
				File vdocVersion = meta.getParentFile();
				if (vdocVersionsIndexed.add(vdocVersion.getName())) {
					// Does the reference home already have properties? If not, then add them...
					Properties versionProps = new Properties();
					populatePrimaryAttributes(ctx, versionProps, this.vdocRoot, content);

					if (this.cmfObject.isHistoryCurrent() && !vdocRootIndexed) {
						final File vdocRootMeta = generateMetadataFile(ctx, versionProps, vdocVersion.getParentFile());
						this.factory.storeToIndex(ctx, this.cmfObject, vdocVersion.getParentFile(), vdocRootMeta,
							MarkerType.VDOC_ROOT);
						vdocRootIndexed = true;
					}

					versionProps.setProperty("cm:name", vdocVersion.getName());
					versionProps.setProperty("dctm:object_name", vdocVersion.getName());
					versionProps.setProperty(AlfImportFileableDelegate.TYPE_PROPERTY, this.vdocVersion.getName());
					Set<String> aspects = new LinkedHashSet<>(this.vdocVersion.getAspects());
					aspects.add(AlfImportFileableDelegate.STATUS_ASPECT);
					versionProps.setProperty(AlfImportFileableDelegate.ASPECT_PROPERTY, StringUtils.join(aspects, ','));
					final File vdocVersionMeta = generateMetadataFile(ctx, versionProps, vdocVersion);
					this.factory.storeToIndex(ctx, this.cmfObject, vdocVersion, vdocVersionMeta,
						MarkerType.VDOC_VERSION);
				}
				this.factory.storeToIndex(ctx, this.cmfObject, main, meta,
					(primaryRendition ? MarkerType.VDOC_STREAM : MarkerType.VDOC_RENDITION));

				CmfProperty<CmfValue> members = this.cmfObject.getProperty(IntermediateProperty.VDOC_MEMBER);
				if (members != null) {
					for (CmfValue member : members) {
						if (member.isNull()) {
							continue;
						}
						Matcher matcher = AlfImportFileableDelegate.VDOC_MEMBER_PARSER.matcher(member.asString());
						if (!matcher.matches()) {
							this.log.warn("Incomplete VDoc member data for [%s](%s) - [%s]", this.cmfObject.getLabel(),
								this.cmfObject.getId(), member.asString());
							continue;
						}

						String[] memberData = matcher.group(1).split("\\|");
						if (memberData.length < 5) {
							this.log.warn("Incomplete VDoc member reference data for [%s](%s) - [%s]",
								this.cmfObject.getLabel(), this.cmfObject.getId(), member.asString());
							continue;
						}

						Properties vdocMemberProperties = new Properties();
						populateVdocReference(vdocMemberProperties, memberData[0], memberData[0], memberData[1],
							memberData[2]);

						File vdocMember = new File(vdocVersion, memberData[0]);
						createStub(ctx, vdocMember, member.asString());
						File vdocMemberMeta = generateMetadataFile(ctx, vdocMemberProperties, vdocMember);
						this.factory.storeToIndex(ctx, this.cmfObject, vdocMember, vdocMemberMeta,
							MarkerType.VDOC_REFERENCE);
					}
				}
			} else {
				this.factory.storeToIndex(ctx, this.cmfObject, main, meta, markerType);

				// IF (and only if) the document is also the head document, but not the latest
				// version (i.e. mid-tree "CURRENT", we need to copy everything over to a "new"
				// location with no version number - including the properties.
				String mainName = main.getName();
				final String suffix = AlfImportDelegateFactory.parseVersionSuffix(mainName);
				if (this.cmfObject.isHistoryCurrent() && !StringUtils.isEmpty(suffix)) {
					final String versionTag = String.format("\\Q%s\\E$", suffix);
					File newMain = new File(main.getAbsolutePath().replaceAll(versionTag, ""));
					File newMeta = new File(meta.getAbsolutePath().replaceAll(versionTag, ""));
					boolean ok = false;
					try {
						FileUtils.copyFile(main, newMain);
						FileUtils.copyFile(meta, newMeta);
						ok = true;
					} catch (IOException e) {
						throw new ImportException(
							String.format("Failed to create a copy of the HEAD version for [%s](%s) from [%s] to [%s]",
								this.cmfObject.getLabel(), this.cmfObject.getId(), main.getAbsolutePath(),
								newMain.getAbsolutePath()),
							e);
					} finally {
						if (!ok) {
							newMain.delete();
							newMeta.delete();
						}
					}
				}
			}
		}

		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), path));
	}
}