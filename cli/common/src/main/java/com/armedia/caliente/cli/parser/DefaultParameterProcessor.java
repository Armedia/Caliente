package com.armedia.caliente.cli.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultParameterProcessor extends MutableParameterSet implements ParameterProcessor {

	private final Map<String, List<String>> namedValues = new HashMap<>();
	private final List<String> positionals = new ArrayList<>();

	public DefaultParameterProcessor(ParameterProcessor other) {
		super(other);
		List<String> positionals = other.getPositionalValues();
		if (positionals != null) {
			this.positionals.addAll(positionals);
		}
		for (Parameter p : other.getOrderedParameters(null)) {
			List<String> l = other.getNamedValues(p);
			if (l != null) {
				this.namedValues.put(BaseParameter.calculateKey(p), new ArrayList<>(l));
			}
		}
	}

	public DefaultParameterProcessor(String name) {
		super(name);
	}

	@Override
	public void addNamedValues(Parameter parameter, List<String> values) throws TooManyParameterValuesException {
		final String key = BaseParameter.calculateKey(parameter);
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
	public List<String> getNamedValues(Parameter parameter) {
		return this.namedValues.get(BaseParameter.calculateKey(parameter));
	}

	@Override
	public List<String> getPositionalValues() {
		return this.positionals;
	}

	@Override
	public ParameterErrorPolicy getErrorPolicy() {
		return null;
	}
}