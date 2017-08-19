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

	private Parameter helpParameter = null;

	private final Map<String, Command> commands = new TreeMap<>();

	public Parameter getHelpParameter() {
		return this.helpParameter;
	}

	/**
	 * Identical to invoking {@link #setHelpParameter(Parameter) setHelpParameter(null)}.
	 *
	 * @return the previous help parameter that was configured, or {@code null} if there was none
	 */
	public Parameter clearHelpParameter() {
		return setHelpParameter(null);
	}

	public Parameter setHelpParameter(Parameter helpParameter) {
		helpParameter = Parameter.ensureImmutable(helpParameter);
		Parameter old = this.helpParameter;
		if (old != null) {
			removeParameter(old);
		}
		if (helpParameter != null) {
			addParameter(helpParameter);
		}
		this.helpParameter = helpParameter;
		return old;
	}

	public Command addCommand(Command command) {
		if (command == null) { throw new IllegalArgumentException("Must provide a non-null command"); }
		return this.commands.put(command.getName(), command);
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

	public int getCommandCount() {
		return this.commands.size();
	}
}