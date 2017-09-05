package com.armedia.caliente.cli;

import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionSchemeExtension implements PositionalValueSupport {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final OptionScheme baseScheme;
	private final Collection<OptionScheme> extensions;
	private final OptionScheme changes;
	private String name = null;

	OptionSchemeExtension(OptionScheme baseScheme) {
		this(Collections.singleton(baseScheme));
	}

	OptionSchemeExtension(Collection<OptionScheme> extensions) {
		this.baseScheme = extensions.iterator().next();
		this.extensions = extensions;
		this.changes = new OptionScheme(this.baseScheme.isCaseSensitive()) //
			.setArgumentName(this.baseScheme.getArgumentName()) //
			.setMinArguments(this.baseScheme.getMinArguments()) //
			.setMaxArguments(this.baseScheme.getMaxArguments()) //
			.setExtensible(this.baseScheme.isExtensible());
	}

	OptionScheme getChanges() {
		return this.changes;
	}

	boolean isModified() {
		return (this.changes.getOptionCount() != 0);
	}

	public final OptionSchemeExtension setName(String name) {
		this.name = name;
		return this;
	}

	public String getName() {
		return this.name;
	}

	public OptionSchemeExtension setDescription(String description) {
		this.changes.setDescription(description);
		return this;
	}

	public String getDescription() {
		return this.changes.getDescription();
	}

	public boolean isSupportsArguments() {
		return this.changes.isSupportsPositionals();
	}

	@Override
	public String getArgumentName() {
		return this.changes.getArgumentName();
	}

	@Override
	public int getMinArguments() {
		return this.changes.getMinArguments();
	}

	@Override
	public int getMaxArguments() {
		return this.changes.getMaxArguments();
	}

	private Option getFirstCollision(Option option) {
		Collection<Option> collisions = this.baseScheme.findCollisions(option);
		for (OptionScheme s : this.extensions) {
			collisions = s.findCollisions(option);
			if (collisions != null) { return collisions.iterator().next(); }
		}
		return null;
	}

	public OptionSchemeExtension add(Option option) {
		Option existing = getFirstCollision(option);
		if (existing != null) {
			if (this.log != null) {
				this.log.debug("The given option {} would collide with {} - skipping!", option, existing);
			}
		} else {
			// No collisions...add it!
			this.changes.addOrReplace(option);
		}
		return this;
	}

	public Collection<Option> getOptions() {
		return this.changes.getOptions();
	}

	public Collection<Option> getRequiredOptions() {
		return this.changes.getRequiredOptions();
	}

	public int getRequiredOptionCount() {
		return this.changes.getRequiredOptionCount();
	}

	public int getOptionCount() {
		return this.changes.getOptionCount();
	}

	public boolean hasOption(Character shortOpt) {
		return this.changes.hasOption(shortOpt);
	}

	public boolean hasOption(String longOpt) {
		return this.changes.hasOption(longOpt);
	}

	public int countCollisions(Option option) {
		return this.changes.countCollisions(option);
	}

	public Option getOption(String longOpt) {
		return this.changes.getOption(longOpt);
	}

	public Option getOption(Character shortOpt) {
		return this.changes.getOption(shortOpt);
	}
}