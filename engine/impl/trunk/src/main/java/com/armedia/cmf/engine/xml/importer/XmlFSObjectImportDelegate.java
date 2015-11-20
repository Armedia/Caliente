package com.armedia.cmf.engine.xml.importer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.SystemUtils;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;
import com.armedia.cmf.storage.tools.FilenameFixer;
import com.armedia.commons.utilities.FileNameTools;

public abstract class XmlFSObjectImportDelegate extends XmlImportDelegate {

	protected XmlFSObjectImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
	}

	protected final String getNewId(File f) {
		return String.format("%08X", f.hashCode());
	}

	@Override
	protected final Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator,
		XmlImportContext ctx) throws ImportException, CmfStorageException, CmfValueDecoderException {

		CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(IntermediateAttribute.IS_LAST_VERSION);
		if ((att != null) && att.hasValues()) {
			CmfValue v = att.getValue();
			if (!v.isNull() && !v.asBoolean() && !this.factory.isIncludeAllVersions()) {
				// If this isn't the last version, we bork out if we're configured to only deal
				// with the last version
				if (this.log.isDebugEnabled()) {
					this.log.warn(String.format("Skipping non-final version for %s [%s](%s)", this.cmfObject.getType(),
						this.cmfObject.getLabel(), this.cmfObject.getId()));
				}
				return Collections.singleton(new ImportOutcome(ImportResult.SKIPPED));
			}
		}

		return doImportObject(translator, ctx);
	}

	protected abstract Collection<ImportOutcome> doImportObject(CmfAttributeTranslator<CmfValue> translator,
		XmlImportContext ctx) throws ImportException, CmfStorageException, CmfValueDecoderException;

	protected final File getTargetFile(XmlImportContext ctx) throws ImportException, IOException {
		final CmfAttributeTranslator<CmfValue> translator = this.factory.getEngine().getTranslator();

		// TODO: We must also determine if the target FS requires "windows mode".. for instance
		// for NTFS on Linux, windows restrictions must be observed... but there's no "clean"
		// way to figure that out from Java...
		final boolean windowsMode = SystemUtils.IS_OS_WINDOWS;

		File tgt = ctx.getSession().getFile();

		CmfProperty<CmfValue> pathProp = this.cmfObject.getProperty(IntermediateProperty.PATH);
		String p = "/";
		if ((pathProp != null) && pathProp.hasValues()) {
			p = ctx.getTargetPath(pathProp.getValue().toString());
		} else {
			p = ctx.getTargetPath(p);
		}

		for (String s : FileNameTools.tokenize(p, '/')) {
			tgt = new File(tgt, FilenameFixer.safeEncode(s, windowsMode));
		}

		CmfAttribute<CmfValue> nameAtt = this.cmfObject.getAttribute(translator.decodeAttributeName(
			this.cmfObject.getType(), IntermediateAttribute.NAME));

		// We always fix the file's name, since it's not part of the path and may also need fixing.
		// Same dilemma as above, though - need to know "when" to use windows mode...
		String name = FilenameFixer.safeEncode(nameAtt.getValue().toString(), windowsMode);
		return new File(tgt, name).getCanonicalFile();
	}
}