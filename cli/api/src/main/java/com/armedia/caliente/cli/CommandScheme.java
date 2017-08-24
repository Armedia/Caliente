package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class CommandScheme extends ParameterScheme {

	public CommandScheme(String name) {
		super(name);
	}

	private final Map<String, Command> commands = new TreeMap<>();

	public CommandScheme addCommand(Command command) {
		if (command == null) { throw new IllegalArgumentException("Must provide a non-null command"); }
		this.commands.put(command.getName(), command);
		for (String alias : command.getAliases()) {
			this.commands.put(alias, command);
		}
		return this;
	}

	public Command removeCommand(Command command) {
		if (command == null) { throw new IllegalArgumentException("Must provide a non-null command"); }
		Command c = this.commands.remove(command.getName());
		if (c != null) {
			for (String alias : c.getAliases()) {
				this.commands.remove(alias);
			}
		}
		return c;
	}

	public Command removeCommand(String command) {
		if (command == null) { throw new IllegalArgumentException("Must provide a non-null command name"); }
		Command c = this.commands.remove(command);
		if (c != null) {
			for (String alias : c.getAliases()) {
				this.commands.remove(alias);
			}
		}
		return c;
	}

	public Collection<Command> getCommands() {
		return new ArrayList<>(this.commands.values());
	}

	public Command getCommand(String nameOrAlias) {
		if (nameOrAlias == null) { throw new IllegalArgumentException("Must provide a non-null name or alias"); }
		return this.commands.get(nameOrAlias);
	}

	public boolean hasCommand(String nameOrAlias) {
		return (getCommand(nameOrAlias) != null);
	}

	public int getCommandCount() {
		return this.commands.size();
	}
}