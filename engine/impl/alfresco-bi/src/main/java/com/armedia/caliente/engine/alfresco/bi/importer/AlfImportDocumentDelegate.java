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
package com.armedia.caliente.engine.alfresco.bi.importer;

import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoType;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public class AlfImportDocumentDelegate extends AlfImportFileableDelegate {

	private static final String BASE_TYPE = "arm:document";
	private static final String RENDITION_TYPE = "arm:rendition";

	private final AlfrescoType renditionType;

	public AlfImportDocumentDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(AlfImportDocumentDelegate.BASE_TYPE, factory, storedObject);
		this.renditionType = factory.getType(AlfImportDocumentDelegate.RENDITION_TYPE);
	}

	@Override
	protected AlfrescoType calculateTargetType(CmfContentStream content) throws ImportException {
		if (!content.isDefaultRendition() || (content.getRenditionPage() > 0)) {
			// If this is a rendition or rendition extra page...
			if (this.renditionType == null) {
				throw new ImportException(String.format("Renditions are not supported for %s (content = %s)",
					this.cmfObject.getDescription(), content.toString()));
			}
			return this.renditionType;
		}
		return super.calculateTargetType(content);
	}
}