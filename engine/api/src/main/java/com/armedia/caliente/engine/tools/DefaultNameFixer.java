package com.armedia.caliente.engine.tools;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.store.CmfNameFixer;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObject.Archetype;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class DefaultNameFixer implements CmfNameFixer<CmfValue> {

	private static final Logger LOG = LoggerFactory.getLogger(CmfNameFixer.class);
	private static final Pattern MAP_KEY_PARSER = Pattern.compile("^\\s*([^#\\s]+)\\s*#\\s*(.+)\\s*$");

	private final Logger output;
	private final Map<CmfObject.Archetype, Map<String, String>> idMap;

	private static Map<CmfObject.Archetype, Map<String, String>> parseProperties(Properties p) {
		Set<CmfObject.Archetype> keys = EnumSet.noneOf(CmfObject.Archetype.class);
		Map<CmfObject.Archetype, Map<String, String>> idMap = new EnumMap<>(CmfObject.Archetype.class);
		if ((p != null) && !p.isEmpty()) {
			for (String key : p.stringPropertyNames()) {
				final String fixedName = p.getProperty(key);
				Matcher matcher = DefaultNameFixer.MAP_KEY_PARSER.matcher(key);
				if (!matcher.matches()) {
					continue;
				}
				final String T = matcher.group(1);
				final CmfObject.Archetype t;
				try {
					t = CmfObject.Archetype.decode(T);
				} catch (Exception e) {
					if (DefaultNameFixer.LOG != null) {
						DefaultNameFixer.LOG.warn("Unsupported object type found [{}] in key [{}] (value = [{}])", T,
							key, fixedName, e);
					}
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
		}
		for (CmfObject.Archetype t : keys) {
			Map<String, String> m = idMap.get(t);
			m = Tools.freezeMap(m);
			idMap.put(t, m);
		}
		return idMap;
	}

	public DefaultNameFixer(Properties p) {
		this(null, p);
	}

	public DefaultNameFixer(Logger output, Properties p) {
		this(output, DefaultNameFixer.parseProperties(p));
	}

	public DefaultNameFixer(Logger output, Map<CmfObject.Archetype, Map<String, String>> idMap) {
		this.output = output;
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
	public final String fixName(CmfObject.Archetype type, String objectId, String historyId) {
		String fixedName = null;
		// Doing the mapping first, and then the property allows us
		// to override whatever was originally set for this object
		Map<String, String> mappings = getMappings(type);
		if ((mappings != null) && !mappings.isEmpty()) {
			fixedName = mappings.get(objectId);
			if (fixedName == null) {
				// No fix for the specific object? What about the history as a whole?
				fixedName = mappings.get(historyId);
			}
		}
		return fixedName;
	}

	@Override
	public final String fixName(CmfObject<CmfValue> dataObject) {
		if (dataObject == null) { return null; }
		String fixedName = fixName(dataObject.getType(), dataObject.getId(), dataObject.getHistoryId());
		if (StringUtils.isEmpty(fixedName)) {
			CmfProperty<CmfValue> prop = dataObject.getProperty(IntermediateProperty.FIXED_NAME);
			if ((prop != null) && prop.hasValues()) {
				fixedName = prop.getValue().asString();
			}
		}
		return fixedName;
	}

	@Override
	public void nameFixed(CmfObject<CmfValue> dataObject, String oldName, String newName) {
		this.output.info("Renamed {} with ID[{}] from [{}] to [{}]", dataObject.getType(), dataObject.getId(), oldName,
			newName);
	}

	@Override
	public boolean handleException(Exception e) {
		return false;
	}
}