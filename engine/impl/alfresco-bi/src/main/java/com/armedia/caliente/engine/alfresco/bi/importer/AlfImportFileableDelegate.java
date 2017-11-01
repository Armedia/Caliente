package com.armedia.caliente.engine.alfresco.bi.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.alfresco.bi.AlfAttributeMapper;
import com.armedia.caliente.engine.alfresco.bi.AlfrescoBaseBulkOrganizationStrategy;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.index.ScanIndexItemMarker.MarkerType;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoType;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaAttribute;
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
	public static final String STATUS_ASPECT = "arm:calienteStatus";
	public static final String CALIENTE_ASPECT = "arm:caliente";

	private static final String TYPE_PROPERTY = "type";
	private static final String ASPECT_PROPERTY = "aspects";

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
		Set<String> set = new TreeSet<>();
		String[] conversions = {
			"cm:owner", "cm:creator", "cm:modifier"
		};
		for (String s : conversions) {
			set.add(s);
		}
		USER_CONVERSIONS = Tools.freezeSet(new LinkedHashSet<>(set));
	}

	private final boolean reference;
	private final boolean virtual;
	private final AlfrescoType defaultType;
	private final AlfrescoType referenceType;

	private final Map<String, Set<String>> strippedAttributes;

	private volatile AlfrescoType vdocRoot = null;
	private volatile AlfrescoType vdocVersion = null;
	private volatile AlfrescoType vdocReference = null;

	public AlfImportFileableDelegate(String defaultType, AlfImportDelegateFactory factory,
		CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, storedObject);
		CmfValue reference = getPropertyValue(IntermediateProperty.IS_REFERENCE);
		this.reference = ((reference != null) && !reference.isNull() && reference.asBoolean());
		CmfValue virtual = getPropertyValue(IntermediateProperty.VDOC_HISTORY);
		this.virtual = ((virtual != null) && !virtual.isNull() && virtual.asBoolean());
		this.defaultType = this.factory.getType(defaultType);
		this.referenceType = this.factory.getType(AlfImportFileableDelegate.REFERENCE_TYPE);

		Map<String, Set<String>> strippedAttributes = new TreeMap<>();
		for (String att : storedObject.getAttributeNames()) {
			String stripped = AlfrescoType.stripNamespace(att);
			Set<String> s = strippedAttributes.get(stripped);
			if (s == null) {
				s = new TreeSet<>();
				strippedAttributes.put(stripped, s);
			}
			s.add(att);
		}

		for (String att : new LinkedHashSet<>(strippedAttributes.keySet())) {
			Set<String> s = strippedAttributes.get(att);
			s = Tools.freezeSet(s);
			strippedAttributes.put(att, s);
		}

		this.strippedAttributes = Tools.freezeMap(new LinkedHashMap<>(strippedAttributes));
	}

	protected final boolean isVirtual() {
		return this.virtual;
	}

	protected final boolean isReference() {
		return this.reference;
	}

	protected AlfrescoType calculateTargetType(CmfContentInfo content) throws ImportException {
		Set<String> allAspects = this.factory.schema.getAspectNames();
		List<String> badAspects = new ArrayList<>();
		List<String> goodAspects = new ArrayList<>(allAspects.size());
		for (String s : this.cmfObject.getSecondarySubtypes()) {
			(allAspects.contains(s) ? goodAspects : badAspects).add(s);
		}
		if (!badAspects.isEmpty()) {
			this.log.warn("Aspects {} not found while importing {}", badAspects, this.cmfObject.getDescription());
		}
		AlfrescoType type = this.factory.schema.buildType(this.cmfObject.getSubtype(), goodAspects);
		if (type != null) { return type; }
		if (this.defaultType != null) { return this.defaultType; }

		// If we have neither the type, nor a default, this is a problem
		throw new ImportException(String.format("Failed to locate the Alfresco type [%s] for %s",
			this.cmfObject.getSubtype(), this.cmfObject.getDescription()));
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

	protected final void storeValue(AlfImportContext ctx, CmfProperty<CmfValue> srcAtt, String name, boolean multiple,
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
				if (multiple || concatenateFallback) {
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
			p.setProperty(name, value);
		}
	}

	protected final void storeValue(AlfImportContext ctx, CmfProperty<CmfValue> srcAtt, SchemaAttribute tgtAtt,
		Properties p, boolean concatenateFallback) throws ImportException {
		storeValue(ctx, srcAtt, tgtAtt.name, tgtAtt.multiple, p, concatenateFallback);
	}

	protected abstract boolean createStub(AlfImportContext ctx, File target, String content) throws ImportException;

	@SuppressWarnings("unused")
	private String getSourceAttribute(String targetAttributeName, AlfrescoType targetType) {

		// First things first: Is there an explicit mapping configured?
		// If there is, then this supersedes everything else...
		// TODO: Implement this mapping...

		// No explicit mapping, so try to do it automatically...

		// If we have a direct match, then we simply do a direct copy
		if (this.cmfObject.hasAttribute(targetAttributeName)) { return targetAttributeName; }

		// No direct match...this is where life gets interesting...

		// Is there a singular source attribute that differs only by namespace? This is only
		// acceptable if this attribute's name without a namespace is also unique within this type,
		// and the same applies to the source (i.e. only one attribute which coincides with the
		// stripped name)
		String stripped = AlfrescoType.stripNamespace(targetAttributeName);
		if (targetType.hasStrippedAttribute(stripped)) {
			Set<String> targets = targetType.getStrippedAttributeMatches(stripped);
			if (targets.size() == 1) {
				// We have a single target...do we have a single source?
				Set<String> sourceMatches = this.strippedAttributes.get(stripped);
				if ((sourceMatches != null) && (sourceMatches.size() == 1)) {
					// We have a single potential source, so use it!
					return sourceMatches.iterator().next();
				}
			}
		}

		// We couldn't do an auto match... so there's no mapping available...
		return null;
	}

	protected final void populatePrimaryAttributes(AlfImportContext ctx, Properties p, AlfrescoType targetType,
		CmfContentInfo content) throws ImportException {

		final AlfAttributeMapper mapper = AlfAttributeMapper.getMapper(this.cmfObject);

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

			// Easy path: is this already mapped? If there's no mapping, we leave the attribute name
			// as-is to avoid confusion
			String srcAttName = Tools.coalesce(mapper.getSourceAttribute(tgtAttName), tgtAttName);
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
		Set<String> specialCopies = new HashSet<>();
		for (final String specialName : mapper.getSpecialCopies()) {
			// First get the attribute the special copy must go to
			final SchemaAttribute specialAtt = targetType.getAttribute(specialName);
			if (specialAtt == null) {
				continue;
			}

			String srcName = mapper.getSpecialCopySourceAttribute(specialName);
			specialCopies.add(srcName);
			CmfAttribute<CmfValue> srcAtt = this.cmfObject.getAttribute(srcName);
			if ((srcAtt == null) || !srcAtt.hasValues()) {
				continue;
			}

			storeValue(ctx, srcAtt, specialAtt, p, false);
		}

		Set<String> srcOnly = new HashSet<>();
		srcOnly.addAll(srcNames);
		srcOnly.removeAll(tgtNames);
		// Now handle the attributes that only exist on the source, but not on the target...here
		// we need to do some detective work...
		Set<String> processed = new HashSet<>(specialCopies);
		final String residualsPrefix = this.factory.getResidualsPrefix();
		nextAtt: for (final String srcAttName : srcOnly) {
			// Avoid attributes that have been handled specially by the code above
			if (processed.contains(srcAttName)) {
				continue;
			}

			final CmfProperty<CmfValue> srcAtt = this.cmfObject.getAttribute(srcAttName);

			// Let's see if we have a specific mapping
			Collection<String> mappings = this.factory.getMappedAttributes(srcAttName);
			if (mappings != null) {
				// We have an explicit mapping!!! Let's apply it!
				boolean mapped = false;
				for (String tgt : mappings) {
					// Here we allow copying one source attribute into many targets
					SchemaAttribute tgtAtt = targetType.getAttribute(tgt);
					if (tgtAtt != null) {
						// The mapping applies!! Let's do it!
						storeValue(ctx, srcAtt, tgtAtt, p, true);
						mapped = true;
					}
				}

				if (mapped) {
					// If this attribute was mapped, move on to the next one
					processed.add(srcAttName);
					continue nextAtt;
				}
			}
		}

		srcOnly.removeAll(processed);
		processed.clear();
		// Process whatever's left at the source that we weren't able to handle gracefully
		for (String srcAttName : srcOnly) {
			final CmfProperty<CmfValue> srcAtt = this.cmfObject.getAttribute(srcAttName);

			// Ok so no mapping was found... we'll try to auto-find the attribute using
			// a name-based approach...

			// Strip the source attribute prefix - try to match strictly based on the tail end
			final String rawName = srcAttName.replaceAll("^[^:]*:", ":");
			SchemaAttribute target = null;
			for (String tgtAttName : tgtOnly) {
				// Make sure we don't double-copy...
				if (tgtAttName.endsWith(rawName) && processed.add(tgtAttName)) {
					target = targetType.getAttribute(tgtAttName);
					break;
				}
			}

			if (target != null) {
				// We have a candidate attribute, so copy directly! But we make sure not to clobber
				// anything that may already have been assigned by some other means
				if (!p.containsKey(target.name)) {
					storeValue(ctx, srcAtt, target, p, true);
				}
			} else if (residualsPrefix != null) {
				// No candidate, copy it as a residual (if so configured)
				storeValue(ctx, srcAtt, String.format("%s%s", residualsPrefix, rawName), srcAtt.isRepeating(), p, true);
			}
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
		p.setProperty("arm:parentPathIDs", StringUtils.join(values, ','));

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
		p.setProperty("arm:parentPaths", StringUtils.join(values, ','));

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

		p.setProperty("arm:objectId", this.cmfObject.getId());

		values.clear();
		values.add(AlfImportFileableDelegate.STATUS_ASPECT);
		if (!isReference()) {
			p.setProperty("arm:historyId", this.cmfObject.getHistoryId());

			// Finally, perform user mappings for special user-relative attributes
			for (String s : AlfImportFileableDelegate.USER_CONVERSIONS) {
				final String userAttribute = mapper.getSpecialCopyMapping(s);
				if (userAttribute == null) {
					continue;
				}
				String v = this.factory.mapUser(p.getProperty(userAttribute));
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

			p.setProperty("arm:aclInfo", Tools.coalesce(generateAcl(ctx, p.getProperty("cm:owner"), group), ""));

			CmfValue aclInherit = getPropertyValue(IntermediateProperty.ACL_INHERITANCE);
			if ((aclInherit != null) && !aclInherit.isNull()) {
				p.setProperty("arm:aclInheritance", aclInherit.asString());
			}

			// Not a reference? Add the caliente aspect
			values.add(AlfImportFileableDelegate.CALIENTE_ASPECT);
		} else {
			CmfProperty<CmfValue> prop = this.cmfObject.getProperty(IntermediateProperty.REF_TARGET);
			if ((prop == null) || !prop.hasValues()) { throw new ImportException(
				String.format("Exported object %s doesn't have the required reference target metadata",
					this.cmfObject.getDescription())); }

			String refTarget = prop.getValue().asString();
			if (StringUtils.isEmpty(refTarget)) { throw new ImportException(
				String.format("Exported object %s has empty reference target metadata (must not be empty)",
					this.cmfObject.getDescription())); }
			p.setProperty("arm:refTarget", refTarget);

			prop = this.cmfObject.getProperty(IntermediateProperty.REF_VERSION);
			if ((prop != null) && prop.hasValues()) {
				String value = prop.getValue().asString();
				if (!StringUtils.isEmpty(value)) {
					p.setProperty("arm:refVersion", value);
				}
			}
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
			throw new ImportException(String.format("Failed to load the ACL [%s] associated with %s", aclId.asString(),
				this.cmfObject.getDescription()), e);
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

	private boolean resolveVdocTypes() {
		if (!this.factory.initializeVdocSupport()) { return false; }
		this.vdocRoot = this.factory.getType("cm:folder", "dctm:vdocRoot");
		this.vdocVersion = this.factory.getType("cm:folder", "dctm:vdocVersion");
		this.vdocReference = this.factory.getType("dctm:vdocReference");
		return (this.vdocRoot != null) && (this.vdocVersion != null) && (this.vdocReference != null);
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
		p.setProperty("cm:name", targetName);
		p.setProperty("arm:refTarget", targetId);
		if (!StringUtils.isEmpty(label)) {
			p.setProperty("arm:refVersion", label);
		}
		if (!StringUtils.isEmpty(referenceId)) {
			p.setProperty("dctm:vdocReferenceId", referenceId);
		}
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
			XmlProperties.saveToXML(p, out, String.format("Properties for %s", this.cmfObject.getDescription()));
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
		if (pathProp == null) { throw new ImportException(
			String.format("Failed to find the required property [%s] in %s",
				IntermediateProperty.PARENT_TREE_IDS.encode(), this.cmfObject.getDescription())); }

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
				ctx.printf("Creating a stub for %s", this.cmfObject.getDescription());
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
				this.log.warn("VDoc renditions aren't yet supported for {}", this.cmfObject.getDescription());
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
			if (this.virtual && resolveVdocTypes()) {
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
							this.log.warn("Incomplete VDoc member data for %s - [%s]", this.cmfObject.getDescription(),
								member.asString());
							continue;
						}

						String[] memberData = matcher.group(1).split("\\|");
						if (memberData.length < 5) {
							this.log.warn("Incomplete VDoc member reference data for %s - [%s]",
								this.cmfObject.getDescription(), member.asString());
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
							String.format("Failed to create a copy of the HEAD version for %s from [%s] to [%s]",
								this.cmfObject.getDescription(), main.getAbsolutePath(), newMain.getAbsolutePath()),
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