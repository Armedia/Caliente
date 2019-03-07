package com.armedia.caliente.engine.tools;

import java.util.ListIterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;

import com.armedia.commons.utilities.Tools;

public class ReadWriteListIterator<ELEMENT> extends ReadWriteIterator<ELEMENT> implements ListIterator<ELEMENT> {

	private final Function<ELEMENT, ELEMENT> validator;
	private final ListIterator<ELEMENT> iterator;

	public ReadWriteListIterator(ListIterator<ELEMENT> iterator) {
		this(null, iterator, null);
	}

	public ReadWriteListIterator(ReadWriteLock rwLock, ListIterator<ELEMENT> iterator) {
		this(rwLock, iterator, null);
	}

	public ReadWriteListIterator(ListIterator<ELEMENT> iterator, Function<ELEMENT, ELEMENT> validator) {
		this(null, iterator, validator);
	}

	public ReadWriteListIterator(ReadWriteLock rwLock, ListIterator<ELEMENT> iterator,
		Function<ELEMENT, ELEMENT> validator) {
		super(rwLock, iterator);
		this.iterator = iterator;
		this.validator = Tools.coalesce(validator, Function.identity());
	}

	@Override
	public boolean hasPrevious() {
		return readLocked(this.iterator::hasPrevious);
	}

	@Override
	public ELEMENT previous() {
		return readLocked(this.iterator::previous);
	}

	@Override
	public int nextIndex() {
		return readLocked(this.iterator::nextIndex);
	}

	@Override
	public int previousIndex() {
		return readLocked(this.iterator::previousIndex);
	}

	@Override
	public void set(ELEMENT e) {
		ELEMENT E = this.validator.apply(e);
		writeLocked(() -> {
			this.iterator.set(E);
		});
	}

	@Override
	public void add(ELEMENT e) {
		ELEMENT E = this.validator.apply(e);
		writeLocked(() -> {
			this.iterator.add(E);
		});
	}
}