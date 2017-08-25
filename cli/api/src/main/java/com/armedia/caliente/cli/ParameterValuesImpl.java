package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.armedia.commons.utilities.Tools;

public final class ParameterValuesImpl implements ParameterValues {

	private final ParameterScheme scheme;

	private final Map<Character, ParameterValue> shortOptions = new TreeMap<>();
	private final Map<String, ParameterValue> longOptions = new TreeMap<>();
	private final Map<String, ParameterValue> parameterValues = new TreeMap<>();

	private final Map<String, List<Collection<String>>> occurrences = new TreeMap<>();
	private final Map<String, List<String>> values = new HashMap<>();

	public ParameterValuesImpl(ParameterScheme scheme) {
		this.scheme = scheme;
	}

	private String getValidKey(Parameter param) {
		Objects.requireNonNull(param, "Must provide a parameter whose presence to check for");
		String key = param.getKey();
		if (key == null) { throw new IllegalArgumentException(
			"The given parameter definition does not define a valid key"); }
		if (ParameterValue.class.isInstance(param)) {
			ParameterValue p = ParameterValue.class.cast(param);
			if (p.getParameterValues() != this) { throw new IllegalArgumentException(
				"The given parameter is not associated to this ParameterValues instance"); }
		}
		return key;
	}

	public final void add(Parameter p, Collection<String> values) {
		if (p == null) { throw new IllegalArgumentException("Must provide a non-null parameter"); }
		if (values == null) {
			values = Collections.emptyList();
		}

		final String key = p.getKey();
		ParameterValue existing = this.parameterValues.get(key);
		if (existing == null) {
			// This is a new parameter value, so we add the stuff that's needed
			existing = new ParameterValue(this, p);
			this.parameterValues.put(key, existing);
			Character shortOpt = p.getShortOpt();
			if (shortOpt != null) {
				this.shortOptions.put(shortOpt, existing);
			}
			String longOpt = p.getLongOpt();
			if (longOpt != null) {
				this.longOptions.put(longOpt, existing);
			}
		}

		List<Collection<String>> occurrences = this.occurrences.get(key);
		if (occurrences == null) {
			occurrences = new LinkedList<>();
			this.occurrences.put(key, occurrences);
		}
		occurrences.add(Tools.freezeCollection(values, true));

		List<String> l = this.values.get(key);
		if (l == null) {
			l = new LinkedList<>();
			this.values.put(key, l);
		}
		l.addAll(values);
	}

	@Override
	public Iterator<ParameterValue> iterator() {
		return new ArrayList<>(this.parameterValues.values()).iterator();
	}

	@Override
	public ParameterScheme getParameterScheme() {
		return this.scheme;
	}

	@Override
	public final Iterable<ParameterValue> shortOptions() {
		return Tools.freezeList(new ArrayList<>(this.shortOptions.values()));
	}

	@Override
	public final ParameterValue getParameter(char shortOpt) {
		return this.shortOptions.get(shortOpt);
	}

	@Override
	public final boolean hasParameter(char shortOpt) {
		return this.shortOptions.containsKey(shortOpt);
	}

	@Override
	public final Iterable<ParameterValue> longOptions() {
		return Tools.freezeList(new ArrayList<>(this.longOptions.values()));
	}

	@Override
	public final ParameterValue getParameter(String longOpt) {
		return this.longOptions.get(longOpt);
	}

	@Override
	public final boolean hasParameter(String longOpt) {
		return this.longOptions.containsKey(longOpt);
	}

	@Override
	public final boolean isDefined(Parameter parameter) {
		return (getParameter(parameter) != null);
	}

	@Override
	public final ParameterValue getParameter(Parameter parameter) {
		if (parameter == null) { throw new IllegalArgumentException(
			"Must provide a parameter definition to retrieve"); }
		return getParameterByKey(parameter.getKey());
	}

	protected final ParameterValue getParameterByKey(String key) {
		if (key == null) { throw new IllegalArgumentException("Must provide a key to search for"); }
		return this.parameterValues.get(key);
	}

	@Override
	public final Boolean getBoolean(Parameter param) {
		String s = getString(param);
		return (s != null ? Tools.toBoolean(s) : null);
	}

	@Override
	public final Boolean getBoolean(Parameter param, Boolean def) {
		Boolean v = getBoolean(param);
		return (v != null ? v.booleanValue() : def);
	}

	@Override
	public final List<Boolean> getAllBooleans(Parameter param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Boolean> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Tools.toBoolean(s));
		}
		return Tools.freezeList(r);
	}

	@Override
	public final Integer getInteger(Parameter param) {
		String s = getString(param);
		return (s != null ? Integer.valueOf(s) : null);
	}

	@Override
	public final Integer getInteger(Parameter param, Integer def) {
		Integer v = getInteger(param);
		return (v != null ? v.intValue() : def);
	}

	@Override
	public final List<Integer> getAllIntegers(Parameter param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Integer> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Integer.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	@Override
	public final Long getLong(Parameter param) {
		String s = getString(param);
		return (s != null ? Long.valueOf(s) : null);
	}

	@Override
	public final Long getLong(Parameter param, Long def) {
		Long v = getLong(param);
		return (v != null ? v.longValue() : def);
	}

	@Override
	public final List<Long> getAllLongs(Parameter param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Long> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Long.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	@Override
	public final Float getFloat(Parameter param) {
		String s = getString(param);
		return (s != null ? Float.valueOf(s) : null);
	}

	@Override
	public final Float getFloat(Parameter param, Float def) {
		Float v = getFloat(param);
		return (v != null ? v.floatValue() : def);
	}

	@Override
	public final List<Float> getAllFloats(Parameter param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Float> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Float.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	@Override
	public final Double getDouble(Parameter param) {
		String s = getString(param);
		return (s != null ? Double.valueOf(s) : null);
	}

	@Override
	public final Double getDouble(Parameter param, Double def) {
		Double v = getDouble(param);
		return (v != null ? v.doubleValue() : def);
	}

	@Override
	public final List<Double> getAllDoubles(Parameter param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Double> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Double.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	@Override
	public final String getString(Parameter param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return param.getDefault(); }
		return l.get(0);
	}

	@Override
	public final String getString(Parameter param, String def) {
		final String v = getString(param);
		return (v != null ? v : def);
	}

	@Override
	public final List<String> getAllStrings(Parameter param) {
		List<String> v = this.values.get(getValidKey(param));
		if (v == null) {
			v = param.getDefaults();
		}
		return v;
	}

	@Override
	public final boolean isPresent(Parameter param) {
		return this.values.containsKey(getValidKey(param));
	}

	@Override
	public int getOccurrences(Parameter param) {
		List<Collection<String>> occurrences = this.occurrences.get(getValidKey(param));
		if (occurrences == null) { return 0; }
		return occurrences.size();
	}

	@Override
	public boolean isDefined(ParameterWrapper paramDel) {
		return isDefined(Parameter.unwrap(paramDel));
	}

	@Override
	public ParameterValue getParameter(ParameterWrapper paramDel) {
		return getParameter(Parameter.unwrap(paramDel));
	}

	@Override
	public Boolean getBoolean(ParameterWrapper paramDel) {
		return getBoolean(Parameter.unwrap(paramDel));
	}

	@Override
	public Boolean getBoolean(ParameterWrapper paramDel, Boolean def) {
		return getBoolean(Parameter.unwrap(paramDel), def);
	}

	@Override
	public List<Boolean> getAllBooleans(ParameterWrapper paramDel) {
		return getAllBooleans(Parameter.unwrap(paramDel));
	}

	@Override
	public Integer getInteger(ParameterWrapper paramDel) {
		return getInteger(Parameter.unwrap(paramDel));
	}

	@Override
	public Integer getInteger(ParameterWrapper paramDel, Integer def) {
		return getInteger(Parameter.unwrap(paramDel), def);
	}

	@Override
	public List<Integer> getAllIntegers(ParameterWrapper paramDel) {
		return getAllIntegers(Parameter.unwrap(paramDel));
	}

	@Override
	public Long getLong(ParameterWrapper paramDel) {
		return getLong(Parameter.unwrap(paramDel));
	}

	@Override
	public Long getLong(ParameterWrapper paramDel, Long def) {
		return getLong(Parameter.unwrap(paramDel), def);
	}

	@Override
	public List<Long> getAllLongs(ParameterWrapper paramDel) {
		return getAllLongs(Parameter.unwrap(paramDel));
	}

	@Override
	public Float getFloat(ParameterWrapper paramDel) {
		return getFloat(Parameter.unwrap(paramDel));
	}

	@Override
	public Float getFloat(ParameterWrapper paramDel, Float def) {
		return getFloat(Parameter.unwrap(paramDel), def);
	}

	@Override
	public List<Float> getAllFloats(ParameterWrapper paramDel) {
		return getAllFloats(Parameter.unwrap(paramDel));
	}

	@Override
	public Double getDouble(ParameterWrapper paramDel) {
		return getDouble(Parameter.unwrap(paramDel));
	}

	@Override
	public Double getDouble(ParameterWrapper paramDel, Double def) {
		return getDouble(Parameter.unwrap(paramDel), def);
	}

	@Override
	public List<Double> getAllDoubles(ParameterWrapper paramDel) {
		return getAllDoubles(Parameter.unwrap(paramDel));
	}

	@Override
	public String getString(ParameterWrapper paramDel) {
		return getString(Parameter.unwrap(paramDel));
	}

	@Override
	public String getString(ParameterWrapper paramDel, String def) {
		return getString(Parameter.unwrap(paramDel), def);
	}

	@Override
	public List<String> getAllStrings(ParameterWrapper paramDel) {
		return getAllStrings(Parameter.unwrap(paramDel));
	}

	@Override
	public boolean isPresent(ParameterWrapper paramDel) {
		return isPresent(Parameter.unwrap(paramDel));
	}

	@Override
	public int getOccurrences(ParameterWrapper param) {
		return getOccurrences(Parameter.unwrap(param));
	}
}