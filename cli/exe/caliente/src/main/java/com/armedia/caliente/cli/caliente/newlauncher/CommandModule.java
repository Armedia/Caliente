package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.Tools;

public abstract class CommandModule implements AutoCloseable {

	public static class Descriptor {

		private static final Pattern HAS_SPACE = Pattern.compile("\\s");

		private final String name;
		private final Set<String> aliases;
		private final String description;

		private static String canonicalize(String s) {
			s = StringUtils.strip(s);
			s = StringUtils.lowerCase(s);
			if (Descriptor.HAS_SPACE.matcher(s).find()) { return null; }
			if (StringUtils.isEmpty(s)) { return null; }
			return s;
		}

		public Descriptor(String name, String description, String... aliases) {
			this(name, description, Arrays.asList(aliases));
		}

		public Descriptor(String name, String description, Collection<String> aliases) {
			this.name = Descriptor.canonicalize(name);
			if (this.name == null) { throw new IllegalArgumentException(
				String.format("The command name [%s] is invalid", name)); }
			Set<String> a = new TreeSet<>();
			if ((aliases != null) && !aliases.isEmpty()) {
				for (String s : aliases) {
					s = Descriptor.canonicalize(s);
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

	protected final CalienteWarningTracker warningTracker;
	private final Descriptor descriptor;
	private final boolean requiresStorage;
	private final boolean requiresCleanData;

	protected CommandModule(CalienteWarningTracker warningTracker, boolean requiresStorage, boolean requiresCleanData,
		Descriptor descriptor) {
		this.warningTracker = warningTracker;
		this.descriptor = descriptor;
		this.requiresStorage = requiresStorage;
		this.requiresCleanData = (requiresStorage && requiresCleanData);
	}

	public Descriptor getDescriptor() {
		return this.descriptor;
	}

	public final boolean isRequiresStorage() {
		return this.requiresStorage;
	}

	public final boolean isRequiresCleanData() {
		return this.requiresCleanData;
	}

	public final int run(EngineProxy engineProxy, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> contentStore, final OptionValues commandValues, final Collection<String> positionals)
		throws CalienteException {
		if (this.requiresStorage) {
			// Make sure the storage engines are there
			Objects.requireNonNull(objectStore,
				String.format("The %s command requires an object store!", this.descriptor.getName()));
			Objects.requireNonNull(contentStore,
				String.format("The %s command requires a content store!", this.descriptor.getName()));
		} else {
			// Make sure they always go null downstream
			objectStore = null;
			contentStore = null;
		}
		return execute(engineProxy, objectStore, contentStore, commandValues, positionals);
	}

	protected abstract int execute(EngineProxy engineProxy, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> contentStore, OptionValues commandValues, Collection<String> positionals)
		throws CalienteException;

}