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

public final class OptionValues implements Iterable<OptionValue> {

	private final Map<Character, OptionValue> shortOptions = new TreeMap<>();
	private final Map<String, OptionValue> longOptions = new TreeMap<>();
	private final Map<String, OptionValue> optionValues = new TreeMap<>();

	private final Map<String, List<Collection<String>>> occurrences = new TreeMap<>();
	private final Map<String, List<String>> values = new HashMap<>();

	OptionValues() {
		// Do nothing...
	}

	private String getValidKey(Option param) {
		Objects.requireNonNull(param, "Must provide an option whose presence to check for");
		String key = param.getKey();
		if (key == null) { throw new IllegalArgumentException(
			"The given option definition does not define a valid key"); }
		if (OptionValue.class.isInstance(param)) {
			OptionValue p = OptionValue.class.cast(param);
			if (p.getOptionValues() != this) { throw new IllegalArgumentException(
				"The given option is not associated to this OptionValues instance"); }
		}
		return key;
	}

	void add(Option p) {
		add(p, null);
	}

	void add(Option p, Collection<String> values) {
		if (p == null) { throw new IllegalArgumentException("Must provide a non-null option"); }
		if (values == null) {
			values = Collections.emptyList();
		}

		final String key = p.getKey();
		OptionValue existing = this.optionValues.get(key);
		if (existing == null) {
			// This is a new option value, so we add the stuff that's needed
			existing = new OptionValue(this, p);
			this.optionValues.put(key, existing);
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
		occurrences.add(Tools.freezeCollection(new LinkedList<>(values), true));

		List<String> l = this.values.get(key);
		if (l == null) {
			l = new LinkedList<>();
			this.values.put(key, l);
		}
		l.addAll(values);
	}

	@Override
	public Iterator<OptionValue> iterator() {
		return new ArrayList<>(this.optionValues.values()).iterator();
	}

	public final Iterable<OptionValue> shortOptions() {
		return Tools.freezeList(new ArrayList<>(this.shortOptions.values()));
	}

	public final OptionValue getOption(char shortOpt) {
		return this.shortOptions.get(shortOpt);
	}

	public final boolean hasOption(char shortOpt) {
		return this.shortOptions.containsKey(shortOpt);
	}

	public final Iterable<OptionValue> longOptions() {
		return Tools.freezeList(new ArrayList<>(this.longOptions.values()));
	}

	public final OptionValue getOption(String longOpt) {
		return this.longOptions.get(longOpt);
	}

	public final boolean hasOption(String longOpt) {
		return this.longOptions.containsKey(longOpt);
	}

	public final boolean isDefined(Option option) {
		return (getOptionValue(option) != null);
	}

	public final OptionValue getOptionValue(Option option) {
		if (option == null) { throw new IllegalArgumentException("Must provide an option definition to retrieve"); }
		return getOptionByKey(option.getKey());
	}

	protected final OptionValue getOptionByKey(String key) {
		if (key == null) { throw new IllegalArgumentException("Must provide a key to search for"); }
		return this.optionValues.get(key);
	}

	public final Boolean getBoolean(Option param) {
		String s = getString(param);
		return (s != null ? Tools.toBoolean(s) : null);
	}

	public final Boolean getBoolean(Option param, Boolean def) {
		Boolean v = getBoolean(param);
		return (v != null ? v.booleanValue() : def);
	}

	public final List<Boolean> getAllBooleans(Option param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Boolean> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Tools.toBoolean(s));
		}
		return Tools.freezeList(r);
	}

	public final Integer getInteger(Option param) {
		String s = getString(param);
		return (s != null ? Integer.valueOf(s) : null);
	}

	public final Integer getInteger(Option param, Integer def) {
		Integer v = getInteger(param);
		return (v != null ? v.intValue() : def);
	}

	public final List<Integer> getAllIntegers(Option param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Integer> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Integer.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	public final Long getLong(Option param) {
		String s = getString(param);
		return (s != null ? Long.valueOf(s) : null);
	}

	public final Long getLong(Option param, Long def) {
		Long v = getLong(param);
		return (v != null ? v.longValue() : def);
	}

	public final List<Long> getAllLongs(Option param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Long> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Long.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	public final Float getFloat(Option param) {
		String s = getString(param);
		return (s != null ? Float.valueOf(s) : null);
	}

	public final Float getFloat(Option param, Float def) {
		Float v = getFloat(param);
		return (v != null ? v.floatValue() : def);
	}

	public final List<Float> getAllFloats(Option param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Float> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Float.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	public final Double getDouble(Option param) {
		String s = getString(param);
		return (s != null ? Double.valueOf(s) : null);
	}

	public final Double getDouble(Option param, Double def) {
		Double v = getDouble(param);
		return (v != null ? v.doubleValue() : def);
	}

	public final List<Double> getAllDoubles(Option param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Double> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Double.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	public final String getString(Option param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return param.getDefault(); }
		return l.get(0);
	}

	public final String getString(Option param, String def) {
		List<String> l = getAllStrings(param);
		if (l == null) { return def; }
		return l.get(0);
	}

	public final List<String> getAllStrings(Option param) {
		List<String> v = getAllStrings(param, null);
		if (v == null) { return param.getDefaults(); }
		return v;
	}

	public final List<String> getAllStrings(Option param, List<String> def) {
		List<String> v = this.values.get(getValidKey(param));
		if (v == null) { return def; }
		return v;
	}

	public final boolean isPresent(Option param) {
		return this.values.containsKey(getValidKey(param));
	}

	public int getOccurrences(Option param) {
		List<Collection<String>> occurrences = this.occurrences.get(getValidKey(param));
		if (occurrences == null) { return 0; }
		return occurrences.size();
	}

	public Collection<String> getOccurrenceValues(Option param, int o) {
		List<Collection<String>> occurrences = this.occurrences.get(getValidKey(param));
		if (occurrences == null) { return null; }
		if ((o < 0) || (o >= occurrences.size())) { throw new IndexOutOfBoundsException(); }
		return occurrences.get(o);
	}

	public int getValueCount(Option param) {
		List<Collection<String>> occurrences = this.occurrences.get(getValidKey(param));
		if (occurrences == null) { return 0; }
		int v = 0;
		for (Collection<String> c : occurrences) {
			v += c.size();
		}
		return v;
	}

	public boolean isDefined(OptionWrapper paramDel) {
		return isDefined(Option.unwrap(paramDel));
	}

	public OptionValue getOption(OptionWrapper paramDel) {
		return getOptionValue(Option.unwrap(paramDel));
	}

	public Boolean getBoolean(OptionWrapper paramDel) {
		return getBoolean(Option.unwrap(paramDel));
	}

	public Boolean getBoolean(OptionWrapper paramDel, Boolean def) {
		return getBoolean(Option.unwrap(paramDel), def);
	}

	public List<Boolean> getAllBooleans(OptionWrapper paramDel) {
		return getAllBooleans(Option.unwrap(paramDel));
	}

	public Integer getInteger(OptionWrapper paramDel) {
		return getInteger(Option.unwrap(paramDel));
	}

	public Integer getInteger(OptionWrapper paramDel, Integer def) {
		return getInteger(Option.unwrap(paramDel), def);
	}

	public List<Integer> getAllIntegers(OptionWrapper paramDel) {
		return getAllIntegers(Option.unwrap(paramDel));
	}

	public Long getLong(OptionWrapper paramDel) {
		return getLong(Option.unwrap(paramDel));
	}

	public Long getLong(OptionWrapper paramDel, Long def) {
		return getLong(Option.unwrap(paramDel), def);
	}

	public List<Long> getAllLongs(OptionWrapper paramDel) {
		return getAllLongs(Option.unwrap(paramDel));
	}

	public Float getFloat(OptionWrapper paramDel) {
		return getFloat(Option.unwrap(paramDel));
	}

	public Float getFloat(OptionWrapper paramDel, Float def) {
		return getFloat(Option.unwrap(paramDel), def);
	}

	public List<Float> getAllFloats(OptionWrapper paramDel) {
		return getAllFloats(Option.unwrap(paramDel));
	}

	public Double getDouble(OptionWrapper paramDel) {
		return getDouble(Option.unwrap(paramDel));
	}

	public Double getDouble(OptionWrapper paramDel, Double def) {
		return getDouble(Option.unwrap(paramDel), def);
	}

	public List<Double> getAllDoubles(OptionWrapper paramDel) {
		return getAllDoubles(Option.unwrap(paramDel));
	}

	public String getString(OptionWrapper paramDel) {
		return getString(Option.unwrap(paramDel));
	}

	public String getString(OptionWrapper paramDel, String def) {
		return getString(Option.unwrap(paramDel), def);
	}

	public List<String> getAllStrings(OptionWrapper paramDel) {
		return getAllStrings(Option.unwrap(paramDel));
	}

	public List<String> getAllStrings(OptionWrapper paramDel, List<String> def) {
		return getAllStrings(Option.unwrap(paramDel), def);
	}

	public boolean isPresent(OptionWrapper paramDel) {
		return isPresent(Option.unwrap(paramDel));
	}

	public int getOccurrences(OptionWrapper param) {
		return getOccurrences(Option.unwrap(param));
	}

	public Collection<String> getOccurrenceValues(OptionWrapper param, int occurrence) {
		return getOccurrenceValues(Option.unwrap(param), occurrence);
	}

	public int getValueCount(OptionWrapper param) {
		return getValueCount(Option.unwrap(param));
	}
}