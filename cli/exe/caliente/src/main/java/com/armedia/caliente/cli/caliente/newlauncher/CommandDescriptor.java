package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.armedia.commons.utilities.Tools;

public class CommandDescriptor {

	private static final Pattern HAS_SPACE = Pattern.compile("\\s");

	private final String name;
	private final Set<String> aliases;
	private final String description;

	private static String canonicalize(String s) {
		s = StringUtils.strip(s);
		s = StringUtils.lowerCase(s);
		if (CommandDescriptor.HAS_SPACE.matcher(s).find()) { return null; }
		if (StringUtils.isEmpty(s)) { return null; }
		return s;
	}

	public CommandDescriptor(String name, String description, String... aliases) {
		this(name, description, Arrays.asList(aliases));
	}

	public CommandDescriptor(String name, String description, Collection<String> aliases) {
		this.name = CommandDescriptor.canonicalize(name);
		if (this.name == null) { throw new IllegalArgumentException(
			String.format("The command name [%s] is invalid", name)); }
		Set<String> a = new TreeSet<>();
		if ((aliases != null) && !aliases.isEmpty()) {
			for (String s : aliases) {
				s = CommandDescriptor.canonicalize(s);
				if (s == null) { throw new IllegalArgumentException(
					String.format("The command alias [%s] is invalid", a)); }
				if (Tools.equals(this.name, s)) {
					// Avoid redundancy
					continue;
				}
				a.add(s);
			}
		}
		this.aliases = Tools.freezeSet(a);
		this.description = description;
	}

	public String getName() {
		return this.name;
	}

	public Set<String> getAliases() {
		return this.aliases;
	}

	public String getDescription() {
		return this.description;
	}

}