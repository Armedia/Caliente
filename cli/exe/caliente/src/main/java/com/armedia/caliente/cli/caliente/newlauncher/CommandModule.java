package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Collection;
import java.util.Objects;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;

public abstract class CommandModule implements AutoCloseable {

	private final CommandDescriptor descriptor;
	private final boolean requiresStorage;
	private final boolean requiresCleanData;

	protected CommandModule(boolean requiresStorage, boolean requiresCleanData, CommandDescriptor descriptor) {
		this.descriptor = descriptor;
		this.requiresStorage = requiresStorage;
		this.requiresCleanData = (requiresStorage && requiresCleanData);
	}

	public CommandDescriptor getDescriptor() {
		return this.descriptor;
	}

	public final boolean isRequiresStorage() {
		return this.requiresStorage;
	}

	public final boolean isRequiresCleanData() {
		return this.requiresCleanData;
	}

	public final int run(final @SuppressWarnings("rawtypes") EngineFactory engineFactory,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, final OptionValues commandValues,
		final Collection<String> positionals) throws CalienteException {
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
		return execute(engineFactory, objectStore, contentStore, commandValues, positionals);
	}

	protected abstract int execute(final @SuppressWarnings("rawtypes") EngineFactory engineFactory,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, OptionValues commandValues,
		Collection<String> positionals) throws CalienteException;

}