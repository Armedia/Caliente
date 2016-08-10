package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.cmf.engine.alfresco.bulk.importer.model.SchemaAttribute;
import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.Tools;

public class AlfDocumentImportDelegate extends AlfImportDelegate {

	private static final Map<String, String> UNMAPPER;
	private static final Map<String, String> MAPPER;
	private static final Map<String, String> COPIER;

	static {
		Map<String, String> m = new HashMap<String, String>();
		Map<String, String> M = new HashMap<String, String>();

		String[][] unmappings = {
			{
				"cmf:acl_name", "dctm:acl_name"
			}, {
				"cmf:group", "dctm:group_name"
			}, {
				"cmf:group_permission", "dctm:group_permit"
			}, {
				"cmf:last_access_date", "dctm:r_access_date"
			}, {
				"cmf:login_realm", "dctm:acl_domain"
			}, {
				"cmf:owner", "dctm:owner_name"
			}, {
				"cmf:owner_permission", "dctm:owner_permit"
			}, {
				"cmf:version_antecedent_id", "dctm:i_antecedent_id"
			}, {
				"cmis:changeToken", "dctm:i_vstamp"
			}, {
				"cmis:checkinComment", "dctm:log_entry"
			}, {
				"cmis:contentStreamLength", "dctm:r_full_content_size"
			}, {
				"cmis:contentStreamMimeType", "dctm:a_content_type"
			}, {
				"cmis:createdBy", "dctm:r_creator_name"
			}, {
				"cmis:creationDate", "dctm:r_creation_date"
			}, {
				"cmis:description", "dctm:title"
			}, {
				"cmis:isImmutable", "dctm:r_immutable_flag"
			}, {
				"cmis:isLatestVersion", "dctm:i_has_folder"
			}, {
				"cmis:lastModificationDate", "dctm:r_modify_date"
			}, {
				"cmis:lastModifiedBy", "dctm:r_modifier"
			}, {
				"cmis:name", "dctm:object_name"
			}, {
				"cmis:objectTypeId", "dctm:r_object_type"
			}, {
				"cmis:parentId", "dctm:i_folder_id"
			}, {
				"cmis:versionLabel", "dctm:r_version_label"
			}, {
				"cmis:versionSeriesCheckedOutBy", "dctm:r_lock_owner"
			}, {
				"cmis:versionSeriesId", "dctm:i_chronicle_id"
			}, {
				"cmis:path", "dctm:r_folder_path"
			},
		};

		for (String[] s : unmappings) {
			m.put(s[0], s[1]);
			M.put(s[1], s[0]);
		}

		UNMAPPER = Tools.freezeMap(m);
		MAPPER = Tools.freezeMap(M);

		m = new HashMap<String, String>();
		String[][] mappings = {
			{
				"dctm:authors", "cm:author"
			}, {
				"dctm:log_entry", "cm:description"
			}, {
				"dctm:message_subject", "cm:subjectline"
			}, {
				"dctm:object_name", "cm:name"
			}, {
				"dctm:owner_name", "cm:owner"
			}, {
				"dctm:r_access_date", "cm:accessed"
			}, {
				"dctm:r_creation_date", "cm:created"
			}, {
				"dctm:r_creator_name", "cm:creator"
			}, {
				"dctm:r_modifier", "cm:modifier"
			}, {
				"dctm:r_modify_date", "cm:modified"
			}, {
				"dctm:sent_date", "cm:sentdate"
			}, {
				"dctm:title", "cm:title"
			},
		};
		for (String[] s : mappings) {
			m.put(s[0], s[1]);
		}

		COPIER = Tools.freezeMap(m);
	}

	private static final Pattern SUFFIX = Pattern.compile("^.*(\\.v\\d+(?:\\.\\d+)?)$");

	private final boolean head;

	public AlfDocumentImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
		CmfAttribute<CmfValue> isLatestVersionAtt = storedObject.getAttribute(IntermediateAttribute.IS_LATEST_VERSION);
		this.head = ((isLatestVersionAtt != null) && isLatestVersionAtt.hasValues()
			&& isLatestVersionAtt.getValue().asBoolean());
	}

	protected AlfrescoType getTargetType(CmfContentInfo content) throws ImportException {
		AlfrescoType type = null;
		if (!content.isDefaultRendition() || (content.getRenditionPage() > 0)) {
			// If this is a rendition or rendition extra page...
			type = this.factory.getType("jsap:rendition");
		} else {
			// Not a rendition? Fine...let's identify the type
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

	protected SchemaAttribute unmapAttribute(AlfrescoType targetType, String attributeName) throws ImportException {
		String target = AlfDocumentImportDelegate.UNMAPPER.get(attributeName);
		if (target == null) { return null; }
		return targetType.getAttribute(target);
	}

	protected void populatePrimaryAttributes(Properties p, AlfrescoType targetType, CmfContentInfo content)
		throws ImportException {

		for (String s : this.cmfObject.getAttributeNames()) {
			CmfAttribute<CmfValue> srcAtt = this.cmfObject.getAttribute(s);

			// Easy mode: direct mapping
			SchemaAttribute tgtAtt = targetType.getAttribute(srcAtt.getName());
			if (tgtAtt == null) {
				// No direct mapping? Try an un-mapping
				tgtAtt = unmapAttribute(targetType, srcAtt.getName());
				if (tgtAtt == null) {
					// This is either a default documentum attribute, or a JSAP attribute...
					int colon = srcAtt.getName().indexOf(':');
					if (colon >= 0) {
						String rest = srcAtt.getName().substring(colon + 1);
						tgtAtt = targetType.getAttribute(String.format("jsap:%s", rest));
					}
				}
			}

			if (tgtAtt == null) {
				// ERROR! No mapping!
				if (this.log.isDebugEnabled()) {
					this.log.warn(String.format(
						"Failed to find a mapping for attribute [%s] from source type [%s] into target type [%s] with aspects %s",
						s, this.cmfObject.getSubtype(), targetType.getName(), targetType.getAspects()));
				}
				continue;
			}

			final String value;
			// If the source attribute is repeating, but the target isn't, we'll concatenate
			if (!srcAtt.hasValues()) {
				continue;
			}

			if (srcAtt.isRepeating()) {
				// Concatenate using the separator
				StringBuilder sb = new StringBuilder();
				char separator = ',';
				// TODO: Detect separator - if it exists, try another
				int i = 0;
				for (CmfValue v : srcAtt) {
					if (i > 0) {
						sb.append(separator);
					}
					sb.append(v.asString());
					i++;
				}
				value = sb.toString();
			} else {
				// Write out the single value
				value = srcAtt.getValue().asString();
			}

			if (!StringUtils.isEmpty(value)) {
				p.setProperty(tgtAtt.name, value);
			}
		}

		// Now, do the mappings as copies of what has already been copied over, except when the
		// target attribute isn't repeating and the source is. In that case, only copy the first
		// value over
		for (String m : AlfDocumentImportDelegate.COPIER.keySet()) {
			String v = p.getProperty(m);
			if (StringUtils.isEmpty(v)) {
				continue;
			}

			String newName = AlfDocumentImportDelegate.COPIER.get(m);
			String srcName = AlfDocumentImportDelegate.MAPPER.get(m);
			if (srcName == null) {
				continue;
			}

			CmfAttribute<CmfValue> srcAtt = this.cmfObject.getAttribute(srcName);
			if ((srcAtt == null) || !srcAtt.hasValues()) {
				continue;
			}

			SchemaAttribute tgtAtt = targetType.getAttribute(newName);
			if (tgtAtt == null) {
				targetType.getAttribute(newName);
				continue;
			}

			if (srcAtt.isRepeating() && !tgtAtt.multiple) {
				v = srcAtt.getValue(0).asString();
			}

			p.put(newName, v);
		}

		// Now handle the special properties
		CmfProperty<CmfValue> prop = this.cmfObject.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (CmfValue v : prop) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(v.asString());
			i++;
		}
		p.setProperty("dctm:r_parent_path_ids", sb.toString());

		// Set the type property
		p.setProperty("type", targetType.getName());

		// Set the aspects
		// TODO: Identify the target aspects
		// cm:ownable, cm:auditable, cm:author, cm:titled
		sb.setLength(0);
		sb.append("arm:caliente");
		for (String s : targetType.getAspects()) {
			sb.append(',').append(s);
		}
		p.setProperty("aspects", sb.toString());
		p.setProperty("arm:aspects", sb.toString());

		// TODO: Generate the ACL attribute

		p.setProperty("dctm:r_object_id", this.cmfObject.getId());
		p.setProperty("dctm:i_chronicle_id", this.cmfObject.getBatchId());
		// p.setProperty("cmis:isLatestVersion", String.valueOf(this.head));
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
		for (CmfContentInfo content : ctx.getContentInfo(this.cmfObject)) {
			CmfContentStore<?, ?, ?>.Handle h = ctx.getContentStore().getHandle(this.factory.getTranslator(),
				this.cmfObject, content);

			// First things first: identify the type we're going to store into
			AlfrescoType targetType = getTargetType(content);

			File main;
			try {
				main = h.getFile();
			} catch (IOException e) {
				throw new ImportException("Failure!");
			}

			String mainName = main.getName();
			final Matcher m = AlfDocumentImportDelegate.SUFFIX.matcher(mainName);
			final String suffix = (m.matches() ? m.group(1) : "");

			mainName = mainName.substring(0, mainName.length() - suffix.length());
			final File meta = new File(main.getParentFile(),
				String.format("%s.metadata.properties.xml%s", mainName, suffix));

			// Ok...so...now that we know where the metadata properties must go, we write them out
			Properties p = new Properties();

			if (content.isDefaultRendition() && (content.getRenditionPage() == 0)) {
				// First page of the default rendition gets ALL the metadata. Everything else
				// only gets supplementary metadata
				populatePrimaryAttributes(p, targetType, content);
			} else {
				// This is a supplementary rendition, and thus will need some minimal
				// metadata set on it
				populateRenditionAttributes(p, targetType, content);
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