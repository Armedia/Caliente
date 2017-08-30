package com.armedia.caliente.cli.help;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.CommandScheme;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.exception.HelpRequestedException;
import com.armedia.commons.utilities.Tools;

public final class HelpRenderer {
	public static final int DEFAULT_WIDTH = 80;

	private static final String NL = String.format("%n");

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

	private void renderPositionals(StringBuilder sb, int min, int max) {
		if (max == 0) { return; }

		sb.append(" ");

		int i = 0;
		// First, render the required arguments
		final String fmt = String.format("arg%%0%dd", String.valueOf(min).length());
		for (i = 1; i <= min; i++) {
			if (i > 1) {
				sb.append(" ");
			}
			sb.append(String.format(fmt, i));
		}
		// Now, render the optional ones (i.e. those between the minimum required, and the maximum
		// allowed. If we're already past the max, then we render nothing else.
		if ((max < 0) || (i < max)) {
			sb.append("[ args... ]");
		}
	}

	private void formatSyntax(HelpFormatter fmt, PrintWriter pw, int width, String programName, OptionScheme baseScheme,
		CommandScheme commandScheme, Command command) {

		// usage: programName [globalOptions...] command [commandOptions...] [arguments...]
		StringBuilder sb = new StringBuilder();
		sb.append(programName);
		int minPositionals = 0;
		int maxPositionals = -1;
		if (commandScheme != null) {
			sb.append(" [ global-options... ]");
			if (command == null) {
				if (!commandScheme.isCommandRequired()) {
					sb.append(" [");
				}
				sb.append(" command [ command-options... ] ");
				if (!commandScheme.isCommandRequired()) {
					sb.append(']');
				}
			} else {
				sb.append(String.format(" %1$s [ %1$s-options ]", command.getName()));
				minPositionals = command.getMinArgs();
				maxPositionals = command.getMaxArgs();
			}
		} else {
			sb.append(" [options...]");
			minPositionals = baseScheme.getMinArgs();
			maxPositionals = baseScheme.getMaxArgs();
		}

		renderPositionals(sb, minPositionals, maxPositionals);
		sb.append(HelpRenderer.NL);
		fmt.printUsage(pw, width, sb.toString());
	}

	private org.apache.commons.cli.Option buildOption(Option o) {
		Builder b = null;
		if (o.getShortOpt() != null) {
			b = org.apache.commons.cli.Option.builder(o.getShortOpt().toString());
		} else {
			b = org.apache.commons.cli.Option.builder();
		}
		return b //
			.longOpt(o.getLongOpt()) //
			.required(o.isRequired()) //
			.desc(o.getDescription()) //
			.hasArg((o.getMinValueCount() > 0) || (o.getMaxValueCount() != 0)) //
			.numberOfArgs(o.getMaxValueCount()) //
			.optionalArg(o.getMinValueCount() == 0) //
			.argName(o.getValueName()) //
			.build();
	}

	private void formatScheme(HelpFormatter fmt, PrintWriter pw, int width, OptionScheme scheme) {
		if (scheme == null) { return; }
		Options options = new Options();
		for (Option o : scheme) {
			options.addOption(buildOption(o));
		}
		fmt.printOptions(pw, width, options, fmt.getLeftPadding(), fmt.getDescPadding());
	}

	private void formatCommand(HelpFormatter fmt, PrintWriter pw, int width, Command command) {
		if (command == null) { return; }
		String aliases = "";
		if (!command.getAliases().isEmpty()) {
			aliases = "(";
			boolean first = true;
			for (String a : command.getAliases()) {
				if (Tools.equals(a, command.getName())) {
					continue;
				}
				aliases = String.format("%s%s%s", aliases, first ? "" : ", ", a);
				first = false;
			}
			aliases = String.format("%s)", aliases);
		}
		fmt.printWrapped(pw, width, String.format("Parameters for '%s'%s:", command.getName(), aliases));
		fmt.printWrapped(pw, width, StringUtils.repeat('-', width));
		formatScheme(fmt, pw, width, command);
	}

	private void formatCommands(HelpFormatter fmt, PrintWriter pw, int width, CommandScheme commandScheme) {
		if (commandScheme == null) { return; }
		fmt.printWrapped(pw, width, "Available Commands:");
		fmt.printWrapped(pw, width, StringUtils.repeat('-', width));

		StringBuilder sb = new StringBuilder();
		for (Command c : commandScheme.getCommands()) {
			sb.setLength(0);
			sb.append("\t").append(c.getName());
			Set<String> aliases = c.getAliases();
			if (!aliases.isEmpty()) {
				sb.append(" (");
				boolean first = true;
				for (String s : aliases) {
					if (Tools.equals(s, c.getName())) {
						continue;
					}
					if (!first) {
						sb.append(", ");
					}
					sb.append(s);
					first = false;
				}
				sb.append(")");
			}

			fmt.printWrapped(pw, width, sb.toString());
			String desc = c.getDescription();
			if ((c != null) && (desc != null)) {
				fmt.printWrapped(pw, width, 8, String.format("\t%s%s", desc, HelpRenderer.NL));
			}
		}
	}

	public final void renderHelp(String programName, HelpRequestedException help, int width, Writer w)
		throws IOException {
		Objects.requireNonNull(programName, "Must provide a program name");
		Objects.requireNonNull(help, "Must provide a scheme to render help for");
		Objects.requireNonNull(w, "Must provide a writer to render on");

		final String line = StringUtils.repeat('-', width);

		final OptionScheme baseScheme = help.getBaseScheme();
		final CommandScheme commandScheme = CommandScheme.castAs(baseScheme);
		final Command command = help.getCommand();
		final PrintWriter pw = new PrintWriter(w);

		HelpFormatter fmt = new HelpFormatter();

		StringBuilder sb = new StringBuilder();
		formatSyntax(fmt, pw, width, programName, baseScheme, commandScheme, command);
		sb.setLength(0);

		if (baseScheme.getOptionCount() > 0) {
			fmt.printWrapped(pw, width,
				String.format("%s Parameters", (commandScheme != null ? "Global" : "Available")));
			fmt.printWrapped(pw, width, line);
			formatScheme(fmt, pw, width, baseScheme);
		}

		if (commandScheme != null) {
			if (command != null) {
				formatCommand(fmt, pw, width, command);
			} else {
				formatCommands(fmt, pw, width, commandScheme);
			}
		}

		w.flush();
	}
}