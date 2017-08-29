package com.armedia.caliente.cli.help;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Objects;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.CommandScheme;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.exception.HelpRequestedException;

public abstract class HelpRenderer {
	public static final int DEFAULT_WIDTH = 80;

	public final void renderHelp(String programName, HelpRequestedException help, OutputStream out) throws IOException {
		renderHelp(programName, help, HelpRenderer.DEFAULT_WIDTH, out);
	}

	public final void renderHelp(String programName, HelpRequestedException help, int width, OutputStream out)
		throws IOException {
		renderHelp(programName, help, width, out, null);
	}

	public final void renderHelp(String programName, HelpRequestedException help, OutputStream out, Charset encoding)
		throws IOException {
		renderHelp(programName, help, HelpRenderer.DEFAULT_WIDTH, out, encoding);
	}

	public final void renderHelp(String programName, HelpRequestedException help, int width, OutputStream out,
		Charset encoding) throws IOException {
		Objects.requireNonNull(out, "Must provide an output stream to write to");
		if (encoding == null) {
			encoding = Charset.defaultCharset();
		}
		renderHelp(programName, help, width, new OutputStreamWriter(out, encoding));
	}

	public final String renderHelp(String programName, HelpRequestedException help) {
		return renderHelp(programName, help, HelpRenderer.DEFAULT_WIDTH);
	}

	public final String renderHelp(String programName, HelpRequestedException help, int width) {
		StringWriter w = new StringWriter();
		try {
			renderHelp(programName, help, width, w);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IOException writing to memory", e);
		}
		return w.toString();
	}

	public final void renderHelp(String programName, HelpRequestedException help, Writer w) throws IOException {
		renderHelp(programName, help, HelpRenderer.DEFAULT_WIDTH, w);
	}

	public final void renderHelp(String programName, HelpRequestedException help, int width, Writer w)
		throws IOException {
		Objects.requireNonNull(programName, "Must provide a program name");
		Objects.requireNonNull(help, "Must provide a scheme to render help for");
		Objects.requireNonNull(w, "Must provide a writer to render on");

		renderUsage(programName, width, w);
		renderScheme(help.getBaseScheme(), width, w);

		Command command = help.getCommand();

		if (CommandScheme.class.isInstance(help.getBaseScheme()) && (command == null)) {
			// This is a command scheme, but no command is given, so list them out
			CommandScheme commandScheme = CommandScheme.class.cast(help.getBaseScheme());
			renderCommands(commandScheme.getCommands(), width, w);
			return;
		}

		if (help.getCommand() != null) {
			// This is a command, so render out the command's parameters
			renderCommandScheme(help.getCommand(), width, w);
			return;
		}
	}

	protected abstract void renderUsage(String programName, int width, Writer w) throws IOException;

	protected abstract void renderScheme(OptionScheme scheme, int width, Writer w) throws IOException;

	protected abstract void renderCommandScheme(Command scheme, int width, Writer w) throws IOException;

	protected abstract void renderCommands(Collection<Command> commands, int width, Writer w) throws IOException;
}