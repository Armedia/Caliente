package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Collection;

import com.armedia.caliente.cli.OptionValues;

public interface CommandFactory extends Iterable<CommandDescriptor> {

	public CommandModule getCommand(String nameOrAlias, OptionValues commandValues, Collection<String> positionals);

}