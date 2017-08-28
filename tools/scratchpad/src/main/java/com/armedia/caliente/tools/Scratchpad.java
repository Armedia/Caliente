package com.armedia.caliente.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used as a testbed to run quick'n'dirty DFC test programs
 *
 * @author diego.rivera@armedia.com
 *
 */
public class Scratchpad {

	public static final void main(String... args) throws Throwable {
		Pattern p = Pattern.compile(args[0]);
		for (int i = 1; i < args.length; i++) {
			final String str = args[i];
			Matcher m = p.matcher(str);
			int start = 0;
			while (m.find()) {
				String value = str.substring(start, m.start());
				start = m.end();
			}
			String lastValue = str.substring(start);
		}
	}

}