package com.armedia.caliente.cli.help;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.CommandScheme;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.exception.HelpRequestedException;
import com.armedia.commons.utilities.Tools;

public final class HelpRenderer {
	public static final int DEFAULT_WIDTH = 100;

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

	private void renderPositionals(StringBuilder sb, String label, Character sep, int min, int max) {
		if (max == 0) { return; }

		sb.append(" ");

		// First, render the required arguments
		final String fmt = String.format("%s%%0%dd", label, String.valueOf(min).length());
		int i = 0;
		while (i < min) {
			if (i > 0) {
				sb.append(sep);
			}
			sb.append(String.format(fmt, i + 1));
			i++;
		}
		// Now, render the optional ones (i.e. those between the minimum required, and the maximum
		// allowed. If we're already past the max, then we render nothing else.
		if ((i < max) || (max < 0)) {
			String trailer = null;
			if (max > 0) {
				trailer = String.format("%s%s", sep, String.format(fmt, max));
				switch (max - i) {
					case 3: // [,argI,argI+1,argMax]
						trailer = String.format("%s%s%s", sep, String.format(fmt, --max), trailer);
					case 2: // [,argI,argMax]
						trailer = String.format("%s%s%s", sep, String.format(fmt, --max), trailer);
					case 1: // [,argMax]
						break;
					default: // [,argI,...,argMax]
						trailer = String.format("%s%s%s%s%s", sep, String.format(fmt, i), sep, "...", trailer);
						break;
				}
			} else {
				trailer = String.format("%s%s%s%s%s%s", sep, String.format(fmt, Math.max(i, 1)), sep, "...", sep,
					String.format("%sN", label));
			}
			sb.append('[').append(trailer);
			if (sep == ' ') {
				sb.append(sep);
			}
			sb.append(']');
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
			sb.append(" [ global-options ]");
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

		renderPositionals(sb, "arg", ' ', minPositionals, maxPositionals);
		sb.append(HelpRenderer.NL);
		fmt.printUsage(pw, width, sb.toString());
	}

	private void formatOption(HelpFormatter fmt, PrintWriter pw, int width, Option o) {

		String shortLabel = "  ";
		Character shortOpt = o.getShortOpt();
		if (shortOpt != null) {
			shortLabel = String.format("-%s", shortOpt);
		}
		String longOpt = o.getLongOpt();
		String longLabel = "";
		if (longOpt != null) {
			shortLabel = (shortOpt != null ? String.format("%s, ", shortLabel) : "    ");
			longLabel = String.format("--%s", longOpt);
		}

		int min = o.getMinValueCount();
		int max = o.getMaxValueCount();

		String valueDesc = "";
		if (max != 0) {
			// First, render the minimums...
			StringBuilder sb = new StringBuilder();
			renderPositionals(sb, Tools.coalesce(o.getValueName(), "arg"), o.getValueSep(), min, max);
			valueDesc = sb.toString();
		}

		fmt.printWrapped(pw, width, (o.isRequired() ? 2 : 4),
			String.format("\t%s%s%s%s", (o.isRequired() ? "* " : "  "), shortLabel, longLabel, valueDesc));

		String desc = o.getDescription();
		if (desc != null) {
			fmt.printWrapped(pw, width, 8, String.format("\t%s", desc));
		}

		if ((min == max) && (min != 0)) {
			fmt.printWrapped(pw, width, 8, String.format("\tRequired values: %d", min));
		} else if ((min != 0) || (max != 0)) {
			String minClause = "";
			String maxClause = "";
			if (min > 0) {
				minClause = String.format("Min values: %d", min);
			}
			if (max > 0) {
				maxClause = String.format("Max values: %d", max);
			}
			String msg = null;
			if (!StringUtils.isEmpty(minClause) && !StringUtils.isEmpty(maxClause)) {
				msg = String.format("\t%s, %s", minClause, maxClause);
			} else if (!StringUtils.isEmpty(minClause)) {
				msg = String.format("\t%s", minClause);
			} else if (!StringUtils.isEmpty(maxClause)) {
				msg = String.format("\t%s", maxClause);
			}

			if (msg != null) {
				fmt.printWrapped(pw, width, 8, msg);
			}
		}

		Set<String> allowed = o.getAllowedValues();
		if ((allowed != null) && !allowed.isEmpty()) {
			fmt.printWrapped(pw, width, 8, String.format("\tAllowed values: %s", allowed));
		}

		List<String> defaults = o.getDefaults();
		if ((defaults != null) && !defaults.isEmpty()) {
			Object def = (defaults.size() == 1 ? defaults.get(0) : defaults);
			String plural = (defaults.size() == 1 ? "" : "s");
			fmt.printWrapped(pw, width, 8, String.format("\tDefault value%s: %s", plural, def));
		}
		pw.println();
	}

	private void formatScheme(HelpFormatter fmt, PrintWriter pw, int width, OptionScheme scheme) {
		if (scheme == null) { return; }
		// Options options = new Options();
		fmt.printWrapped(pw, width, "(* = the option is required)");
		for (Option o : scheme) {
			formatOption(fmt, pw, width, o);
		}
	}

	private void formatCommand(HelpFormatter fmt, PrintWriter pw, int width, Command command) {
		if (command == null) { return; }
		String aliases = "";
		if (!command.getAliases().isEmpty()) {
			aliases = " (";
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
		fmt.printWrapped(pw, width,
			String.format("Options for '%s'%s: (* = the option is required)", command.getName(), aliases));
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
		pw.println();
		sb.setLength(0);

		if (baseScheme.getOptionCount() > 0) {
			fmt.printWrapped(pw, width, String.format("%s Options: (* = the option is required)",
				(commandScheme != null ? "Global" : "Available")));
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