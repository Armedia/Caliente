package com.armedia.caliente.cli.datagen.data;

import java.util.Map;

public interface DataRecord extends Iterable<String> {

	public boolean isComplete();

	public String get(int c);

	public String get(String name);

	public <E extends Enum<E>> String get(E e);

	public long getNumber();

	public boolean hasColumn(String name);

	public boolean hasValue(String name);

	public Map<String, String> asMap();
}