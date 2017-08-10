package com.armedia.caliente.cli.parser.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.armedia.caliente.cli.parser.Parameter;

final class DefaultParameterData implements ParameterData {

	private final String name;
	private final Map<String, Parameter> parameters = new HashMap<>();
	private final Map<String, List<String>> namedValues = new HashMap<>();
	private final List<String> positionals = new ArrayList<>();

	DefaultParameterData() {
		this(null);
	}

	DefaultParameterData(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Collection<Parameter> getParameters() {
		return this.parameters.values();
	}

	@Override
	public void addNamedValues(Parameter parameter, List<String> values) throws TooManyParameterValuesException {
		final String key = parameter.getKey();
		this.parameters.put(key, parameter);
		if (values == null) {
			values = Collections.emptyList();
		}
		List<String> l = this.namedValues.get(key);
		if (l == null) {
			l = new ArrayList<>();
			this.namedValues.put(key, l);
		}
		for (String s : values) {
			if (s != null) {
				l.add(s);
			}
		}
	}

	@Override
	public void setPositionalValues(List<String> values) {
		if (values == null) {
			values = Collections.emptyList();
		}
		this.positionals.clear();
		for (String s : values) {
			if (s != null) {
				this.positionals.add(s);
			}
		}
	}

	@Override
	public List<String> getValues(Parameter parameter) {
		return this.namedValues.get(parameter.getKey());
	}

	@Override
	public List<String> getPositionalValues() {
		return this.positionals;
	}

	@Override
	public String toString() {
		return String.format("DefaultParameterData [name=%s, parameters=%s, namedValues=%s, positionals=%s]", this.name,
			this.parameters, this.namedValues, this.positionals);
	}
}