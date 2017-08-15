package com.armedia.caliente.cli.parser.token;

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

import com.armedia.caliente.cli.ImmutableParameter;
import com.armedia.caliente.cli.MutableParameter;
import com.armedia.caliente.cli.Parameter;
import com.armedia.caliente.cli.parser.DuplicateParameterException;
import com.armedia.caliente.cli.parser.InvalidParameterException;

public class MutableParameterSet implements ParameterSubSchema, Cloneable {

	protected static final Pattern VALID_SHORT = Pattern.compile("^[[\\p{Punct}&&[^-]]\\p{Alnum}]$");

	private String description;

	private final Map<Character, Parameter> shortOpts = new HashMap<>();
	private final Map<String, Parameter> longOpts = new HashMap<>();
	private final Map<String, Parameter> parameters = new HashMap<>();

	public MutableParameterSet() {
		this(null);
	}

	public MutableParameterSet(ParameterSubSchema other) {
		if (other != null) {
			this.description = other.getDescription();
			for (Parameter p : other.getParameters(null)) {
				p = new MutableParameter(p);
				Character s = p.getShortOpt();
				if (s != null) {
					this.shortOpts.put(s, p);
				}
				String l = p.getLongOpt();
				if (l != null) {
					this.longOpts.put(l, p);
				}
				this.parameters.put(p.getKey(), p);
			}
		}
	}

	@Override
	public MutableParameterSet clone() {
		return new MutableParameterSet(this);
	}

	public ImmutableParameterSet freezeCopy() {
		return new ImmutableParameterSet(this);
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
				if (Parameter.isEquivalent(shortParam, def)) {
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
				if (Parameter.isEquivalent(longParam, def)) {
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