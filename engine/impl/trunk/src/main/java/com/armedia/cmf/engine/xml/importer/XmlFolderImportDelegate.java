package com.armedia.cmf.engine.xml.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.xml.importer.jaxb.FolderIndexEntryT;
import com.armedia.cmf.engine.xml.importer.jaxb.FolderIndexT;
import com.armedia.cmf.engine.xml.importer.jaxb.FolderT;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;
import com.armedia.commons.utilities.XmlTools;

public class XmlFolderImportDelegate extends XmlAggregatedImportDelegate<FolderIndexEntryT, FolderIndexT> {

	private final XmlAggregateFoldersImportDelegate delegate;

	protected XmlFolderImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, FolderIndexT.class);
		this.delegate = new XmlAggregateFoldersImportDelegate(factory, storedObject);
	}

	@Override
	protected FolderIndexEntryT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {

		FolderT f = this.delegate.createItem(translator, ctx);
		CmfContentStore<?, ?, ?>.Handle h = ctx.getContentStore().getHandle(translator, this.cmfObject, "");
		if (!h.getSourceStore().isSupportsFileAccess()) { return null; }
		File tgt = h.getFile();
		File dir = tgt.getParentFile();
		if (dir != null) {
			try {
				FileUtils.forceMkdir(dir);
			} catch (IOException e) {
				throw new ImportException(String.format("Failed to create the folder at [%s]", dir.getAbsolutePath()),
					e);
			}
		}

		tgt = new File(dir, String.format("%s-folder.xml", tgt.getName()));

		final OutputStream out;
		try {
			out = new FileOutputStream(tgt);
		} catch (FileNotFoundException e) {
			throw new ImportException(String.format("Failed to open an output stream to [%s]", tgt), e);
		}

		boolean ok = false;
		String xml = null;
		try {
			xml = XmlTools.marshal(f, XmlImportDelegateFactory.SCHEMA, true);
			try {
				IOUtils.write(xml, out);
			} catch (IOException e) {
				throw new ImportException(String.format("Failed to write out the XML to [%s]:%n%s", tgt, xml), e);
			}
			ok = true;
		} catch (JAXBException e) {
			throw new ImportException(String.format("Failed to marshal the XML for folder [%s](%s) to [%s]",
				this.cmfObject.getLabel(), this.cmfObject.getId(), tgt), e);
		} finally {
			IOUtils.closeQuietly(out);
			if (!ok) {
				FileUtils.deleteQuietly(tgt);
			}
		}

		FolderIndexEntryT idx = new FolderIndexEntryT();
		idx.setId(f.getId());
		idx.setLocation(this.factory.relativizeXmlLocation(tgt.getAbsolutePath()));
		idx.setName(f.getName());
		idx.setPath(f.getSourcePath());
		return idx;
	}
}