package com.armedia.caliente.cli.help;

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
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValueFilter;
import com.armedia.caliente.cli.exception.CommandLineSyntaxException;
import com.armedia.caliente.cli.exception.HelpRequestedException;
import com.armedia.commons.utilities.Tools;

public final class HelpRenderer {
	public static final int DEFAULT_WIDTH = 100;

	private static final String NL = String.format("%n");

	private HelpRenderer() {
		// Ensure the class isn't instantiable
	}

	public static void renderHelp(String programName, HelpRequestedException help, OutputStream out) {
		HelpRenderer.renderHelp(programName, help, HelpRenderer.DEFAULT_WIDTH, out);
	}

	public static void renderHelp(String programName, HelpRequestedException help, int width, OutputStream out) {
		HelpRenderer.renderHelp(programName, help, width, out, null);
	}

	public static void renderHelp(String programName, HelpRequestedException help, OutputStream out, Charset encoding) {
		HelpRenderer.renderHelp(programName, help, HelpRenderer.DEFAULT_WIDTH, out, encoding);
	}

	public static void renderHelp(String programName, HelpRequestedException help, int width, OutputStream out,
		Charset encoding) {
		Objects.requireNonNull(out, "Must provide an output stream to write to");
		if (encoding == null) {
			encoding = Charset.defaultCharset();
		}
		HelpRenderer.renderHelp(programName, help, width, new OutputStreamWriter(out, encoding));
	}

	public static String renderHelp(String programName, HelpRequestedException help) {
		return HelpRenderer.renderHelp(programName, help, HelpRenderer.DEFAULT_WIDTH);
	}

	public static String renderHelp(String programName, HelpRequestedException help, int width) {
		StringWriter w = new StringWriter();
		HelpRenderer.renderHelp(programName, help, width, w);
		return w.toString();
	}

	public static void renderHelp(String programName, HelpRequestedException help, Writer w) {
		HelpRenderer.renderHelp(programName, help, HelpRenderer.DEFAULT_WIDTH, w);
	}

	private static void renderPositionals(StringBuilder sb, String label, Character sep, int min, int max) {
		if (max == 0) { return; }

		sb.append(" ");

		if ((min == max) && (min == 1)) {
			// No need to append numbers to the parameter...
			sb.append('<').append(label).append('>');
			return;
		}

		// First, render the required arguments
		final String fmt = String.format("<%s#%%d>", label);

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
					String.format("<%s#N>", label));
			}
			if ((sep == ' ') && (min > 0)) {
				sb.append(sep);
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

	private static void formatSyntax(PrintWriter pw, int width, String programName, OptionScheme baseScheme,
		CommandScheme commandScheme, Command command) {

		// usage: programName [globalOptions...] command [commandOptions...] [arguments...]
		StringBuilder sb = new StringBuilder();
		sb.append("usage: ").append(programName);
		int minPositionals = 0;
		int maxPositionals = -1;
		if (commandScheme != null) {
			sb.append(" [ global-options ]");
			if (command == null) {
				if (!commandScheme.isCommandRequired()) {
					sb.append(" [");
				}
				sb.append(" command [ command-options ] ");
				if (!commandScheme.isCommandRequired()) {
					sb.append(']');
				}
			} else {
				sb.append(String.format(" %1$s [ %1$s-options ]", command.getName()));
				minPositionals = command.getMinArguments();
				maxPositionals = command.getMaxArguments();
			}
		} else {
			sb.append(" [ options ]");
			minPositionals = baseScheme.getMinArguments();
			maxPositionals = baseScheme.getMaxArguments();
		}

		HelpRenderer.renderPositionals(sb, "arg", ' ', minPositionals, maxPositionals);
		HelpRenderer.printWrapped(pw, width, sb.toString());
		HelpRenderer.printWrapped(pw, width, "(* = option is required)");
	}

	private static void printWrapped(PrintWriter pw, int totalWidth, String msg) {
		HelpRenderer.printWrapped(pw, totalWidth, 0, msg);
	}

	private static void printWrapped(PrintWriter pw, int totalWidth, int indentWidth, String msg) {
		msg = WordUtils.wrap(msg, totalWidth - indentWidth);
		String lead = StringUtils.repeat(' ', indentWidth);
		for (String s : new StrTokenizer(msg, HelpRenderer.NL).getTokenList()) {
			pw.printf("%s%s%n", lead, s);
		}
	}

	private static void formatOption(PrintWriter pw, int width, Option o) {

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

		int min = o.getMinArguments();
		int max = o.getMaxArguments();

		String valueDesc = "";
		if (max != 0) {
			// First, render the minimums...
			StringBuilder sb = new StringBuilder();
			HelpRenderer.renderPositionals(sb, Tools.coalesce(o.getArgumentName(), "arg"), o.getValueSep(), min, max);
			valueDesc = sb.toString();
		}

		int indent = (o.isRequired() ? 2 : (shortOpt != null ? 4 : 8));
		HelpRenderer.printWrapped(pw, width, indent,
			String.format("%s%s%s%s", (o.isRequired() ? "* " : "  "), shortLabel, longLabel, valueDesc));

		String desc = o.getDescription();
		if (desc != null) {
			HelpRenderer.printWrapped(pw, width, 12, String.format("%s", desc));
			pw.println();
		}

		boolean addLine = false;
		if ((min == max) && (min != 0)) {
			HelpRenderer.printWrapped(pw, width, 12, String.format("Required values: %d", min));
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
				HelpRenderer.printWrapped(pw, width, 12, msg);
				addLine = true;
			}
		}

		OptionValueFilter filter = o.getValueFilter();
		if (filter != null) {
			String definition = filter.getDefinition();
			if (!StringUtils.isBlank(definition)) {
				HelpRenderer.printWrapped(pw, width, 12, String.format("Allowed values: %s", definition));
			}
			addLine = true;
		}

		List<String> defaults = o.getDefaults();
		if ((defaults != null) && !defaults.isEmpty()) {
			Object def = (defaults.size() == 1 ? defaults.get(0) : defaults);
			String plural = (defaults.size() == 1 ? "" : "s");
			HelpRenderer.printWrapped(pw, width, 12, String.format("Default value%s: %s", plural, def));
			addLine = true;
		}
		if (addLine) {
			pw.println();
		}
	}

	private static void formatScheme(PrintWriter pw, int width, OptionScheme scheme) {
		if (scheme == null) { return; }
		for (Option o : scheme.getBaseGroup()) {
			HelpRenderer.formatOption(pw, width, o);
		}
		for (String s : scheme.getGroupNames()) {
			OptionGroup g = scheme.getGroup(s);
			String desc = g.getDescription();
			if (desc == null) {
				desc = "";
			} else {
				desc = String.format(" %s", desc);
			}
			HelpRenderer.printWrapped(pw, width, String.format("Options for %s:%s", g.getName(), desc));
			HelpRenderer.printWrapped(pw, width, StringUtils.repeat('-', (3 * width) / 4));
			for (Option o : g) {
				HelpRenderer.formatOption(pw, width, o);
			}
		}
	}

	private static void formatCommand(PrintWriter pw, int width, Command command) {
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
		HelpRenderer.printWrapped(pw, width, String.format("Command Options for '%s'%s:", command.getName(), aliases));
		HelpRenderer.printWrapped(pw, width, StringUtils.repeat('=', width));
		HelpRenderer.formatScheme(pw, width, command);
	}

	private static void formatCommands(PrintWriter pw, int width, CommandScheme commandScheme) {
		if (commandScheme == null) { return; }
		HelpRenderer.printWrapped(pw, width, "Available Commands:");
		HelpRenderer.printWrapped(pw, width, StringUtils.repeat('=', width));

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

			HelpRenderer.printWrapped(pw, width, sb.toString());
			String desc = c.getDescription();
			if ((c != null) && (desc != null)) {
				HelpRenderer.printWrapped(pw, width, 8, desc);
			}
		}
	}

	public static void renderHelp(String programName, HelpRequestedException help, int width, Writer w) {
		Objects.requireNonNull(programName, "Must provide a program name");
		Objects.requireNonNull(help, "Must provide a scheme to render help for");
		Objects.requireNonNull(w, "Must provide a writer to render on");
		width = Math.max(80, width);

		final String line = StringUtils.repeat('=', width);

		final OptionScheme baseScheme = help.getBaseScheme();
		final CommandScheme commandScheme = CommandScheme.castAs(baseScheme);
		final Command command = help.getCommand();
		final PrintWriter pw = new PrintWriter(w);

		HelpRenderer.formatSyntax(pw, width, programName, baseScheme, commandScheme, command);
		pw.println();

		if (baseScheme.getOptionCount() > 0) {
			HelpRenderer.printWrapped(pw, width,
				String.format("%s Options:", (commandScheme != null ? "Global" : "Available")));
			HelpRenderer.printWrapped(pw, width, line);
			HelpRenderer.formatScheme(pw, width, baseScheme);
		}

		if (commandScheme != null) {
			if (command != null) {
				HelpRenderer.formatCommand(pw, width, command);
			} else {
				HelpRenderer.formatCommands(pw, width, commandScheme);
			}
		}

		CommandLineSyntaxException e = help.getCause();
		if (e != null) {
			pw.println();
			HelpRenderer.renderError("Syntax Error: ", e, width, w);
			pw.println();
		}

		pw.flush();
	}

	public static void renderError(CommandLineSyntaxException e, OutputStream out) {
		HelpRenderer.renderError(null, e, out);
	}

	public static void renderError(CommandLineSyntaxException e, int width, OutputStream out) {
		HelpRenderer.renderError(null, e, width, out);
	}

	public static void renderError(CommandLineSyntaxException e, OutputStream out, Charset encoding) {
		HelpRenderer.renderError(null, e, out, encoding);
	}

	public static void renderError(CommandLineSyntaxException e, int width, OutputStream out, Charset encoding) {
		HelpRenderer.renderError(null, e, width, out, encoding);
	}

	public static void renderError(CommandLineSyntaxException e, Writer w) {
		HelpRenderer.renderError(null, e, w);
	}

	public static void renderError(CommandLineSyntaxException e, int width, Writer w) {
		HelpRenderer.renderError(null, e, width, w);
	}

	public static void renderError(String prefix, CommandLineSyntaxException e, OutputStream out) {
		HelpRenderer.renderError(prefix, e, HelpRenderer.DEFAULT_WIDTH, out);
	}

	public static void renderError(String prefix, CommandLineSyntaxException e, int width, OutputStream out) {
		HelpRenderer.renderError(prefix, e, width, out, null);
	}

	public static void renderError(String prefix, CommandLineSyntaxException e, OutputStream out, Charset encoding) {
		HelpRenderer.renderError(prefix, e, HelpRenderer.DEFAULT_WIDTH, out, encoding);
	}

	public static void renderError(String prefix, CommandLineSyntaxException e, int width, OutputStream out,
		Charset encoding) {
		Objects.requireNonNull(out, "Must provide an output stream to write to");
		if (encoding == null) {
			encoding = Charset.defaultCharset();
		}
		HelpRenderer.renderError(prefix, e, width, new OutputStreamWriter(out, encoding));
	}

	public static void renderError(String prefix, CommandLineSyntaxException e, Writer w) {
		HelpRenderer.renderError(prefix, e, HelpRenderer.DEFAULT_WIDTH, w);
	}

	public static void renderError(String prefix, CommandLineSyntaxException e, int width, Writer w) {
		Objects.requireNonNull(e, "Must provide an exception to render the message for");
		Objects.requireNonNull(w, "Must provide a writer to render on");
		width = Math.max(80, width);
		final PrintWriter pw = new PrintWriter(w);
		if (!StringUtils.isEmpty(prefix)) {
			prefix = String.format("%s: ", prefix);
		} else {
			prefix = "";
		}
		HelpRenderer.printWrapped(pw, width, String.format("%s%s%n", prefix, e.getMessage()));
		pw.flush();
	}
}