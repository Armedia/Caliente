/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
		CmfContentStore<?, ?>.Handle h = ctx.getContentStore().createHandle(translator, this.cmfObject,
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