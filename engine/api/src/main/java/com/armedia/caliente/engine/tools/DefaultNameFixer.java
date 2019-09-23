package com.armedia.caliente.engine.tools;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfNameFixer;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObject.Archetype;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public abstract class DefaultNameFixer implements CmfNameFixer<CmfValue> {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private static final Pattern MAP_KEY_PARSER = Pattern.compile("^\\s*([^#\\s]+)\\s*#\\s*(.+)\\s*$");

	private final Map<CmfObject.Archetype, Map<String, String>> idMap;

	public DefaultNameFixer(Properties p) {
		Set<CmfObject.Archetype> keys = EnumSet.noneOf(CmfObject.Archetype.class);
		Map<CmfObject.Archetype, Map<String, String>> idMap = new EnumMap<>(CmfObject.Archetype.class);
		for (String key : p.stringPropertyNames()) {
			final String fixedName = p.getProperty(key);
			Matcher matcher = DefaultNameFixer.MAP_KEY_PARSER.matcher(key);
			if (!matcher.matches()) {
				continue;
			}
			final String T = matcher.group(1);
			final CmfObject.Archetype t;
			try {
				t = CmfObject.Archetype.valueOf(T);
			} catch (Exception e) {
				this.log.warn("Unsupported object type found [{}] in key [{}] (value = [{}])", T, key, fixedName, e);
				continue;
			}
			final String id = matcher.group(2);
			Map<String, String> m = idMap.get(t);
			if (m == null) {
				m = new TreeMap<>();
				idMap.put(t, m);
				keys.add(t);
			}
			m.put(id, fixedName);
		}
		for (CmfObject.Archetype t : keys) {
			Map<String, String> m = idMap.get(t);
			m = Tools.freezeMap(m);
			idMap.put(t, m);
		}
		this.idMap = Tools.freezeMap(idMap);
	}

	public DefaultNameFixer(Map<CmfObject.Archetype, Map<String, String>> idMap) {
		this.idMap = Tools.freezeMap(idMap, true);
	}

	@Override
	public final boolean isEmpty() {
		return this.idMap.isEmpty();
	}

	@Override
	public final boolean supportsType(Archetype type) {
		return this.idMap.containsKey(type);
	}

	public final Map<String, String> getMappings(CmfObject.Archetype type) {
		return this.idMap.get(type);
	}

	@Override
	public final String fixName(CmfObject<CmfValue> dataObject) {
		if (dataObject == null) { return null; }
		Map<String, String> mappings = getMappings(dataObject.getType());
		if ((mappings == null) || mappings.isEmpty()) { return null; }
		String result = mappings.get(dataObject.getId());
		if (result == null) {
			// No fix for the specific object? What about the history as a whole?
			result = mappings.get(dataObject.getHistoryId());
		}
		return result;
	}

	@Override
	public boolean handleException(Exception e) {
		return false;
	}
}