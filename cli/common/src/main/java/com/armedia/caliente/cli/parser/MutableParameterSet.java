package com.armedia.caliente.cli.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class MutableParameterSet implements ParameterSet, Cloneable {

	protected static final Pattern VALID_SHORT = Pattern.compile("^[[\\p{Punct}&&[^-]]\\p{Alnum}]$");

	private String name = null;
	private String description;

	private final Map<Character, Parameter> shortOpts = new HashMap<>();
	private final Map<String, Parameter> longOpts = new HashMap<>();
	private final Map<String, Parameter> parameters = new HashMap<>();

	public MutableParameterSet(ParameterSet other) {
		this.name = other.getName();
		this.description = other.getDescription();
		for (Character s : other.getShortOptions()) {
			Parameter p = other.getParameter(s);
			this.shortOpts.put(s, p);
			this.parameters.put(p.getKey(), p);
		}
		for (String l : other.getLongOptions()) {
			Parameter p = other.getParameter(l);
			this.longOpts.put(l, p);
			this.parameters.put(p.getKey(), p);
		}
	}

	public MutableParameterSet(String name) {
		if (!validateLong(name)) { throw new IllegalArgumentException(
			String.format("The string [%s] is not a valid parameter set name", name)); }
		this.name = name;
	}

	@Override
	public MutableParameterSet clone() {
		return new MutableParameterSet(this);
	}

	public ParameterSet freeze() {
		return new ImmutableParameterSet(this);
	}

	@Override
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		if (!validateLong(name)) { throw new IllegalArgumentException(
			String.format("The string [%s] is not a valid parameter set name", name)); }
		this.name = name;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private boolean validateShort(Character shortOpt) {
		if (!MutableParameterSet.VALID_SHORT.matcher(shortOpt.toString()).matches()) { return false; }
		if (Tools.equals(shortOpt, TokenProcessor.DEFAULT_PARAMETER_MARKER)) { return false; }
		if (!isShortOptionValid(shortOpt)) { return false; }
		return true;
	}

	protected boolean isShortOptionValid(Character shortOpt) {
		return (shortOpt != null);
	}

	protected boolean validateLong(String name) {
		if (StringUtils.isEmpty(name)) { return false; }
		if (StringUtils.containsWhitespace(name)) { return false; }
		if (!isLongOptionValid(name)) { return false; }
		return true;
	}

	protected boolean isLongOptionValid(String longOpt) {
		return (longOpt != null);
	}

	@Override
	public boolean hasParameter(char shortOpt) {
		return this.shortOpts.containsKey(shortOpt);
	}

	@Override
	public Parameter getParameter(char shortOpt) {
		return this.shortOpts.get(shortOpt);
	}

	public Parameter removeParameter(char shortOpt) {
		return this.shortOpts.remove(shortOpt);
	}

	@Override
	public Set<Character> getShortOptions() {
		return new LinkedHashSet<>(this.shortOpts.keySet());
	}

	@Override
	public boolean hasParameter(String longOpt) {
		return this.longOpts.containsKey(longOpt);
	}

	@Override
	public Parameter getParameter(String longOpt) {
		return this.longOpts.get(longOpt);
	}

	public Parameter removeParameter(String longOpt) {
		return this.longOpts.remove(longOpt);
	}

	@Override
	public Set<String> getLongOptions() {
		return new LinkedHashSet<>(this.longOpts.keySet());
	}

	public void addParameter(Parameter def) throws InvalidParameterException, DuplicateParameterException {
		if (def == null) { throw new InvalidParameterException("Parameter definition may not be null"); }

		final Character shortOpt = def.getShortOpt();
		final boolean hasShortOpt = (shortOpt != null);

		final String longOpt = def.getLongOpt();
		final boolean hasLongOpt = (longOpt != null);

		if (!hasShortOpt && !hasLongOpt) { throw new InvalidParameterException(
			"The given parameter definition has neither a short or a long option"); }
		if (hasShortOpt && !validateShort(shortOpt)) { throw new InvalidParameterException(
			String.format("The short option value [%s] is not valid", shortOpt)); }
		if (hasLongOpt && !validateLong(longOpt)) { throw new InvalidParameterException(
			String.format("The long option value [%s] is not valid", longOpt)); }

		Parameter shortParam = null;
		if (shortOpt != null) {
			shortParam = this.shortOpts.get(shortOpt);
			if (shortParam != null) {
				if (BaseParameter.isEquivalent(shortParam, def)) {
					// It's the same parameter, so we can safely return the existing one
					return;
				}
				// The commandLineParameters aren't equal...so...this is an error
				throw new DuplicateParameterException(
					String.format("The new parameter definition for short option [%s] collides with an existing one",
						shortOpt),
					shortParam, def);
			}
		}

		Parameter longParam = null;
		if (longOpt != null) {
			longParam = this.longOpts.get(longOpt);
			if (longParam != null) {
				if (BaseParameter.isEquivalent(longParam, def)) {
					// It's the same parameter, so we can safely return the existing one
					return;
				}
				// The commandLineParameters aren't equal...so...this is an error
				throw new DuplicateParameterException(String
					.format("The new parameter definition for long option [%s] collides with an existing one", longOpt),
					longParam, def);
			}
		}

		Parameter ret = new ImmutableParameter(def);
		if (shortOpt != null) {
			this.shortOpts.put(shortOpt, ret);
		}
		if (longOpt != null) {
			this.longOpts.put(longOpt, ret);
		}
		this.parameters.put(ret.getKey(), ret);
	}

	@Override
	public Collection<Parameter> getParameters(Comparator<? super Parameter> c) {
		List<Parameter> ret = new ArrayList<>(this.parameters.values());
		if (c != null) {
			Collections.sort(ret, c);
		}
		return ret;
	}
}