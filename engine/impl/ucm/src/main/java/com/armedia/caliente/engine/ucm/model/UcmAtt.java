/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.engine.ucm.model;

public enum UcmAtt {

	// Custom attributes
	cmfLatestVersion,
	cmfParentPath,
	cmfParentURI,
	cmfPath,
	cmfUniqueURI,

	// System attributes
	ProductBuildInfo, //

	// Common Attributes
	fCreateDate, //
	fCreator, //
	fDisplayName, //
	fIsInTrash, //
	fLastModifiedDate, //
	fLastModifier, //
	fOwner, //
	fParentGUID, //
	fSecurityGroup, //
	fTargetGUID, //

	// Folder Attributes
	fDisplayDescription, //
	fFolderDescription, //
	fFolderGUID, //
	fFolderName, //

	// Document Attributes
	dCheckoutUser, //
	dFileSize, //
	dFormat, //
	dDocAuthor, //
	dDocCreator, //
	dDocCreatedDate, //
	dDocLastModifiedDate, //
	dDocLastModifier, //
	dDocName, //
	dDocTitle, //
	dDocType, //
	dExtension, //
	dID, //
	dOriginalName, //
	dProcessingState, //
	dPublishedRevisionID, //
	dRevisionID, //
	dRevLabel, //
	dRevRank, //
	dStatus, //
	fFileGUID, //
	fFileName, //
	fPublishedFilename, //
	xComments, //

	// Rendition Attributes
	rendDescription, //
	rendFormat, //
	rendName, //
	rendType, //
	//
	;
}