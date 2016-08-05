package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

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

public class AlfDocumentImportDelegate extends AlfImportDelegate {

	private static final Pattern SUFFIX = Pattern.compile("^.*(\\.v\\d+(?:\\.\\d+)?)$");

	private final boolean head;
	private final int major;
	private final int minor;

	public AlfDocumentImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
		CmfAttribute<CmfValue> isLatestVersionAtt = storedObject.getAttribute(IntermediateAttribute.IS_LATEST_VERSION);
		this.head = ((isLatestVersionAtt != null) && isLatestVersionAtt.hasValues()
			&& isLatestVersionAtt.getValue().asBoolean());

		CmfAttribute<CmfValue> versionLabelAtt = storedObject.getAttribute(IntermediateAttribute.VERSION_LABEL);
		// Try to parse out the major version, and get the counter from the factory for how many
		// items for that major version have been processed so far
		this.major = 0;
		this.minor = 0;
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, AlfImportContext ctx)
		throws ImportException, CmfStorageException {

		if (!ctx.getContentStore()
			.isSupportsFileAccess()) { throw new ImportException("This engine requires filesystem access"); }

		String path = null;
		CmfProperty<CmfValue> pathProp = this.cmfObject.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		if (pathProp == null) { throw new ImportException(String.format(
			"Failed to find the required property [%s] in %s [%s](%s)", IntermediateProperty.PARENT_TREE_IDS.encode(),
			this.cmfObject.getSubtype(), this.cmfObject.getLabel(), this.cmfObject.getId())); }

		String prefix = (pathProp.hasValues() ? pathProp.getValue().asString() : "");
		path = String.format("%s%s%s", prefix, StringUtils.isEmpty(prefix) ? "" : "/", this.cmfObject.getId());

		boolean foundHead = false;
		for (CmfContentInfo content : ctx.getContentInfo(this.cmfObject)) {
			CmfContentStore<?, ?, ?>.Handle h = ctx.getContentStore().getHandle(this.factory.getTranslator(),
				this.cmfObject, content);

			File main;
			try {
				main = h.getFile();
			} catch (IOException e) {
				throw new ImportException("Failure!");
			}

			String mainName = main.getName();
			Matcher m = AlfDocumentImportDelegate.SUFFIX.matcher(mainName);
			final String suffix;
			if (m.matches()) {
				suffix = m.group(1);
			} else {
				suffix = "";
				foundHead = true;
			}

			mainName = mainName.substring(0, mainName.length() - suffix.length());
			final File meta = new File(main.getParentFile(),
				String.format("%s.metadata.properties.xml%s", mainName, suffix));

			// Ok...so...now that we know where the metadata properties must go, we write them out
			Properties p = new Properties();
			for (String s : this.cmfObject.getAttributeNames()) {
				CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(s);

				if (att.getName().startsWith("cmf:") || att.getName().startsWith("cmis:")) {
					// TODO: Process the non-documentum mappings that may or may not go
					// to CMIS attributes
					continue;
				}

				final String value;
				if (att.isRepeating()) {
					// Concatenate using the separator
					StringBuilder sb = new StringBuilder();
					char separator = ',';
					// TODO: Detect separator - if it exists, try another
					int i = 0;
					for (CmfValue v : att) {
						if (i > 0) {
							sb.append(separator);
						}
						sb.append(v.asString());
						i++;
					}
					value = sb.toString();
				} else {
					// Write out the single value
					value = att.getValue().asString();
				}
				if (value != null) {
					p.setProperty(att.getName(), value);
				}
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
			// TODO: Identify the target type
			p.setProperty("type", "jsap:js_doc");

			// Set the aspects
			// TODO: Identify the target aspects
			// cm:ownable, cm:auditable, cm:author, cm:titled
			p.setProperty("aspects", "dctm:dm_document,cm:ownable,cm:auditable,cm:author,cm:titled");

			p.setProperty("dctm:r_object_id", this.cmfObject.getId());
			p.setProperty("dctm:i_chronicle_id", this.cmfObject.getBatchId());
			// p.setProperty("cmis:isLatestVersion", String.valueOf(suffix.length() == 0));

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

		// TODO: Does it nead HEAD repair (i.e. creation of one final file based on an earlier
		// version? If so, do it here

		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), path));
	}
}