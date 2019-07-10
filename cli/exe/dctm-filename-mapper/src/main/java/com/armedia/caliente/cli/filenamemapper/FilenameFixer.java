/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.filenamemapper;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.armedia.commons.utilities.Tools;

public class FilenameFixer {

	// This was modified from a string array to disable the possibility of it being modified
	// at runtime somewhow via subterfuge or other means. Since this is now schema-bound,
	// it cannot change unless the source code changes.
	private static enum WinForbidden {
		//
		CON,
		PRN,
		AUX,
		NUL,
		COM1,
		COM2,
		COM3,
		COM4,
		COM5,
		COM6,
		COM7,
		COM8,
		COM9,
		LPT1,
		LPT2,
		LPT3,
		LPT4,
		LPT5,
		LPT6,
		LPT7,
		LPT8,
		LPT9,
		//
		;
	}

	public static enum Mode {

		//
		WIN(255, "[\"*\\\\><?/:|\u0000]") {

			@Override
			protected String fixLength(String name) {
				// Mind the extension on the rename...
				String ext = FilenameUtils.getExtension(name);
				String base = FilenameUtils.getBaseName(name);
				int maxLength = this.maxLength;
				if (!StringUtils.isEmpty(ext)) {
					maxLength -= (ext.length() + 1);
				}
				base = base.substring(0, Math.min(base.length(), maxLength));
				if (!StringUtils.isEmpty(ext)) {
					name = String.format("%s.%s", base, ext);
				}

				if (name.length() > maxLength) {
					// If the length is still too long, then just use the base method
					// because the extension was likely too long
					name = super.fixLength(name);
				}
				return name;
			}

			@Override
			public boolean isValidFixChar(Character fixChar) {
				if (!super.isValidFixChar(fixChar)) { return false; }
				if (Character.isWhitespace(fixChar)) { return false; }
				if ('.' == fixChar) { return false; }
				return true;
			}

			@Override
			protected String fixNameChars(String name, Character fixChar) {
				// Windows has some extra rules as to how files shall not be named

				// File names may not end in one or more dots (.)
				name = name.replaceAll("\\.$", fixChar.toString());

				// File names may not end in one or more spaces
				name = name.replaceAll("\\s$", fixChar.toString());

				// File must also not be named any of the forbidden names (case insenstitive)
				for (WinForbidden f : WinForbidden.values()) {
					if (StringUtils.equalsIgnoreCase(f.name(), name)) {
						name = String.format("%s%s", fixChar.toString(), name);
						break;
					}
				}

				// That's it - this is now a clean windows filename
				return name;
			}
		},

		UNIX(255, "[/\0]"),
		//
		;

		protected final int maxLength;
		protected final String forbiddenChars;

		private Mode() {
			this(0, null);
		}

		private Mode(String forbiddenChars) {
			this(0, forbiddenChars);
		}

		private Mode(int maxLength, String forbiddenChars) {
			this.maxLength = maxLength;
			this.forbiddenChars = forbiddenChars;
			if (forbiddenChars != null) {
				try {
					Pattern.compile(forbiddenChars);
				} catch (PatternSyntaxException e) {
					throw new RuntimeException(
						String.format("Forbidden characters must be a valid pattern for %s", name()), e);
				}
			}
		}

		protected String fixNameChars(String name, Character fixChar) {
			return name;
		}

		protected String fixLength(String name) {
			return name.substring(0, Math.min(name.length(), this.maxLength));
		}

		public boolean isValidFixChar(Character fixChar) {
			if (this.forbiddenChars != null) { return !fixChar.toString().matches(this.forbiddenChars); }
			return true;
		}

		public final String fixName(String srcName, Character fixChar, boolean fixLength) {
			if (srcName == null) { throw new IllegalArgumentException("Must provide a name to fix"); }
			// If no fix is desired, return the same value
			if ((fixChar == null) && !fixLength) { return srcName; }

			// First, fix the characters
			if (fixChar != null) {
				// First, bulk-replace the forbidden characters
				if (this.forbiddenChars != null) {
					srcName = srcName.replaceAll(this.forbiddenChars, fixChar.toString());
				}
				// Now, perform any additional replacements based on special rules (i.e. allowed
				// last characters, first characters, etc.)
				srcName = fixNameChars(srcName, fixChar);
			}

			// Now, fix the length
			if (fixLength && (this.maxLength > 0) && (srcName.length() >= this.maxLength)) {
				srcName = fixLength(srcName);
			}

			return srcName;
		}

		public static Mode getDefault() {
			// Find the default for this platform...
			return SystemUtils.IS_OS_WINDOWS ? WIN : UNIX;
		}
	}

	public static final boolean DEFAULT_FIX_LENGTH = true;
	public static final Character DEFAULT_FIX_CHAR = '_';

	private final Mode mode;
	private final Character fixChar;
	private final boolean fixLength;

	public FilenameFixer() {
		this(null, FilenameFixer.DEFAULT_FIX_CHAR, FilenameFixer.DEFAULT_FIX_LENGTH);
	}

	public FilenameFixer(Mode mode) {
		this(mode, FilenameFixer.DEFAULT_FIX_CHAR, FilenameFixer.DEFAULT_FIX_LENGTH);
	}

	public FilenameFixer(Character fixChar) {
		this(null, fixChar, FilenameFixer.DEFAULT_FIX_LENGTH);

	}

	public FilenameFixer(boolean fixLength) {
		this(null, FilenameFixer.DEFAULT_FIX_CHAR, fixLength);

	}

	public FilenameFixer(Mode mode, Character fixChar) {
		this(mode, fixChar, FilenameFixer.DEFAULT_FIX_LENGTH);

	}

	public FilenameFixer(Mode mode, boolean fixLength) {
		this(mode, FilenameFixer.DEFAULT_FIX_CHAR, fixLength);

	}

	public FilenameFixer(Mode mode, Character fixChar, boolean fixLength) {
		this.mode = Tools.coalesce(mode, Mode.getDefault());
		this.fixChar = fixChar;
		this.fixLength = fixLength;
	}

	public Mode getFixModel() {
		return this.mode;
	}

	public Character getFixChar() {
		return this.fixChar;
	}

	public boolean isFixLength() {
		return this.fixLength;
	}

	public final String fixName(String srcName) {
		return this.mode.fixName(srcName, this.fixChar, this.fixLength);
	}
}