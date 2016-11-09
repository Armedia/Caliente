package com.armedia.caliente.cli.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.armedia.commons.utilities.Tools;

public final class CommandLine implements Iterable<ParameterValue> {

	protected static final String[] NO_PARAM = {};

	private final Map<ParameterValue, List<String>> values;
	private final List<String> remainingParameters;

	CommandLine(List<ParameterData> parsedParameters, List<String> remainingParameters) {
		Map<ParameterValue, List<String>> values = new TreeMap<>();
		if ((parsedParameters != null) && !parsedParameters.isEmpty()) {
			for (ParameterData p : parsedParameters) {
				if (p.parameter == null) {
					continue;
				}
				values.put(new ParameterValue(this, p.parameter), Tools.freezeCopy(p.values, true));
			}
		}
		this.values = Tools.freezeMap(values);
		this.remainingParameters = Tools.freezeCopy(remainingParameters, true);
	}

	protected final void assertValid(ParameterValue param) {
		Objects.requireNonNull(param, "Must provide a parameter whose presence to check for");
		if (param.getCLI() != this) { throw new IllegalArgumentException(
			"The given parameter is not associated to this command-line interface"); }
	}

	@Override
	public final Iterator<ParameterValue> iterator() {
		return this.values.keySet().iterator();
	}

	final boolean isPresent(ParameterValue param) {
		assertValid(param);
		return this.values.containsKey(param);
	}

	final Boolean getBoolean(ParameterValue param) {
		assertValid(param);
		String s = getString(param);
		return (s != null ? Tools.toBoolean(s) : null);
	}

	final boolean getBoolean(ParameterValue param, boolean def) {
		assertValid(param);
		Boolean v = getBoolean(param);
		return (v != null ? v.booleanValue() : def);
	}

	final List<Boolean> getAllBooleans(ParameterValue param) {
		assertValid(param);
		List<String> l = getAllStrings(param);
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Boolean> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Tools.toBoolean(s));
		}
		return Tools.freezeList(r);
	}

	final Integer getInteger(ParameterValue param) {
		assertValid(param);
		String s = getString(param);
		return (s != null ? Integer.valueOf(s) : null);
	}

	final int getInteger(ParameterValue param, int def) {
		assertValid(param);
		Integer v = getInteger(param);
		return (v != null ? v.intValue() : def);
	}

	final List<Integer> getAllIntegers(ParameterValue param) {
		assertValid(param);
		List<String> l = getAllStrings(param);
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Integer> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Integer.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	final Double getDouble(ParameterValue param) {
		assertValid(param);
		String s = getString(param);
		return (s != null ? Double.valueOf(s) : null);
	}

	final double getDouble(ParameterValue param, double def) {
		assertValid(param);
		Double v = getDouble(param);
		return (v != null ? v.doubleValue() : def);
	}

	final List<Double> getAllDoubles(ParameterValue param) {
		assertValid(param);
		List<String> l = getAllStrings(param);
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Double> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Double.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	final String getString(ParameterValue param, String def) {
		assertValid(param);
		final String v = getString(param);
		return (v != null ? v : def);
	}

	final String getString(ParameterValue param) {
		List<String> v = getAllStrings(param);
		if ((v == null) || v.isEmpty()) { return null; }
		return v.get(0);
	}

	final List<String> getAllStrings(ParameterValue param) {
		assertValid(param);
		return this.values.get(param);
	}

	public List<String> getRemainingParameters() {
		return this.remainingParameters;
	}
}