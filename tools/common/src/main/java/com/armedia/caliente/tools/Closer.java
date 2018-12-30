package com.armedia.caliente.tools;

public class Closer {

	public static final Exception closeQuietly(AutoCloseable c) {
		if (c == null) { return null; }
		try {
			c.close();
			return null;
		} catch (Exception e) {
			return e;
		}
	}

}