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
/**
 *
 */

package com.armedia.caliente.engine.exporter;

import java.util.Stack;

import org.slf4j.Logger;

import com.armedia.caliente.engine.TransferContext;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.content.ContentExtractor;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.CfgTools;

/**
 *
 *
 */
public class ExportContext< //
	SESSION, //
	VALUE, //
	CONTEXT_FACTORY extends ExportContextFactory<SESSION, ?, VALUE, ?, ?> //
> extends TransferContext<SESSION, VALUE, CONTEXT_FACTORY> {

	private final Stack<ExportTarget> referrents = new Stack<>();

	/**
	 * @param rootId
	 * @param rootType
	 * @param session
	 * @param output
	 */
	public ExportContext(CONTEXT_FACTORY factory, CfgTools settings, String rootId, CmfObject.Archetype rootType,
		SESSION session, Logger output, WarningTracker tracker) {
		super(factory, settings, rootId, rootType, session, output, tracker);
	}

	final void pushReferrent(ExportTarget referrent) {
		if (referrent == null) { throw new IllegalArgumentException("Must provide a referrent object to track"); }
		this.referrents.push(referrent);
	}

	public boolean shouldWaitForRequirement(CmfObject.Archetype referrent, CmfObject.Archetype referenced) {
		switch (referrent) {
			case FOLDER:
			case DOCUMENT:
				return (referenced == CmfObject.Archetype.FOLDER);
			default:
				return false;
		}
	}

	public final ContentExtractor getContentExtractor() {
		return null;
	}

	public final boolean isReferrentLoop(ExportTarget referrent) {
		return this.referrents.contains(referrent);
	}

	public final ExportTarget getReferrent() {
		if (this.referrents.isEmpty()) { return null; }
		return this.referrents.peek();
	}

	public final String getFixedName(CmfObject.Archetype type, String objectId, String historyId) {
		return getFactory().getFixedName(type, objectId, historyId);
	}

	public final String getFixedName(CmfObject<VALUE> object) {
		return getFactory().getFixedName(object);
	}

	ExportTarget popReferrent() {
		if (this.referrents.isEmpty()) { return null; }
		return this.referrents.pop();
	}

	public final boolean isSupportsCompanionMetadata(CmfObject.Archetype type) {
		return getFactory().isSupportsCompanionMetadata(type);
	}
}