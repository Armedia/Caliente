package com.armedia.caliente.cli.exception;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.CommandScheme;

public class MissingRequiredCommandException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final CommandScheme commandScheme;

	public MissingRequiredCommandException(CommandScheme scheme) {
		super(scheme, null, null);
		this.commandScheme = scheme;
	}

	public CommandScheme getCommandScheme() {
		return this.commandScheme;
	}

	@Override
	protected String renderMessage() {
		StringBuilder msg = new StringBuilder();
		final String nl = String.format("%n");
		for (Command c : this.commandScheme.getCommands()) {
			String name = c.getName();
			Set<String> aliases = c.getAliases();
			msg.append(nl).append('\t').append(name);
			if (!aliases.isEmpty()) {
				msg.append(" (");
				boolean firstAlias = true;
				for (String a : aliases) {
					if (StringUtils.equalsIgnoreCase(name, a)) {
						continue;
					}
					if (!firstAlias) {
						msg.append(", ");
					}
					msg.append(a);
					firstAlias = false;
				}
				msg.append(')');
			}
		}

		return String.format("A command name is required, please use one of%s", msg);
	}
}