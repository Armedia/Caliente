package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class CommandScheme extends ParameterScheme {

	private final boolean caseSensitive;

	public CommandScheme(String name, boolean caseSensitive) {
		super(name);
		this.caseSensitive = caseSensitive;
	}

	private final Map<String, Command> commands = new TreeMap<>();

	private String canonicalize(String str) {
		if (str == null) { return null; }
		if (!this.caseSensitive) {
			str = str.toLowerCase();
		}
		return str;
	}

	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}

	public CommandScheme addCommand(Command command) {
		if (command == null) { throw new IllegalArgumentException("Must provide a non-null command"); }
		this.commands.put(canonicalize(command.getName()), command);
		for (String alias : command.getAliases()) {
			this.commands.put(canonicalize(alias), command);
		}
		return this;
	}

	public Command removeCommand(Command command) {
		if (command == null) { throw new IllegalArgumentException("Must provide a non-null command"); }
		Command c = this.commands.remove(canonicalize(command.getName()));
		if (c != null) {
			for (String alias : c.getAliases()) {
				this.commands.remove(canonicalize(alias));
			}
		}
		return c;
	}

	public Command removeCommand(String command) {
		if (command == null) { throw new IllegalArgumentException("Must provide a non-null command name"); }
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
		if (nameOrAlias == null) { throw new IllegalArgumentException("Must provide a non-null name or alias"); }
		return this.commands.get(canonicalize(nameOrAlias));
	}

	public boolean hasCommand(String nameOrAlias) {
		return (getCommand(nameOrAlias) != null);
	}

	public int getCommandCount() {
		return this.commands.size();
	}
}