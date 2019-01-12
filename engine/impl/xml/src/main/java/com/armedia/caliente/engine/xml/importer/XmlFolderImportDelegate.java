package com.armedia.caliente.engine.xml.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.xml.importer.jaxb.FolderIndexEntryT;
import com.armedia.caliente.engine.xml.importer.jaxb.FolderIndexT;
import com.armedia.caliente.engine.xml.importer.jaxb.FolderT;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class XmlFolderImportDelegate extends XmlAggregatedImportDelegate<FolderIndexEntryT, FolderIndexT> {

	private final XmlAggregateFoldersImportDelegate delegate;

	protected XmlFolderImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, FolderIndexT.class);
		this.delegate = new XmlAggregateFoldersImportDelegate(factory, storedObject);
	}

	@Override
	protected FolderIndexEntryT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException {

		FolderT f = this.delegate.createItem(translator, ctx);
		CmfContentStore<?, ?, ?>.Handle h = ctx.getContentStore().getHandle(translator, this.cmfObject,
			new CmfContentStream(0));
		if (!h.getSourceStore().isSupportsFileAccess()) { return null; }
		File tgt = null;
		try {
			tgt = h.getFile();
		} catch (IOException e) {
			// Failed to get the file, so we can't handle this
			throw new CmfStorageException(
				String.format("Failed to locate the location for the %s", this.cmfObject.getDescription()), e);
		}
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

		boolean ok = false;
		try (OutputStream out = new FileOutputStream(tgt)) {
			XmlImportDelegateFactory.marshalXml(f, out);
			ok = true;
		} catch (FileNotFoundException e) {
			throw new ImportException(String.format("Failed to open an output stream to [%s]", tgt), e);
		} catch (IOException e) {
			throw new ImportException(
				String.format("IOException caught while writing %s to [%s] ", this.cmfObject.getDescription(), tgt), e);
		} catch (JAXBException e) {
			throw new ImportException(
				String.format("Failed to marshal the XML for %s to [%s]", this.cmfObject.getDescription(), tgt), e);
		} finally {
			if (!ok) {
				FileUtils.deleteQuietly(tgt);
			}
		}

		FolderIndexEntryT idx = new FolderIndexEntryT();
		idx.setId(f.getId());
		idx.setLocation(this.factory.relativizeXmlLocation(tgt.getAbsolutePath()));
		idx.setName(f.getName());
		idx.setPath(f.getSourcePath());
		idx.setType(f.getType());
		return idx;
	}
}