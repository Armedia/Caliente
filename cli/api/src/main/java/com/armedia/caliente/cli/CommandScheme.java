package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class CommandScheme extends OptionScheme {

	private final boolean commandRequired;
	private final Map<String, Command> commands = new TreeMap<>();

	public CommandScheme(String name, boolean commandRequired) {
		this(name, commandRequired, false);
	}

	public CommandScheme(String name, boolean commandRequired, boolean caseSensitive) {
		super(name, caseSensitive);
		this.commandRequired = commandRequired;
	}

	public boolean isCommandRequired() {
		return this.commandRequired;
	}

	public CommandScheme addCommand(Command command) {
		Objects.requireNonNull(command, "Must provide a non-null command");
		this.commands.put(canonicalize(command.getName()), command);
		for (String alias : command.getAliases()) {
			this.commands.put(canonicalize(alias), command);
		}
		return this;
	}

	public Command removeCommand(Command command) {
		Objects.requireNonNull(command, "Must provide a non-null command");
		Command c = this.commands.remove(canonicalize(command.getName()));
		if (c != null) {
			for (String alias : c.getAliases()) {
				this.commands.remove(canonicalize(alias));
			}
		}
		return c;
	}

	public Command removeCommand(String command) {
		Objects.requireNonNull(command, "Must provide a non-null command name");
		Command c = this.commands.remove(canonicalize(command));
		if (c != null) {
			for (String alias : c.getAliases()) {
				this.commands.remove(canonicalize(alias));
			}
		}
		return c;
	}

	public Collection<Command> getCommands() {
		return new ArrayList<>(this.commands.values());
	}

	public Command getCommand(String nameOrAlias) {
		Objects.requireNonNull(nameOrAlias, "Must provide a non-null command name or alias");
		return this.commands.get(canonicalize(nameOrAlias));
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