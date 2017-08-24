package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class CommandScheme extends ParameterScheme {

	public CommandScheme(String name) {
		super(name);
	}

	private final Map<String, Command> commands = new TreeMap<>();

	public CommandScheme addCommand(Command command) {
		if (command == null) { throw new IllegalArgumentException("Must provide a non-null command"); }
		this.commands.put(command.getName(), command);
		return this;
	}

	public Command removeCommand(Command command) {
		if (command == null) { throw new IllegalArgumentException("Must provide a non-null command"); }
		return this.commands.remove(command.getName());
	}

	public Command removeCommand(String command) {
		if (command == null) { throw new IllegalArgumentException("Must provide a non-null command name"); }
		return this.commands.remove(command);
	}

	public Set<String> getCommandNames() {
		return new TreeSet<>(this.commands.keySet());
	}

	public Collection<Command> getCommands() {
		return new ArrayList<>(this.commands.values());
	}

	public Command getCommand(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a non-null name"); }
		return this.commands.get(name);
	}

	public boolean hasCommand(String name) {
		return (getCommand(name) != null);
	}

	public int getCommandCount() {
		return this.commands.size();
	}
}