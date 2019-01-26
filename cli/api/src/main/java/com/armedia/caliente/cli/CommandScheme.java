package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class CommandScheme extends OptionScheme {

	private final boolean commandRequired;
	private final Map<String, String> aliases = new TreeMap<>();
	private final Map<String, Command> commands = new TreeMap<>();

	public CommandScheme(String name, boolean commandRequired) {
		super(name);
		this.commandRequired = commandRequired;
	}

	public boolean isCommandRequired() {
		return this.commandRequired;
	}

	private String canonicalizeCommand(String command) {
		if (command == null) { return null; }
		return command.toLowerCase();
	}

	public CommandScheme addCommand(Command command) {
		Objects.requireNonNull(command, "Must provide a non-null command");
		String name = canonicalizeCommand(command.getName());
		this.commands.put(name, command);
		this.aliases.put(name, name);
		for (String alias : command.getAliases()) {
			this.aliases.put(canonicalizeCommand(alias), name);
		}
		return this;
	}

	public Command removeCommand(Command command) {
		Objects.requireNonNull(command, "Must provide a non-null command");
		String name = canonicalizeCommand(command.getName());
		Command c = this.commands.remove(name);
		this.aliases.remove(name);
		if (c != null) {
			c.getAliases().stream().map(this::canonicalizeCommand).forEachOrdered(this.aliases::remove);
		}
		return c;
	}

	public Command removeCommand(String command) {
		Objects.requireNonNull(command, "Must provide a non-null command name");
		String name = canonicalizeCommand(command);
		Command c = this.commands.remove(name);
		this.aliases.remove(name);
		if (c != null) {
			c.getAliases().stream().map(this::canonicalizeCommand).forEachOrdered(this.aliases::remove);
		}
		return c;
	}

	public Collection<Command> getCommands() {
		return new ArrayList<>(this.commands.values());
	}

	public Set<String> getAliases() {
		return new TreeSet<>(this.aliases.keySet());
	}

	public Command getCommand(String nameOrAlias) {
		Objects.requireNonNull(nameOrAlias, "Must provide a non-null command name or alias");
		String name = this.aliases.get(canonicalizeCommand(nameOrAlias));
		return (name != null ? this.commands.get(name) : null);
	}

	public boolean hasCommand(String nameOrAlias) {
		return (getCommand(nameOrAlias) != null);
	}

	public int getCommandCount() {
		return this.commands.size();
	}

	public static CommandScheme castAs(OptionScheme scheme) {
		return (CommandScheme.class.isInstance(scheme) ? CommandScheme.class.cast(scheme) : null);
	}
}