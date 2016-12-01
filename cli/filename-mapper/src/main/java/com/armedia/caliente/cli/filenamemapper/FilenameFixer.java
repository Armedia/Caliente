package com.armedia.caliente.cli.filenamemapper;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.armedia.commons.utilities.Tools;

public class FilenameFixer {

	public static enum FixModel {

		//
		WIN(255, "[\"*\\\\><?/:|\u0000]") {
			private final String[] forbidden = {
				"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
				"LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
			};

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
				for (String f : this.forbidden) {
					if (f.equalsIgnoreCase(name)) {
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

		private FixModel() {
			this(0, null);
		}

		private FixModel(String forbiddenChars) {
			this(0, forbiddenChars);
		}

		private FixModel(int maxLength, String forbiddenChars) {
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

		public static FixModel getDefault() {
			// Find the default for this platform...
			return SystemUtils.IS_OS_WINDOWS ? WIN : UNIX;
		}
	}

	public static final boolean DEFAULT_FIX_LENGTH = true;
	public static final Character DEFAULT_FIX_CHAR = '_';

	private final FixModel fixModel;
	private final Character fixChar;
	private final boolean fixLength;

	public FilenameFixer() {
		this(null, FilenameFixer.DEFAULT_FIX_CHAR, FilenameFixer.DEFAULT_FIX_LENGTH);
	}

	public FilenameFixer(FixModel fixModel) {
		this(fixModel, FilenameFixer.DEFAULT_FIX_CHAR, FilenameFixer.DEFAULT_FIX_LENGTH);
	}

	public FilenameFixer(Character fixChar) {
		this(null, fixChar, FilenameFixer.DEFAULT_FIX_LENGTH);

	}

	public FilenameFixer(boolean fixLength) {
		this(null, FilenameFixer.DEFAULT_FIX_CHAR, fixLength);

	}

	public FilenameFixer(FixModel fixModel, Character fixChar) {
		this(fixModel, fixChar, FilenameFixer.DEFAULT_FIX_LENGTH);

	}

	public FilenameFixer(FixModel fixModel, boolean fixLength) {
		this(fixModel, FilenameFixer.DEFAULT_FIX_CHAR, fixLength);

	}

	public FilenameFixer(FixModel fixModel, Character fixChar, boolean fixLength) {
		this.fixModel = Tools.coalesce(fixModel, FixModel.getDefault());
		this.fixChar = fixChar;
		this.fixLength = fixLength;
	}

	public FixModel getFixModel() {
		return this.fixModel;
	}

	public Character getFixChar() {
		return this.fixChar;
	}

	public boolean isFixLength() {
		return this.fixLength;
	}

	public final String fixName(String srcName) {
		return this.fixModel.fixName(srcName, this.fixChar, this.fixLength);
	}
}