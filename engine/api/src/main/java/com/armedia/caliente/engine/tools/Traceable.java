package com.armedia.caliente.engine.tools;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

import com.armedia.commons.utilities.function.CheckedRunnable;
import com.armedia.commons.utilities.function.CheckedSupplier;

public interface Traceable {

	public Logger getLog();

	public Serializable getId();

	public String getName();

	public static String formatArgs(Object... args) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append("{}");
		}
		return Traceable.format(sb.toString(), args);
	}

	public static String format(String format, Object... args) {
		return MessageFormatter.arrayFormat(format, args).getMessage();
	}

	public default <E extends Throwable> void invoke(CheckedRunnable<E> r, String method, Object... args) throws E {
		invoke(() -> {
			r.runChecked();
			return null;
		}, method, args);
	}

	public default <V, E extends Throwable> V invoke(CheckedSupplier<V, E> s, String method, Object... args) throws E {
		final String argStr = Traceable.formatArgs(args);
		final Logger log = getLog();
		log.trace("{}.{}({})", getName(), method, argStr);
		boolean ok = false;
		V ret = null;
		try {
			ret = s.getChecked();
			ok = true;
			return ret;
		} finally {
			log.trace("{}.{}({}) {} (returning {})", getName(), method, argStr, ok ? "completed" : "FAILED", ret);
		}
	}
}