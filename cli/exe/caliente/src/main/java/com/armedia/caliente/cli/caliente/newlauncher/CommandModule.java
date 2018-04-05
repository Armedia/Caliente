package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.Tools;

public abstract class CommandModule implements AutoCloseable {

	private final String name;
	private final OptionValues commandValues;
	private final List<String> positionals;
	private final boolean requiresStorage;
	private final boolean requiresCleanData;

	protected CommandModule(boolean requiresStorage, boolean requiresCleanData, String name, OptionValues commandValues,
		Collection<String> positionals) {
		this.name = name;
		this.commandValues = commandValues;
		if ((positionals != null) && !positionals.isEmpty()) {
			this.positionals = Tools.freezeList(new ArrayList<>(positionals));
		} else {
			this.positionals = Collections.emptyList();
		}
		this.requiresStorage = requiresStorage;
		this.requiresCleanData = requiresCleanData;
	}

	public final String getName() {
		return this.name;
	}

	public final OptionValues getCommandValues() {
		return this.commandValues;
	}

	public final List<String> getPositionals() {
		return this.positionals;
	}

	public final boolean isRequiresStorage() {
		return this.requiresStorage;
	}

	public final boolean isRequiresCleanData() {
		return this.requiresCleanData;
	}

	public final int run(CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore) throws Exception {
		if (this.requiresStorage) {
			// Make sure the storage engines are there
			Objects.requireNonNull(objectStore, String.format("The %s command requires an object store!", this.name));
			Objects.requireNonNull(contentStore, String.format("The %s command requires a content store!", this.name));
		} else {
			// Make sure they always go null downstream
			objectStore = null;
			contentStore = null;
		}
		return execute(objectStore, contentStore);
	}

	protected abstract int execute(CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore)
		throws Exception;

}