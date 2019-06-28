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

import oracle.stellent.ridc.model.DataBinder;

public enum FolderIteratorMode {
	//
	COMBINED {
		@Override
		protected void setParameters(DataBinder requestBinder) {
			super.setParameters(requestBinder);
			requestBinder.putLocal("doCombinedBrowse", "1");
			requestBinder.putLocal("foldersFirst", "1");
		}
	}, //
	FILES, //
	FOLDERS, //
	//
	;

	final String count;
	final String startRow;

	private FolderIteratorMode() {
		String countLabel = name().toLowerCase();
		this.count = String.format("%sCount", countLabel);
		this.startRow = String.format("%sStartRow", countLabel);
	}

	protected void setParameters(DataBinder requestBinder) {
		requestBinder.putLocal("doRetrieveTargetInfo", "1");
	}
}