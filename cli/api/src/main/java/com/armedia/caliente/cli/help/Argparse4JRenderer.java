package com.armedia.caliente.cli.help;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.OptionScheme;

public class Argparse4JRenderer extends HelpRenderer {

	@Override
	protected void renderUsage(String programName, boolean withCommands, int width, Writer w) throws IOException {
	}

	@Override
	protected void renderScheme(OptionScheme scheme, int width, Writer w) throws IOException {
	}

	@Override
	protected void renderCommandScheme(Command scheme, int width, Writer w) throws IOException {
	}

	@Override
	protected void renderCommands(Collection<Command> commands, int width, Writer w) throws IOException {
	}
}