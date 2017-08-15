package com.armedia.caliente.cli.parser.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.armedia.caliente.cli.ImmutableParameter;
import com.armedia.caliente.cli.Parameter;
import com.armedia.commons.utilities.Tools;

public class ImmutableParameterSet implements ParameterSubSchema {

	private final String description;

	private final Map<Character, Parameter> shortOpts;
	private final Map<String, Parameter> longOpts;
	private final Collection<Parameter> sorted;

	public ImmutableParameterSet(ParameterSubSchema other) {
		this.description = other.getDescription();
		Map<Character, Parameter> shortOpts = new HashMap<>();
		Map<String, Parameter> longOpts = new HashMap<>();
		List<Parameter> parameters = new ArrayList<>();
		for (Parameter p : other.getParameters(ParameterSubSchema.DEFAULT_COMPARATOR)) {
			p = new ImmutableParameter(p);
			Character s = p.getShortOpt();
			if (s != null) {
				shortOpts.put(s, p);
			}
			String l = p.getLongOpt();
			if (l != null) {
				longOpts.put(l, p);
			}
			parameters.add(p);
		}
		Collections.sort(parameters, ParameterSubSchema.DEFAULT_COMPARATOR);
		this.shortOpts = Tools.freezeMap(shortOpts);
		this.longOpts = Tools.freezeMap(longOpts);
		this.sorted = Tools.freezeList(parameters);
	}

	public MutableParameterSet thawCopy() {
		return new MutableParameterSet(this);
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public boolean hasParameter(char shortOpt) {
		return this.shortOpts.containsKey(shortOpt);
	}

	@Override
	public Parameter getParameter(char shortOpt) {
		return this.shortOpts.get(shortOpt);
	}

	@Override
	public Set<Character> getShortOptions() {
		return this.shortOpts.keySet();
	}

	@Override
	public boolean hasParameter(String longOpt) {
		return this.longOpts.containsKey(longOpt);
	}

	@Override
	public Parameter getParameter(String longOpt) {
		return this.longOpts.get(longOpt);
	}

	@Override
	public Set<String> getLongOptions() {
		return this.longOpts.keySet();
	}

	@Override
	public Collection<Parameter> getParameters(Comparator<? super Parameter> c) {
		if ((c == null) || (c == ParameterSubSchema.DEFAULT_COMPARATOR)) { return this.sorted; }
		List<Parameter> l = new ArrayList<>(this.sorted);
		Collections.sort(l, c);
		return l;
	}
}