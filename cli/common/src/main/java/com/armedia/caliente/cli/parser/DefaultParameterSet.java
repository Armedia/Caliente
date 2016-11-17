package com.armedia.caliente.cli.parser;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.parser.Parser.ParameterSet;
import com.armedia.commons.utilities.Tools;

public class DefaultParameterSet implements ParameterSet {

	protected static final Pattern VALID_SHORT = Pattern.compile("^[[\\p{Punct}&&[^-]]\\p{Alnum}]$");

	private final String name;

	private final Map<Character, Parameter> shortOpts = new HashMap<>();
	private final Map<String, Parameter> longOpts = new HashMap<>();
	private final Map<String, ParameterSet> subs = new HashMap<>();

	public DefaultParameterSet(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	private boolean validateShort(Character shortOpt) {
		if (!DefaultParameterSet.VALID_SHORT.matcher(shortOpt.toString()).matches()) { return false; }
		if (Tools.equals(shortOpt, Parser.DEFAULT_PARAMETER_MARKER)) { return false; }
		if (!isShortOptionValid(shortOpt)) { return false; }
		return true;
	}

	protected boolean isShortOptionValid(Character shortOpt) {
		return (shortOpt != null);
	}

	private boolean validateLong(String name) {
		if (StringUtils.isEmpty(name)) { return false; }
		if (StringUtils.containsWhitespace(name)) { return false; }
		if (!isLongOptionValid(name)) { return false; }
		return true;
	}

	protected boolean isLongOptionValid(String longOpt) {
		return (longOpt != null);
	}

	@Override
	public ParameterSet getSubcommand(String subName) {
		if (!validateLong(subName)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid subcommand name - it may not be null, the empty string, or contain whitespace",
			subName)); }
		return this.subs.get(subName);
	}

	public boolean hasSubcommand(String subName) {
		if (!validateLong(subName)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid subcommand name - it may not be null, the empty string, or contain whitespace",
			subName)); }
		return this.subs.containsKey(subName);
	}

	public void addSubcommand(String subName, ParameterSet sub) {
		if (!validateLong(subName)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid subcommand name - it may not be null, the empty string, or contain whitespace",
			subName)); }
		if (sub == null) { throw new IllegalArgumentException("Must provide a subcommand parameter set"); }
		this.subs.put(subName, sub);
	}

	public ParameterSet removeSubcommand(String subName) {
		if (!validateLong(subName)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid subcommand name - it may not be null, the empty string, or contain whitespace",
			subName)); }
		return this.subs.remove(subName);
	}

	public boolean hasParameter(char shortOpt) {
		if (!validateShort(shortOpt)) { throw new IllegalArgumentException(
			String.format("The character [%s] is not a valid short option", shortOpt)); }
		return this.shortOpts.containsKey(shortOpt);
	}

	@Override
	public Parameter getParameter(char shortOpt) {
		if (!validateShort(shortOpt)) { throw new IllegalArgumentException(
			String.format("The character [%s] is not a valid short option", shortOpt)); }
		return this.shortOpts.get(shortOpt);
	}

	public Parameter removeParameter(char shortOpt) {
		if (!validateShort(shortOpt)) { throw new IllegalArgumentException(
			String.format("The character [%s] is not a valid short option", shortOpt)); }
		return this.shortOpts.remove(shortOpt);
	}

	public boolean hasParameter(String longOpt) {
		if (!validateLong(longOpt)) { throw new IllegalArgumentException(
			String.format("The string [%s] is not a valid long option name", longOpt)); }
		return this.longOpts.containsKey(longOpt);
	}

	@Override
	public Parameter getParameter(String longOpt) {
		if (!validateLong(longOpt)) { throw new IllegalArgumentException(
			String.format("The string [%s] is not a valid long option name", longOpt)); }
		return this.longOpts.get(longOpt);
	}

	public Parameter removeParameter(String longOpt) {
		if (!validateLong(longOpt)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid long option name - it may not be null, the empty string, or contain whitespace",
			longOpt)); }
		return this.longOpts.remove(longOpt);
	}

	public Collection<Parameter> getParameters() {
		// Return an alphabetically-ordered list of parameters, ordered first by short option,
		// then by long option if there is no short option...
		return Collections.emptyList();
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
				if (shortParam.isEqual(def)) {
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
				if (longParam.isEqual(def)) {
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
	}
}