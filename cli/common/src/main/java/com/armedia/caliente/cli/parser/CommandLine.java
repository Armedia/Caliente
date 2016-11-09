package com.armedia.caliente.cli.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public abstract class CommandLine implements Iterable<Parameter> {

	protected static final String[] NO_PARAM = {};

	private final Map<Character, Parameter> shortOptions = new TreeMap<Character, Parameter>();
	private final Map<String, Parameter> longOptions = new TreeMap<String, Parameter>();
	private final List<Parameter> parameters = new ArrayList<Parameter>();

	protected abstract void parseImpl(Boolean strict, String... parameters) throws Exception;

	protected abstract void addParameter(String key, ParameterDefinition def) throws Exception;

	protected abstract void removeParameter(String key);

	protected abstract boolean checkPresent(Parameter param);

	protected abstract String getStringValue(Parameter param);

	protected abstract List<String> getAllStringValues(Parameter param);

	public abstract List<String> getRemainingParameters();

	public final void parse(String... parameters) throws Exception {
		if (parameters == null) {
			parameters = CommandLine.NO_PARAM;
		}
		parse(false, parameters);
	}

	public final void parseStrict(String... parameters) throws Exception {
		if (parameters == null) {
			parameters = CommandLine.NO_PARAM;
		}
		parse(true, parameters);
	}

	public final synchronized void parse(Boolean strict, String... parameters) throws Exception {
		if (parameters == null) {
			parameters = CommandLine.NO_PARAM;
		}
		parseImpl(strict, parameters);
	}

	private boolean isShortOptionValid(Character shortOpt) {
		if (shortOpt == null) { return false; }
		final char c = shortOpt.charValue();
		if (!Character.isLetterOrDigit(c)) { return false; }
		return true;
	}

	private boolean isLongOptionValid(String longOpt) {
		if (longOpt == null) { return false; }
		if (StringUtils.containsWhitespace(longOpt)) { return false; }
		if (StringUtils.isEmpty(longOpt)) { return false; }
		return true;
	}

	protected final void assertValid(Parameter param) {
		Objects.requireNonNull(param, "Must provide a parameter whose presence to check for");
		if (param.getCLI() != this) { throw new IllegalArgumentException(
			"The given parameter is not associated to this command-line interface"); }
	}

	protected final void assertValid(ParameterDefinition def) {
		Objects.requireNonNull(def, "Must provide a parameter definition");

		final Character shortOpt = def.getShortOpt();
		final boolean hasShortOpt = (shortOpt != null);

		final String longOpt = def.getLongOpt();
		final boolean hasLongOpt = (longOpt != null);

		if (!hasShortOpt && !hasLongOpt) { throw new IllegalArgumentException(
			"The given parameter definition has neither a short or a long option"); }
		if (hasShortOpt && !isShortOptionValid(shortOpt)) { throw new IllegalArgumentException(
			String.format("The short option value [%s] is not valid", shortOpt)); }
		if (hasLongOpt && !isLongOptionValid(longOpt)) { throw new IllegalArgumentException(
			String.format("The long option value [%s] is not valid", longOpt)); }
	}

	public final synchronized Parameter addParameter(ParameterDefinition def) throws Exception {
		assertValid(def);
		final Character shortOpt = def.getShortOpt();
		if ((shortOpt != null) && this.shortOptions.containsKey(shortOpt)) {
			// Duplicate
		}
		final String longOpt = def.getLongOpt();
		if ((longOpt != null) && this.longOptions.containsKey(longOpt)) {
			// Duplicate
		}
		Parameter ret = new Parameter(this, def);
		if (shortOpt != null) {
			this.shortOptions.put(shortOpt, ret);
		}
		if (longOpt != null) {
			this.longOptions.put(longOpt, ret);
		}
		this.parameters.add(ret);
		return ret;
	}

	@Override
	public final Iterator<Parameter> iterator() {
		return Tools.freezeList(this.parameters).iterator();
	}

	final Boolean getBoolean(Parameter param) {
		assertValid(param);
		String s = getString(param);
		return (s != null ? Tools.toBoolean(s) : null);
	}

	final boolean getBoolean(Parameter param, boolean def) {
		assertValid(param);
		Boolean v = getBoolean(param);
		return (v != null ? v.booleanValue() : def);
	}

	final List<Boolean> getAllBoolean(Parameter param) {
		assertValid(param);
		List<String> l = getAllString(param);
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Boolean> r = new ArrayList<Boolean>(l.size());
		for (String s : l) {
			r.add(Tools.toBoolean(s));
		}
		return Tools.freezeList(r);
	}

	final Integer getInteger(Parameter param) {
		assertValid(param);
		String s = getString(param);
		return (s != null ? Integer.valueOf(s) : null);
	}

	final int getInteger(Parameter param, int def) {
		assertValid(param);
		Integer v = getInteger(param);
		return (v != null ? v.intValue() : def);
	}

	final List<Integer> getAllInteger(Parameter param) {
		assertValid(param);
		List<String> l = getAllString(param);
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Integer> r = new ArrayList<Integer>(l.size());
		for (String s : l) {
			r.add(Integer.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	final Double getDouble(Parameter param) {
		assertValid(param);
		String s = getString(param);
		return (s != null ? Double.valueOf(s) : null);
	}

	final double getDouble(Parameter param, double def) {
		assertValid(param);
		Double v = getDouble(param);
		return (v != null ? v.doubleValue() : def);
	}

	final List<Double> getAllDouble(Parameter param) {
		assertValid(param);
		List<String> l = getAllString(param);
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Double> r = new ArrayList<Double>(l.size());
		for (String s : l) {
			r.add(Double.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	final String getString(Parameter param, String def) {
		assertValid(param);
		final String v = getString(param);
		return (v != null ? v : def);
	}

	final String getString(Parameter param) {
		assertValid(param);
		return getStringValue(param);
	}

	final List<String> getAllString(Parameter param) {
		assertValid(param);
		return getAllStringValues(param);
	}

	final boolean isPresent(Parameter param) {
		assertValid(param);
		return checkPresent(param);
	}

	static Option buildOption(ParameterDefinition def) {
		Builder b = (def.getShortOpt() == null ? Option.builder() : Option.builder(def.getShortOpt().toString()));
		b.required(def.isRequired());
		if (def.getLongOpt() != null) {
			b.longOpt(def.getLongOpt());
		}
		if (def.getDescription() != null) {
			b.desc(def.getDescription());
		}
		if (def.getValueSep() != null) {
			b.valueSeparator(def.getValueSep());
		}
		final int paramCount = def.getParameterCount();
		if (paramCount != 0) {
			if (def.getArgName() != null) {
				b.argName(def.getArgName());
			}
			b.optionalArg(def.isOptionalArg());
			if (paramCount < 0) {
				b.hasArgs();
			}
			if (paramCount > 0) {
				b.numberOfArgs(paramCount);
			}
		}
		return b.build();
	}
}