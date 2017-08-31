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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StrTokenizer;
import org.apache.commons.text.WordUtils;

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
		final String fmt = String.format("%s#%%d", label);

		int opt = min + 1;
		if (min > 0) {
			// Render from 1 to min
			String trailer = String.format(fmt, min);
			switch (min) {
				case 3: // arg1,arg2,arg3
					trailer = String.format("%s%s%s", String.format(fmt, --min), sep, trailer);
				case 2: // arg1,arg2
					trailer = String.format("%s%s%s", String.format(fmt, --min), sep, trailer);
				case 1: // arg1
					break;
				default: // arg1,...,argMin
					trailer = String.format("%s%s%s%s%s", String.format(fmt, 1), sep, "...", sep, trailer);
					break;
			}
			sb.append(trailer);
		}

		// Now, render the optional ones (i.e. those between the minimum required, and the maximum
		// allowed. If we're already past the max, then we render nothing else.
		if ((opt <= max) || (max < 0)) {
			String trailer = null;
			if (max > 0) {
				trailer = String.format(fmt, max);
				switch ((max - opt) + 1) {
					case 3: // [,argI,argI+1,argMax]
						trailer = String.format("%s%s%s", String.format(fmt, --max), sep, trailer);
					case 2: // [,argI,argMax]
						trailer = String.format("%s%s%s", String.format(fmt, --max), sep, trailer);
					case 1: // [,argMax]
						break;
					default: // [,argI,...,argMax]
						trailer = String.format("%s%s%s%s%s", String.format(fmt, opt), sep, "...", sep, trailer);
						break;
				}
			} else {
				trailer = String.format("%s%s%s%s%s", String.format(fmt, Math.max(opt, 1)), sep, "...", sep,
					String.format("%s#N", label));
			}
			sb.append('[');
			if ((min > 0) || (sep == ' ')) {
				sb.append(sep);
			}
			sb.append(trailer);
			if (sep == ' ') {
				sb.append(sep);
			}
			sb.append(']');
		}
	}

	private void formatSyntax(PrintWriter pw, int width, String programName, OptionScheme baseScheme,
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
		printWrapped(pw, width, sb.toString());
	}

	private void printWrapped(PrintWriter pw, int totalWidth, String msg) {
		printWrapped(pw, totalWidth, 0, msg);
	}

	private void printWrapped(PrintWriter pw, int totalWidth, int indentWidth, String msg) {
		msg = WordUtils.wrap(msg, totalWidth - indentWidth);
		String lead = StringUtils.repeat(' ', indentWidth);
		for (String s : new StrTokenizer(msg, HelpRenderer.NL).getTokenList()) {
			pw.printf("%s%s%n", lead, s);
		}
	}

	private void formatOption(PrintWriter pw, int width, Option o) {

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

		printWrapped(pw, width, (o.isRequired() ? 2 : 4),
			String.format("%s%s%s%s", (o.isRequired() ? "* " : "  "), shortLabel, longLabel, valueDesc));

		String desc = o.getDescription();
		if (desc != null) {
			printWrapped(pw, width, 8, String.format("%s", desc));
			pw.println();
		}

		boolean addLine = false;
		if ((min == max) && (min != 0)) {
			printWrapped(pw, width, 12, String.format("Required values: %d", min));
			addLine = true;
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
				msg = String.format("%s, %s", minClause, maxClause);
			} else if (!StringUtils.isEmpty(minClause)) {
				msg = String.format("%s", minClause);
			} else if (!StringUtils.isEmpty(maxClause)) {
				msg = String.format("%s", maxClause);
			}

			if (msg != null) {
				printWrapped(pw, width, 12, msg);
				addLine = true;
			}
		}

		Set<String> allowed = o.getAllowedValues();
		if ((allowed != null) && !allowed.isEmpty()) {
			Object a = (allowed.size() == 1 ? allowed.iterator().next() : allowed);
			String plural = (allowed.size() == 1 ? "" : "s");
			printWrapped(pw, width, 12, String.format("Allowed value%s: %s", plural, a));
			addLine = true;
		}

		List<String> defaults = o.getDefaults();
		if ((defaults != null) && !defaults.isEmpty()) {
			Object def = (defaults.size() == 1 ? defaults.get(0) : defaults);
			String plural = (defaults.size() == 1 ? "" : "s");
			printWrapped(pw, width, 12, String.format("Default value%s: %s", plural, def));
			addLine = true;
		}
		if (addLine) {
			pw.println();
		}
	}

	private void formatScheme(PrintWriter pw, int width, OptionScheme scheme) {
		if (scheme == null) { return; }
		// Options options = new Options();
		for (Option o : scheme) {
			formatOption(pw, width, o);
		}
		// fmt.printOptions(pw, width, options, fmt.getLeftPadding(), fmt.getDescPadding());
	}

	private void formatCommand(PrintWriter pw, int width, Command command) {
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
		printWrapped(pw, width,
			String.format("Command Options for '%s'%s: (* = option is required)", command.getName(), aliases));
		printWrapped(pw, width, StringUtils.repeat('-', width));
		formatScheme(pw, width, command);
	}

	private void formatCommands(PrintWriter pw, int width, CommandScheme commandScheme) {
		if (commandScheme == null) { return; }
		printWrapped(pw, width, "Available Commands:");
		printWrapped(pw, width, StringUtils.repeat('-', width));

		StringBuilder sb = new StringBuilder();
		for (Command c : commandScheme.getCommands()) {
			sb.setLength(0);
			sb.append(c.getName());
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

			printWrapped(pw, width, sb.toString());
			String desc = c.getDescription();
			if ((c != null) && (desc != null)) {
				printWrapped(pw, width, 8, desc);
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

		formatSyntax(pw, width, programName, baseScheme, commandScheme, command);
		pw.println();

		if (baseScheme.getOptionCount() > 0) {
			printWrapped(pw, width, String.format("%s Options: (* = option is required)",
				(commandScheme != null ? "Global" : "Available")));
			printWrapped(pw, width, line);
			formatScheme(pw, width, baseScheme);
		}

		if (commandScheme != null) {
			if (command != null) {
				formatCommand(pw, width, command);
			} else {
				formatCommands(pw, width, commandScheme);
			}
		}

		w.flush();
	}
}