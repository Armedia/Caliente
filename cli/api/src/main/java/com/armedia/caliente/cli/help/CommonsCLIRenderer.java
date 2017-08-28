package com.armedia.caliente.cli.help;

import java.io.IOException;
import java.io.Writer;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.exception.HelpRequestedException;

public class CommonsCLIRenderer extends HelpRenderer {

	@Override
	protected void doRenderHelp(String programName, HelpRequestedException help, int width, Writer w)
		throws IOException {
		for (Option o : help.getBaseScheme()) {
			Character shortOpt = o.getShortOpt();
		}
	}
}