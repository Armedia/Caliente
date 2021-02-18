/*******************************************************************************
r * #%L
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

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.xml.importer.jaxb.FolderIndexEntryT;
import com.armedia.caliente.engine.xml.importer.jaxb.FolderIndexT;
import com.armedia.caliente.engine.xml.importer.jaxb.FolderT;
import com.armedia.caliente.store.CmfAttributeTranslator;
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

		if (!ctx.getContentStore().isSupportsFileAccess()) { return null; }

		FolderT f = this.delegate.createItem(translator, ctx);
		String location = ctx.getContentStore().renderContentPath(this.cmfObject,
			new CmfContentStream(this.cmfObject, 0));
		location = String.format("%s.folder.xml", location);
		Path tgt = ctx.getSession().getMetadataRoot().resolve(location);
		Path dir = ctx.getSession().makeAbsolute(tgt);
		dir = tgt.getParent();
		if (dir != null) {
			try {
				FileUtils.forceMkdir(dir.toFile());
			} catch (IOException e) {
				throw new ImportException(String.format("Failed to create the folder at [%s]", dir.toAbsolutePath()),
					e);
			}
		}

		boolean ok = false;
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tgt.toFile()))) {
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
				FileUtils.deleteQuietly(tgt.toFile());
			}
		}

		FolderIndexEntryT idx = new FolderIndexEntryT();
		idx.setId(f.getId());
		idx.setLocation(location);
		idx.setName(f.getName());
		idx.setPath(f.getSourcePath());
		idx.setType(f.getType());
		return idx;
	}
}