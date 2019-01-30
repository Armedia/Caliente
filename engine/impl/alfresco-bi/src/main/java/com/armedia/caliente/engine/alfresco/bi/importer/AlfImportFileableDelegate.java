package com.armedia.caliente.engine.alfresco.bi.importer;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.index.ScanIndexItemMarker.MarkerType;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoType;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaAttribute;
import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.tools.AclTools.AccessorType;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectHandler;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueSerializer;
import com.armedia.caliente.store.tools.DefaultCmfObjectHandler;
import com.armedia.commons.utilities.Tools;

abstract class AlfImportFileableDelegate extends AlfImportDelegate {

	protected static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	public static final String REFERENCE_TYPE = "arm:reference";
	public static final String STATUS_ASPECT = "arm:calienteStatus";
	public static final String CALIENTE_ASPECT = "arm:caliente";

	private static final String TYPE_PROPERTY = "type";
	private static final String ASPECT_PROPERTY = "aspects";

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

	private final boolean reference;
	private final boolean virtual;
	private final AlfrescoType defaultType;
	private final AlfrescoType referenceType;

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
	}

	protected final boolean isVirtual() {
		return this.virtual;
	}

	protected final boolean isReference() {
		return this.reference;
	}

	protected AlfrescoType calculateTargetType(CmfContentStream content) throws ImportException {
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

	protected final AlfrescoType getTargetType(CmfContentStream content) throws ImportException {
		if (!isReference()) { return calculateTargetType(content); }
		if (this.referenceType == null) {
			throw new ImportException(
				String.format("References are not supported for %s", this.cmfObject.getDescription()));
		}
		return this.referenceType;
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
				serializer = CmfValueSerializer.get(CmfValue.Type.STRING);
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

	private boolean includeProperty(String propertyName, AlfrescoType targetType) {
		return targetType.hasAttribute(propertyName);
	}

	protected final void populatePrimaryAttributes(AlfImportContext ctx, Properties p, AlfrescoType targetType,
		CmfContentStream content) throws ImportException {

		this.cmfObject.forEach((attribute) -> {
			List<String> newValues = new ArrayList<>();
			attribute.forEach((v) -> {
				if (v == null) {
					v = attribute.getType().getNull();
				}
				final String s;
				try {
					s = v.serialize();
				} catch (ParseException e) {
					throw new AlfRenderingException(
						String.format("Failed to render %s value [%s]", v.getDataType().name(), v.asString()), e);
				}
				if (attribute.isRepeating() || !StringUtils.isEmpty(s)) {
					newValues.add(s);
				}
			});
			// Don't add properties that have no values
			if (newValues.isEmpty()) { return; }

			String propertyValue = null;
			if (newValues.size() == 1) {
				propertyValue = newValues.get(0);
			} else {
				// Make sure no "null" strings make it in
				newValues.replaceAll((v) -> (v == null ? StringUtils.EMPTY : v));
				propertyValue = Tools.joinCSVEscaped(newValues);
			}

			// Don't add empty/null properties
			if (!StringUtils.isEmpty(propertyValue)) {
				p.setProperty(attribute.getName(), propertyValue);
			}
		});

		// Now handle the special properties
		Set<String> values = new LinkedHashSet<>();
		String currentProperty = null;

		currentProperty = "arm:parentPathIDs";
		if (includeProperty(currentProperty, targetType)) {
			values.clear();
			getPropertyValues(IntermediateProperty.PARENT_TREE_IDS).stream().map((v) -> v.asString())
				.filter(StringUtils::isNotEmpty).forEachOrdered(values::add);
			if (!values.isEmpty()) {
				p.setProperty(currentProperty, Tools.joinCSVEscaped(values));
			}
		}

		currentProperty = "arm:parentPaths";
		if (includeProperty(currentProperty, targetType)) {
			values.clear();
			getPropertyValues(IntermediateProperty.PATH).forEach((v) -> {
				String s = v.asString();
				if (!StringUtils.isEmpty(s)) {
					try {
						values.add(URLEncoder.encode(s, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new AlfRenderingException("Unsupported encoding UTF-8...what?!?!?", e);
					}
				}
			});
			if (!values.isEmpty()) {
				p.setProperty(currentProperty, Tools.joinCSVEscaped(values));
			}
		}

		// Set the type property
		p.setProperty(AlfImportFileableDelegate.TYPE_PROPERTY, targetType.getName());

		currentProperty = "arm:objectId";
		if (includeProperty(currentProperty, targetType)) {
			p.setProperty(currentProperty, this.cmfObject.getId());
		}

		values.clear();
		if (this.factory.getSchema().hasAspect(AlfImportFileableDelegate.STATUS_ASPECT)) {
			values.add(AlfImportFileableDelegate.STATUS_ASPECT);
		}

		if (!isReference()) {
			// Set the history ID property
			currentProperty = "arm:historyId";
			if (includeProperty(currentProperty, targetType)) {
				p.setProperty(currentProperty, this.cmfObject.getHistoryId());
			}

			currentProperty = "arm:aclInfo";
			if (includeProperty(currentProperty, targetType)) {
				// Map the group attributes
				String group = null;
				CmfValue groupValue = getAttributeValue(IntermediateAttribute.GROUP);
				if (groupValue != null) {
					group = groupValue.asString();
				}
				p.setProperty(currentProperty, Tools.coalesce(generateAcl(ctx, p.getProperty("cm:owner"), group), ""));
			}

			currentProperty = "arm:aclInheritance";
			if (includeProperty(currentProperty, targetType)) {
				CmfValue aclInherit = getPropertyValue(IntermediateProperty.ACL_INHERITANCE);
				if ((aclInherit != null) && !aclInherit.isNull()) {
					p.setProperty("arm:aclInheritance", aclInherit.asString());
				}
			}

			// Not a reference? Add the caliente aspect
			if (this.factory.getSchema().hasAspect(AlfImportFileableDelegate.CALIENTE_ASPECT)) {
				values.add(AlfImportFileableDelegate.CALIENTE_ASPECT);
			}
		} else {
			final String refTarget = "arm:refTarget";
			final String refVersion = "arm:refVersion";
			if (includeProperty(refTarget, targetType) && includeProperty(refVersion, targetType)) {
				CmfProperty<CmfValue> prop = this.cmfObject.getProperty(IntermediateProperty.REF_TARGET);
				if ((prop == null) || !prop.hasValues()) {
					throw new ImportException(
						String.format("Exported object %s doesn't have the required reference target metadata",
							this.cmfObject.getDescription()));
				}

				String refTargetValue = prop.getValue().asString();
				if (StringUtils.isEmpty(refTargetValue)) {
					throw new ImportException(
						String.format("Exported object %s has empty reference target metadata (must not be empty)",
							this.cmfObject.getDescription()));
				}
				p.setProperty("arm:refTarget", refTargetValue);

				prop = this.cmfObject.getProperty(IntermediateProperty.REF_VERSION);
				if ((prop != null) && prop.hasValues()) {
					String value = prop.getValue().asString();
					if (!StringUtils.isEmpty(value)) {
						p.setProperty(refVersion, value);
					}
				}
			}
		}

		targetType.getExtraAspects().forEach((s) -> {
			if (!StringUtils.isEmpty(s)) {
				if (!this.factory.getSchema().hasAspect(s)) {
					throw new AlfRenderingException(String.format(
						"No aspect named [%s] is defined in the current content model schema, while importing %s", s,
						this.cmfObject.getDescription()));
				}
				values.add(s);
			}
		});

		if (!values.isEmpty()) {
			String aspectList = StringUtils.join(values, ',');
			p.setProperty(AlfImportFileableDelegate.ASPECT_PROPERTY, aspectList);

			currentProperty = "arm:aspects";
			if (includeProperty(currentProperty, targetType)) {
				p.setProperty(currentProperty, aspectList);
			}
		}

		// Now, get the head object
		currentProperty = "cm:name";
		if (includeProperty(currentProperty, targetType)) {
			CmfObject<CmfValue> head = this.cmfObject;
			try {
				head = ctx.getHeadObject(this.cmfObject);
			} catch (CmfStorageException e) {
				this.log.warn(String.format("Failed to load the HEAD object for %s batch [%s]",
					this.cmfObject.getType().name(), this.cmfObject.getHistoryId()), e);
			}
			String name = ctx.getObjectName(head);

			CmfProperty<CmfValue> unfiledProp = this.cmfObject.getProperty(IntermediateProperty.IS_UNFILED);
			final boolean unfiled = (unfiledProp != null) && unfiledProp.hasValues()
				&& unfiledProp.getValue().asBoolean();
			if (unfiled) {
				// This helps protect against duplicate object names
				name = this.factory.getUnfiledName(ctx, this.cmfObject);
			}
			p.setProperty(currentProperty, name);
		}
	}

	protected final String generateAcl(final AlfImportContext ctx, final String owner, final String group)
		throws ImportException {
		// Make sure that if ACL processing is disabled, we don't process it
		// if (!ctx.isSupported(CmfObject.Archetype.ACL)) { return null; }
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

				final int count = Tools.min(accessors.getValueCount(), accessorTypes.getValueCount(),
					permits.getValueCount());

				for (int i = 0; i < count; i++) {
					String accessor = accessors.getValue(i).asString();
					final boolean is_group = accessorTypes.getValue(i).asBoolean();
					final int permit = permits.getValue(i).asInteger();
					final int permitType;
					if (i < permitTypes.getValueCount()) {
						permitType = permitTypes.getValue(i).asInteger();
					} else {
						AlfImportFileableDelegate.this.log.warn(
							"Assuming permitType value 0 for {} referenced from {}, accessor # {} ({} [{}]), expected {} but only have {} permitType values",
							dataObject.getDescription(), AlfImportFileableDelegate.this.cmfObject.getDescription(),
							i + 1, (is_group ? "group" : "user"), accessor, count, permitTypes.getValueCount());
						permitType = 0;
					}

					if (permitType != 0) {
						// We can't handle other permit types than AccessPermit yet...
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
						// accessor = AlfImportFileableDelegate.this.factory.mapGroup(accessor);
					} else {
						type = AccessorType.USER;
						// accessor = AlfImportFileableDelegate.this.factory.mapUser(accessor);
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
			int count = ctx.loadObjects(CmfObject.Archetype.ACL, Collections.singleton(aclId.asString()), handler);
			if (count > 0) { return ret.toString(); }
		} catch (CmfStorageException e) {
			throw new ImportException(String.format("Failed to load the ACL [%s] associated with %s", aclId.asString(),
				this.cmfObject.getDescription()), e);
		}
		return null;
	}

	protected String generateRenditionName(CmfContentStream info) {
		return String.format("%s-[%s]-%08x-%s", this.cmfObject.getId(), info.getRenditionIdentifier(),
			info.getRenditionPage(), Tools.coalesce(info.getModifier(), ""));
	}

	protected final void populateRenditionAttributes(Properties p, AlfrescoType targetType, CmfContentStream content)
		throws ImportException {
		// Set the type property
		p.setProperty(AlfImportFileableDelegate.TYPE_PROPERTY, targetType.getName());
		p.setProperty("cm:name", generateRenditionName(content));
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

	@Override
	protected final Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator,
		AlfImportContext ctx) throws ImportException, CmfStorageException {
		boolean ok = false;
		try {
			Collection<ImportOutcome> outcomes = doImportObject(translator, ctx);
			ok = true;
			return outcomes;
		} finally {
			if (!ok) {
				this.factory.resetIndex();
			}
		}
	}

	protected final Collection<ImportOutcome> doImportObject(CmfAttributeTranslator<CmfValue> translator,
		AlfImportContext ctx) throws ImportException, CmfStorageException {

		if (!ctx.getContentStore().isSupportsFileAccess()) {
			throw new ImportException("This engine requires filesystem access");
		}

		String path = null;
		CmfValue pathProp = getPropertyValue(IntermediateProperty.LATEST_PARENT_TREE_IDS);
		if ((pathProp == null) || pathProp.isNull()) {
			pathProp = getPropertyValue(IntermediateProperty.PARENT_TREE_IDS);
		}
		if (pathProp == null) {
			throw new ImportException(String.format("Failed to find the required property [%s] in %s",
				IntermediateProperty.PARENT_TREE_IDS.encode(), this.cmfObject.getDescription()));
		}

		String prefix = (!pathProp.isNull() ? pathProp.asString() : "");
		path = String.format("%s%s%s", prefix, StringUtils.isEmpty(prefix) ? "" : "/", this.cmfObject.getId());

		// Step 1: copy over all the attributes that need copying over, un-mapping them as needed
		Collection<CmfContentStream> contents = ctx.getContentStreams(this.cmfObject);
		if (contents.isEmpty()) {
			// No content streams, so make one up so we can build the properties file
			contents = Collections.singleton(new CmfContentStream(0));
		}

		boolean vdocRootIndexed = false;
		Set<String> vdocVersionsIndexed = new HashSet<>();
		boolean renditionsRootIndexed = false;
		Set<String> renditionTypesIndexed = new HashSet<>();
		final boolean skipRenditions = this.factory.isSkipRenditions();
		for (CmfContentStream content : contents) {
			if (skipRenditions && !content.isDefaultRendition()) {
				// Skip the non-default rendition
				continue;
			}

			CmfContentStore<?, ?, ?>.Handle h = ctx.getContentStore().getHandle(this.factory.getTranslator(),
				this.cmfObject, content);

			// First things first: identify the type we're going to store into
			AlfrescoType targetType = getTargetType(content);
			if (targetType == null) {
				// No type...so...this is a skip
				throw new ImportException(String.format(
					"Failed to identify a target object type for %s, content %s (source type = %s, secondaries = %s, reference = %s)",
					this.cmfObject.getDescription(), content.toString(), this.cmfObject.getSubtype(),
					this.cmfObject.getSecondarySubtypes(), isReference()));
			}

			final File main;
			try {
				main = h.getFile();
			} catch (IOException e) {
				throw new ImportException("Failure!", e);
			}

			if (!main.exists()) {
				ctx.printf("Creating a stub for %s", this.cmfObject.getDescription());
				if (!createStub(ctx, main, this.cmfObject.getLabel())) {
					return Collections.singleton(ImportOutcome.SKIPPED);
				}
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
				try {
					populatePrimaryAttributes(ctx, p, targetType, content);
				} catch (AlfRenderingException e) {
					throw new ImportException(e.getMessage(), e);
				}
			} else {
				// This is a supplementary rendition, and thus will need some minimal
				// metadata set on it
				populateRenditionAttributes(p, targetType, content);
				if (!renditionsRootIndexed) {
					this.factory.storeToIndex(ctx, false, this.cmfObject, main, null, MarkerType.RENDITION_ROOT);
					renditionsRootIndexed = true;
				}
				if (renditionTypesIndexed.add(content.getRenditionIdentifier())) {
					this.factory.storeToIndex(ctx, false, this.cmfObject, main, null, MarkerType.RENDITION_TYPE);
				}
			}

			final File meta = this.factory.generateMetadataFile(p, this.cmfObject, main);
			if (this.virtual && resolveVdocTypes()) {
				File vdocVersion = meta.getParentFile();
				if (vdocVersionsIndexed.add(vdocVersion.getName())) {
					// Does the reference home already have properties? If not, then add them...
					Properties versionProps = new Properties();
					try {
						populatePrimaryAttributes(ctx, versionProps, this.vdocRoot, content);
					} catch (AlfRenderingException e) {
						throw new ImportException(e.getMessage(), e);
					}

					if (this.cmfObject.isHistoryCurrent() && !vdocRootIndexed) {
						final File vdocRootMeta = this.factory.generateMetadataFile(versionProps, this.cmfObject,
							vdocVersion.getParentFile());
						this.factory.storeToIndex(ctx, true, this.cmfObject, vdocVersion.getParentFile(), vdocRootMeta,
							MarkerType.VDOC_ROOT);
						vdocRootIndexed = true;
					}

					versionProps.setProperty("cm:name", vdocVersion.getName());
					versionProps.setProperty("dctm:object_name", vdocVersion.getName());
					versionProps.setProperty(AlfImportFileableDelegate.TYPE_PROPERTY, this.vdocVersion.getName());
					Set<String> aspects = new LinkedHashSet<>(this.vdocVersion.getAspects());
					aspects.add(AlfImportFileableDelegate.STATUS_ASPECT);
					versionProps.setProperty(AlfImportFileableDelegate.ASPECT_PROPERTY, StringUtils.join(aspects, ','));
					final File vdocVersionMeta = this.factory.generateMetadataFile(versionProps, this.cmfObject,
						vdocVersion);
					this.factory.storeToIndex(ctx, true, this.cmfObject, vdocVersion, vdocVersionMeta,
						MarkerType.VDOC_VERSION);
				}
				this.factory.storeToIndex(ctx, false, this.cmfObject, main, meta,
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
						File vdocMemberMeta = this.factory.generateMetadataFile(vdocMemberProperties, this.cmfObject,
							vdocMember);
						this.factory.storeToIndex(ctx, false, this.cmfObject, vdocMember, vdocMemberMeta,
							MarkerType.VDOC_REFERENCE);
					}
				}
			} else {
				final boolean folder = (main.exists() && main.isFile() ? false : true);
				this.factory.storeToIndex(ctx, folder, this.cmfObject, main, meta, markerType);

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