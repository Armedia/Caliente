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
package com.armedia.caliente.engine.exporter;

import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferContextFactory;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public abstract class ExportContextFactory< //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ExportContext<SESSION, VALUE, ?>, //
	ENGINE extends ExportEngine<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?, ?> //
> extends TransferContextFactory<SESSION, VALUE, CONTEXT, ENGINE> {

	private final Set<CmfObject.Archetype> companionMetadata;

	protected ExportContextFactory(ENGINE engine, CfgTools settings, SESSION session, CmfObjectStore<?> objectStore,
		CmfContentStore<?, ?> contentStore, Logger output, WarningTracker tracker) throws Exception {
		super(engine, settings, session, objectStore, contentStore, null, output, tracker);

		Set<CmfObject.Archetype> companionMetadata = EnumSet.noneOf(CmfObject.Archetype.class);
		if (contentStore.isSupportsFileAccess()) {
			if (settings.hasValue(ExportSetting.METADATA_XML)) {
				for (CmfObject.Archetype t : settings.getEnums(ExportSetting.METADATA_XML, CmfObject.Archetype.class,
					(o, e) -> null)) {
					if (t != null) {
						companionMetadata.add(t);
					}
				}
			}
		}
		Set<CmfObject.Archetype> allowedCompanionMetadata = getAllowedCompanionMetadata();
		if ((allowedCompanionMetadata != null) && !allowedCompanionMetadata.isEmpty()) {
			companionMetadata.retainAll(allowedCompanionMetadata);
		}
		if (this.log.isDebugEnabled()) {
			this.log.debug("Companion metadata that will be generated for this context factory instance ({}): {}",
				getClass().getSimpleName(), companionMetadata);
		}
		this.companionMetadata = Tools.freezeSet(companionMetadata);
	}

	protected Set<CmfObject.Archetype> getAllowedCompanionMetadata() {
		return EnumSet.of(CmfObject.Archetype.FOLDER, CmfObject.Archetype.DOCUMENT);
	}

	public final boolean isSupportsCompanionMetadata(CmfObject.Archetype type) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to check for"); }
		return !this.companionMetadata.contains(type);
	}

	@Override
	protected String getContextLabel() {
		return "export";
	}
}