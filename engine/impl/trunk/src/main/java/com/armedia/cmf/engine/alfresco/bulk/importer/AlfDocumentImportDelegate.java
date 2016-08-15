package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.cmf.engine.alfresco.bulk.importer.model.SchemaAttribute;
import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.engine.tools.AclTools.AccessorType;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectHandler;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueSerializer;
import com.armedia.cmf.storage.tools.DefaultCmfObjectHandler;
import com.armedia.commons.utilities.Tools;

public class AlfDocumentImportDelegate extends AlfImportDelegate {

	private static final Map<String, String> ATTRIBUTE_MAPPER;
	private static final Map<String, String> ATTRIBUTE_SPECIAL_COPIES;
	private static final Set<String> USER_CONVERSIONS;

	private static enum PermitValue {
		//
		NONE, BROWSE, READ, RELATE, VERSION, WRITE, DELETE,
		//
		;
	}

	static {
		Map<String, String> m = new HashMap<String, String>();

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

		m = new HashMap<String, String>();
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

		m = new HashMap<String, String>();
		String[] conversions = {
			"cm:owner", "cm:creator", "cm:modifier"
		};
		for (String s : conversions) {
			m.put(s, s);
		}
		USER_CONVERSIONS = Tools.freezeSet(new HashSet<String>(m.keySet()));
	}

	private static final Pattern SUFFIX = Pattern.compile("^.*(\\.v\\d+(?:\\.\\d+)?)$");

	private final boolean reference;

	public AlfDocumentImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
		CmfAttribute<CmfValue> att = this.cmfObject.getAttribute("dctm:i_is_reference");
		this.reference = ((att != null) && att.hasValues() && att.getValue().asBoolean());
		if (this.reference) {
			AlfDocumentImportDelegate.USER_CONVERSIONS.size();
		}
	}

	protected final boolean isReference() {
		return this.reference;
	}

	protected AlfrescoType getTargetType(CmfContentInfo content) throws ImportException {
		AlfrescoType type = null;
		if (isReference()) {
			// If this is a reference - folder or document, doesn't matter...
			type = this.factory.getType("jsap:reference");
		} else if (!content.isDefaultRendition() || (content.getRenditionPage() > 0)) {
			// If this is a rendition or rendition extra page...
			type = this.factory.getType("jsap:rendition");
		} else {
			// Not a rendition or a reference? Fine...let's identify the type
			String srcTypeName = this.cmfObject.getSubtype().toLowerCase();
			String finalTypeName = String.format("jsap:%s", srcTypeName);
			if (this.factory.schema.hasType(finalTypeName)) {
				type = this.factory.schema.buildType(finalTypeName);
			} else {
				// Not an identifiable type? Let's go with the base archetype
				switch (this.cmfObject.getType()) {
					case DOCUMENT:
						type = this.factory.getType("jsap:document");
						break;
					case FOLDER:
						type = this.factory.getType("jsap:folder");
						break;
					default:
						break;
				}
			}
		}

		// No match? Error!
		if (type == null) { throw new ImportException(String.format(
			"Failed to find a proper type mapping for %s of type [%s] (%s rendition, page #%d)",
			this.cmfObject.getType(), this.cmfObject.getSubtype(),
			content.isDefaultRendition() ? "default" : content.getRenditionIdentifier(), content.getRenditionPage())); }

		return type;
	}

	protected CmfAttribute<CmfValue> mapAttribute(String attributeName) {
		String target = AlfDocumentImportDelegate.ATTRIBUTE_MAPPER.get(attributeName);
		if (target == null) { return null; }
		return this.cmfObject.getAttribute(target);
	}

	protected void storeValue(CmfProperty<CmfValue> srcAtt, SchemaAttribute tgtAtt, Properties p,
		boolean concatenateFallback) throws ImportException, ParseException {
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

		if (!StringUtils.isEmpty(value)) {
			p.setProperty(tgtAtt.name, value);
		}
	}

	protected void populatePrimaryAttributes(AlfImportContext ctx, Properties p, AlfrescoType targetType,
		CmfContentInfo content) throws ImportException, ParseException {

		Set<String> tgtNames = targetType.getAttributeNames();
		Set<String> srcNames = this.cmfObject.getAttributeNames();

		Set<String> common = new HashSet<String>();
		common.addAll(tgtNames);
		common.retainAll(srcNames);
		for (String s : common) {
			final CmfProperty<CmfValue> src = this.cmfObject.getAttribute(s);
			final SchemaAttribute tgt = targetType.getAttribute(s);
			storeValue(src, tgt, p, true);
		}

		Set<String> tgtOnly = new HashSet<String>();
		tgtOnly.addAll(tgtNames);
		tgtOnly.removeAll(srcNames);
		for (final String tgtAttName : tgtOnly) {
			final SchemaAttribute tgtAtt = targetType.getAttribute(tgtAttName);

			// Easy path: is this already mapped?
			String srcAttName = AlfDocumentImportDelegate.ATTRIBUTE_MAPPER.get(tgtAttName);
			if (srcAttName == null) {
				// This is because the source attributes all come with either "dctm:", "cmf:", or
				// "cmis:" as a prefix, so we first try the happy path
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
			storeValue(srcAtt, tgtAtt, p, true);
		}

		// Now, do the mappings as copies of what has already been copied over, except when the
		// source attribute is repeating.
		for (String m : AlfDocumentImportDelegate.ATTRIBUTE_SPECIAL_COPIES.keySet()) {
			final String tgtName = AlfDocumentImportDelegate.ATTRIBUTE_SPECIAL_COPIES.get(m);
			String srcName = AlfDocumentImportDelegate.ATTRIBUTE_MAPPER.get(tgtName);
			if (srcName == null) {
				// If it didn't need mapping, stick to the original name.
				srcName = tgtName;
			}

			SchemaAttribute tgtAtt = targetType.getAttribute(tgtName);
			if (tgtAtt == null) {
				continue;
			}

			CmfAttribute<CmfValue> srcAtt = this.cmfObject.getAttribute(srcName);
			if ((srcAtt == null) || !srcAtt.hasValues()) {
				continue;
			}

			storeValue(srcAtt, tgtAtt, p, false);
		}

		// Now handle the special properties
		StringBuilder sb = new StringBuilder();
		{
			CmfProperty<CmfValue> prop = this.cmfObject.getProperty(IntermediateProperty.PARENT_TREE_IDS);
			int i = 0;
			for (CmfValue v : prop) {
				if (i > 0) {
					sb.append(',');
				}
				sb.append(v.asString());
				i++;
			}
			p.setProperty("dctm:r_parent_path_ids", sb.toString());
		}

		// Set the type property
		p.setProperty("type", targetType.getName());

		p.setProperty("dctm:r_object_id", this.cmfObject.getId());

		sb.setLength(0);

		if (!isReference()) {
			p.setProperty("dctm:i_chronicle_id", this.cmfObject.getBatchId());

			// Perform user mappings
			for (String s : AlfDocumentImportDelegate.USER_CONVERSIONS) {
				String v = ctx.mapUser(p.getProperty(s));
				if (v == null) {
					continue;
				}
				p.setProperty(s, v);
			}

			// Map the group attribute
			String group = null;
			CmfProperty<CmfValue> prop = this.cmfObject.getAttribute(IntermediateAttribute.GROUP);
			if ((prop != null) && prop.hasValues()) {
				group = ctx.mapGroup(prop.getValue().asString());
			}

			p.setProperty("arm:dmAcl", Tools.coalesce(generateAcl(ctx, p.getProperty("cm:owner"), group), ""));

			CmfProperty<CmfValue> aclInherit = this.cmfObject.getProperty(IntermediateProperty.ACL_INHERITANCE);
			if (aclInherit != null) {
				p.setProperty("arm:aclInheritance", aclInherit.getValue().asString());
			}

			// Not a reference? Add the caliente aspect
			sb.append("arm:caliente");
		}

		for (String s : targetType.getAspects()) {
			if (StringUtils.isEmpty(s)) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(s);
		}
		p.setProperty("aspects", sb.toString());
		p.setProperty("arm:aspects", sb.toString());

		p.setProperty("cm:name", this.cmfObject.getName());
	}

	protected String generateAcl(final AlfImportContext ctx, final String owner, final String group)
		throws ImportException {
		// TODO: This is hardcoded to Documentum-generated attributes, since we don't yet have
		// a universal means of expressing ACLs

		// Make sure that if ACL processing is disabled, we don't process it
		// if (!ctx.isSupported(CmfType.ACL)) { return null; }
		CmfProperty<CmfValue> aclIdAtt = this.cmfObject.getProperty(IntermediateProperty.ACL_ID);
		if ((aclIdAtt == null) || !aclIdAtt.hasValues()) { return null; }
		CmfValue aclId = aclIdAtt.getValue();
		if ((aclId == null) || aclId.isNull()) { return null; }

		final StringBuilder ret = new StringBuilder();
		CmfObjectHandler<CmfValue> handler = new DefaultCmfObjectHandler<CmfValue>() {

			@Override
			public boolean handleObject(CmfObject<CmfValue> dataObject) throws CmfStorageException {
				CmfProperty<CmfValue> accessors = dataObject.getProperty("accessors");
				CmfProperty<CmfValue> accessorTypes = dataObject.getProperty("accessorTypes");
				CmfProperty<CmfValue> permitTypes = dataObject.getProperty("permitTypes");
				CmfProperty<CmfValue> permitValues = dataObject.getProperty("permitValues");

				final int count = Tools.min(accessors.getValueCount(), accessorTypes.getValueCount(),
					permitTypes.getValueCount(), permitValues.getValueCount());

				for (int i = 0; i < count; i++) {
					String accessor = accessors.getValue(i).asString();
					String accessorType = accessorTypes.getValue(i).asString();
					int permitType = permitTypes.getValue(i).asInteger();
					String permitValue = permitValues.getValue(i).asString();

					if (permitType != 0) {
						// We only support DF_ACCESS_PERMIT
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
					if (accessorType.equals("user")) {
						type = AccessorType.USER;
						accessor = ctx.mapUser(accessor);
					} else if (accessorType.indexOf("role") < 0) {
						type = AccessorType.ROLE;
						accessor = ctx.mapRole(accessor);
					} else {
						type = AccessorType.GROUP;
						accessor = ctx.mapGroup(accessor);
					}

					String permit = "?";
					try {
						permit = PermitValue.valueOf(permitValue.toUpperCase()).name();
					} catch (Exception e) {
						// Unknown value, use "?"
						permit = "?";
					}

					if (i > 0) {
						ret.append(',');
					}
					ret.append(String.format("%1.1s:%s:%1.1s", type.name().toLowerCase(), accessor, permit));
				}

				return false;
			}

		};
		try {
			int count = ctx.loadObjects(CmfType.ACL, Collections.singleton(aclId.asString()), handler);
			if (count > 0) { return ret.toString(); }
		} catch (CmfStorageException e) {
			throw new ImportException(String.format("Failed to load the ACL [%s] associated with %s [%s](%s)", aclIdAtt,
				this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}
		return null;
	}

	protected void populateRenditionAttributes(Properties p, AlfrescoType targetType, CmfContentInfo content)
		throws ImportException {
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, AlfImportContext ctx)
		throws ImportException, CmfStorageException {

		if (!ctx.getContentStore()
			.isSupportsFileAccess()) { throw new ImportException("This engine requires filesystem access"); }

		String path = null;
		CmfProperty<CmfValue> pathProp = this.cmfObject.getProperty(IntermediateProperty.JSAP_PARENT_TREE_IDS);
		if (pathProp == null) {
			pathProp = this.cmfObject.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		}
		if (pathProp == null) { throw new ImportException(String.format(
			"Failed to find the required property [%s] in %s [%s](%s)", IntermediateProperty.PARENT_TREE_IDS.encode(),
			this.cmfObject.getSubtype(), this.cmfObject.getLabel(), this.cmfObject.getId())); }

		String prefix = (pathProp.hasValues() ? pathProp.getValue().asString() : "");
		path = String.format("%s%s%s", prefix, StringUtils.isEmpty(prefix) ? "" : "/", this.cmfObject.getId());

		// Step 1: copy over all the attributes that need copying over, un-mapping them as needed
		Collection<CmfContentInfo> contents = ctx.getContentInfo(this.cmfObject);
		if (contents.isEmpty()) {
			// No content streams, so make one up so we can build the properties file
			if (isReference()) {
				contents.size();
			}
			contents = Collections.singleton(new CmfContentInfo());
		}
		for (CmfContentInfo content : contents) {
			CmfContentStore<?, ?, ?>.Handle h = ctx.getContentStore().getHandle(this.factory.getTranslator(),
				this.cmfObject, content);

			// First things first: identify the type we're going to store into
			AlfrescoType targetType = getTargetType(content);

			File main;
			try {
				main = h.getFile();
			} catch (IOException e) {
				throw new ImportException("Failure!", e);
			}

			if (!main.exists()) {
				try {
					FileUtils.write(main, this.cmfObject.getLabel());
				} catch (IOException e) {
					throw new ImportException("Failure!", e);
				}
			}

			String mainName = main.getName();
			final Matcher m = AlfDocumentImportDelegate.SUFFIX.matcher(mainName);
			final String suffix = (m.matches() ? m.group(1) : "");

			mainName = mainName.substring(0, mainName.length() - suffix.length());
			final File meta = new File(main.getParentFile(),
				String.format("%s.metadata.properties.xml%s", mainName, suffix));

			// Ok...so...now that we know where the metadata properties must go, we write them
			// out
			Properties p = new Properties();
			boolean primary = false;
			try {
				if (content.isDefaultRendition() && (content.getRenditionPage() == 0)) {
					// First page of the default rendition gets ALL the metadata. Everything
					// else
					// only gets supplementary metadata
					primary = true;
					populatePrimaryAttributes(ctx, p, targetType, content);
				} else {
					// This is a supplementary rendition, and thus will need some minimal
					// metadata set on it
					populateRenditionAttributes(p, targetType, content);
				}
			} catch (ParseException e) {
				String renditionSpec = "primary rendition";
				if (!primary) {
					renditionSpec = String.format("rendition [%s], page # %d", content.getRenditionIdentifier(),
						content.getRenditionPage());
				}
				throw new ImportException(String.format("Failed to serialize the attributes for %s [%s](%s), %s",
					this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId(), renditionSpec), e);
			}

			final OutputStream out;
			try {
				out = new FileOutputStream(meta);
			} catch (FileNotFoundException e) {
				throw new ImportException("Failed to open properties file", e);
			}
			try {
				p.storeToXML(out, null);
			} catch (IOException e) {
				meta.delete();
				throw new ImportException("Failed to write properties", e);
			} finally {
				IOUtils.closeQuietly(out);
			}
		}

		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), path));
	}
}