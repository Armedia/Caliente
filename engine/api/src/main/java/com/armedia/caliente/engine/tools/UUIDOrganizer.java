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
package com.armedia.caliente.engine.tools;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class UUIDOrganizer extends LocalOrganizer {
	public static final String NAME = "uuid";

	protected static final Pattern UUID_PARSER = Pattern
		.compile("\\b([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\b", Pattern.CASE_INSENSITIVE);

	private Set<String> candidates = Collections.emptySet();

	public UUIDOrganizer() {
		this(UUIDOrganizer.NAME);
	}

	protected UUIDOrganizer(String name) {
		super(name);
	}

	@Override
	protected void doConfigure(CfgTools settings) {
		super.doConfigure(settings);
		this.candidates = Tools
			.freezeSet(new LinkedHashSet<>(Tools.splitEscaped(',', settings.getString("candidates", ""))));
	}

	protected <T> List<Triple<String, Number, String>> gatherCandidates(CmfAttributeTranslator<T> translator,
		CmfObject<T> object) {
		// First... harvest the candidate values
		List<Triple<String, Number, String>> candidateValues = new LinkedList<>();
		for (String attName : this.candidates) {
			CmfAttribute<T> att = object.getAttribute(attName);
			if (att == null) {
				// Is this "ID" or "HistoryID"?
				if (StringUtils.equalsAnyIgnoreCase(attName, "id", "historyId")) {
					if (StringUtils.equalsIgnoreCase("id", attName)) {
						candidateValues.add(Triple.of("id", 0, object.getId()));
					}
					if (StringUtils.equalsIgnoreCase("historyId", attName)) {
						candidateValues.add(Triple.of("historyId", 0, object.getHistoryId()));
					}
				}
			} else if (att.hasValues()) {
				CmfValueCodec<T> codec = translator.getCodec(att.getType());
				int pos = -1;
				for (T t : att) {
					candidateValues.add(Triple.of(attName, ++pos, codec.encode(t).asString()));
				}
			}
		}
		// If there are no candidate values listed, by default use the object ID and the
		// history ID ... in that order
		if (candidateValues.isEmpty()) {
			candidateValues.add(Triple.of("id", 0, object.getId()));
			candidateValues.add(Triple.of("historyId", 0, object.getHistoryId()));
		}
		return candidateValues;
	}

	protected static UUID parseUuid(String value) {
		Matcher m = UUIDOrganizer.UUID_PARSER.matcher(value);
		if (!m.find()) { return null; }
		return UUID.fromString(m.group(1));
	}

	private UUID parseUuid(Triple<String, Number, String> candidate) {
		UUID uuid = UUIDOrganizer.parseUuid(candidate.getRight());
		if ((uuid == null) && this.log.isDebugEnabled()) {
			this.log.debug("No UUID found in {}[{}] = [{}]", candidate.getLeft(), candidate.getMiddle(),
				candidate.getRight());
		}
		return uuid;
	}

	protected <T> UUID findUUID(CmfAttributeTranslator<T> translator, CmfObject<T> object) {
		List<Triple<String, Number, String>> candidateValues = gatherCandidates(translator, object);
		UUID uuid = null;
		for (Triple<String, Number, String> candidate : candidateValues) {
			uuid = parseUuid(candidate);
			if (uuid != null) { return uuid; }
		}
		return new UUID(object.getNumber(), object.getNumber());
	}

	@Override
	protected <T> Location calculateLocation(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		final UUID uuid = findUUID(translator, object);
		final String appendix = String.format("%08x", info.getIndex());
		return newLocation(Collections.emptyList(), uuid.toString(), null, null, appendix);
	}
}