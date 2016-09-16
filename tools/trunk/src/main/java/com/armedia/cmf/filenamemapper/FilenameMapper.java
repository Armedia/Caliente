package com.armedia.cmf.filenamemapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.filenamemapper.FilenameDeduplicator.ConflictResolver;
import com.armedia.cmf.filenamemapper.FilenameDeduplicator.IdValidator;
import com.armedia.cmf.filenamemapper.FilenameDeduplicator.Processor;
import com.armedia.cmf.filenamemapper.tools.DfUtils;
import com.armedia.cmf.storage.CmfType;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

public class FilenameMapper {

	private static enum Fixer {

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
				int maxLength = this.maxLength;
				if (!StringUtils.isEmpty(ext)) {
					maxLength -= (ext.length() + 1);
				}
				name = name.substring(0, Math.min(name.length(), maxLength));
				if (!StringUtils.isEmpty(ext)) {
					name = String.format("%s.%s", name, ext);
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
			protected String fixFinalName(String name, Character fixChar) {
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

		private Fixer() {
			this(0, null);
		}

		private Fixer(String forbiddenChars) {
			this(0, forbiddenChars);
		}

		private Fixer(int maxLength, String forbiddenChars) {
			this.maxLength = maxLength;
			this.forbiddenChars = forbiddenChars;
			if (forbiddenChars != null) {
				try {
					Pattern.compile(forbiddenChars);
				} catch (PatternSyntaxException e) {
					throw new RuntimeException(
						String.format("Forbidden characters must be a valid pattern for %s", this.name()), e);
				}
			}
		}

		protected String fixFinalName(String name, Character fixChar) {
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
			// If no fix is desired, return the same value
			if ((fixChar == null) && !fixLength) { return srcName; }

			// First, fix the characters
			if ((fixChar != null) && (this.forbiddenChars != null)) {
				srcName = srcName.replaceAll(this.forbiddenChars, fixChar.toString());
			}

			srcName = fixFinalName(srcName, Tools.coalesce(fixChar, FilenameMapper.DEFAULT_FIX_CHAR));

			// Now, fix the length
			if ((this.maxLength > 0) && fixLength) {
				srcName = fixLength(srcName);
			}

			return srcName;
		}

		public static Fixer getDefault() {
			// Find the default for this platform...
			return SystemUtils.IS_OS_WINDOWS ? WIN : UNIX;
		}
	}

	private static final IdValidator ID_VALIDATOR = new IdValidator() {

		@Override
		public boolean isValidId(String id) {
			if (StringUtils.isEmpty(id)) { return false; }
			return (FilenameMapper.decodeType(id) != null);
		}

	};

	private static final Logger log = LoggerFactory.getLogger(FilenameMapper.class);

	private static final Character DEFAULT_FIX_CHAR = '_';

	private static final String DEFAULT_TARGET = "filenamemap.xml";

	private static final String DEFAULT_DEDUP_PATTERN = "${name}${fixChar}${id}";

	private static final String ALL_DQL = //
		"         select r_object_id, i_folder_id, object_name " + //
			"       from dm_sysobject " + //
			"      where object_name is not nullstring " + //
			"        and r_object_id is not nullstring " + //
			"        and r_object_id is not nullid " + //
			"        and i_folder_id is not nullstring " + //
			"        and i_folder_id is not nullid " + //
			"        and not folder('/Integration', DESCEND) " + //
			"        and not folder('/dm_bof_registry', DESCEND) " + //
			"        and not folder('/Resources', DESCEND) " + //
			"        and not folder('/System', DESCEND) " + //
			"        and not folder('/Temp', DESCEND) " + //
			"        and not folder('/Templates', DESCEND) " + //
			"            enable (ROW_BASED) ";

	private static CmfType decodeType(String idString) {
		if (idString == null) { throw new IllegalArgumentException(
			"Must provide an ID to decode the information from"); }
		// TODO: Not happy with this hardcoded table - maybe find a way to dynamically populate this
		// from the installation?
		final IDfId id = new DfId(idString);
		switch (id.getTypePart()) {
			case 0x28:
				return CmfType.DATASTORE;

			case 0x11:
				return CmfType.USER;

			case 0x12:
				return CmfType.GROUP;

			case 0x45:
				return CmfType.ACL;

			case 0x03:
				return CmfType.TYPE;

			case 0x27:
				return CmfType.FORMAT;

			case 0x0b: // fall-through, both folders and cabinets map as folders
			case 0x0c:
				return CmfType.FOLDER;

			case 0x09:
				return CmfType.DOCUMENT;

			default:
				return null;
		}
	}

	private static boolean checkDedupPattern(String pattern) {
		final Set<String> found = new HashSet<String>();
		new StrSubstitutor(new StrLookup<String>() {
			@Override
			public String lookup(String key) {
				found.add(key);
				return key;
			}
		}).replace(pattern);
		return found.contains("id");
	}

	private static String generateKey(String entryId) {
		CmfType t = FilenameMapper.decodeType(entryId);
		if (t == null) { return null; }
		return String.format("%s # %s", t.name(), entryId);
	}

	static int run() throws Exception {
		final String docbase = CLIParam.docbase.getString();
		final String dctmUser = CLIParam.dctm_user.getString();
		final String dctmPass = CLIParam.dctm_pass.getString();

		final File targetFile;
		{
			String targetStr = CLIParam.target.getString(FilenameMapper.DEFAULT_TARGET);
			if (StringUtils.isEmpty(targetStr)) {
				FilenameMapper.log.error("Invalid empty filename given");
				return 1;
			}
			File f = new File(targetStr);
			try {
				f = f.getCanonicalFile();
			} catch (IOException e) {
				f = f.getAbsoluteFile();
			}
			targetFile = f;
		}

		final Fixer fixer;
		final Character fixChar;
		final boolean fixLength;
		final boolean dedupEnabled = !CLIParam.no_dedup.isPresent();
		final String resolverPattern;
		if (dedupEnabled) {
			resolverPattern = CLIParam.dedup_pattern.getString(FilenameMapper.DEFAULT_DEDUP_PATTERN);
			if (!FilenameMapper.checkDedupPattern(resolverPattern)) {
				FilenameMapper.log.error("Illegal deduplication pattern - doesn't contain ${id}: [{}]",
					resolverPattern);
				return 1;
			}
		} else {
			resolverPattern = FilenameMapper.DEFAULT_DEDUP_PATTERN;
		}

		if (CLIParam.no_fix.isPresent()) {
			fixer = null;
			fixChar = null;
			fixLength = false;
		} else {
			if (CLIParam.fix_mode.isPresent()) {
				String fixMode = CLIParam.fix_mode.getString("");
				try {
					fixer = Fixer.valueOf(fixMode);
				} catch (IllegalArgumentException e) {
					FilenameMapper.log.error("Invalid fix mode specified: [{}]", fixMode);
					return 1;
				}
			} else {
				fixer = Fixer.getDefault();
			}

			fixLength = !CLIParam.no_length_fix.isPresent();

			if (CLIParam.no_char_fix.isPresent()) {
				fixChar = null;
			} else {
				if (CLIParam.fix_char.isPresent()) {
					String s = CLIParam.fix_char.getString();
					if (StringUtils.isEmpty(s)) {
						FilenameMapper.log.error("The fix_char string must be at least 1 character long");
						return 1;
					}
					fixChar = s.charAt(0);
					if (!fixer.isValidFixChar(fixChar)) {
						FilenameMapper.log.error("The character [{}] is not a valid fix character for fix mode {}",
							fixChar, fixer.name());
						return 1;
					}
				} else {
					fixChar = FilenameMapper.DEFAULT_FIX_CHAR;
				}
			}
		}

		final DfcSessionPool dfcPool;
		try {
			dfcPool = new DfcSessionPool(docbase, dctmUser, dctmPass);
		} catch (DfException e) {
			FilenameMapper.log
				.error(String.format("Failed to open the session pool to docbase [%s] as [%s]", docbase, dctmUser), e);
			return 1;
		}

		try {
			final FilenameDeduplicator deduplicator = new FilenameDeduplicator(FilenameMapper.ID_VALIDATOR);

			IDfSession session = dfcPool.acquireSession();
			final Runtime runtime = Runtime.getRuntime();
			final Properties finalMap = new Properties();
			final long memPre;
			try {
				IDfLocalTransaction tx = null;
				IDfCollection collection = null;
				try {
					if (session.isTransactionActive()) {
						tx = session.beginTransEx();
					} else {
						session.beginTrans();
					}

					FilenameMapper.log.info("Executing the main query");
					collection = DfUtils.executeQuery(session, FilenameMapper.ALL_DQL);
					long count = 0;
					final long start = System.currentTimeMillis();
					FilenameMapper.log.info("Query ready, iterating over the results");
					memPre = runtime.maxMemory() - runtime.freeMemory();
					while (collection.next()) {
						String containerId = collection.getString("i_folder_id");
						String entryId = collection.getString("r_object_id");
						String name = collection.getString("object_name");

						// First things first: make sure the filename is fixed
						if (fixer != null) {
							final String oldName = name;
							name = fixer.fixName(name, fixChar, fixLength);
							if (name == null) {
								name = oldName;
							}
							if (!Tools.equals(name, oldName)) {
								// If it was renamed, then the mapping is output, conflict
								// or no conflict. If the same entry later has a conflict,
								// it will be overwritten anyway...
								String key = FilenameMapper.generateKey(entryId);
								if (key != null) {
									finalMap.setProperty(key, name);
								}
							}
						}

						if (dedupEnabled) {
							deduplicator.addEntry(containerId, entryId, name);
						}

						if ((++count % 1000) == 0) {
							double duration = (System.currentTimeMillis() - start);
							double metric = ((count * 1000) / duration);
							FilenameMapper.log.info("Loaded {} entries (~{}/s)", count, (long) metric);
						}
					}
				} finally {
					DfUtils.closeQuietly(collection);
					try {
						if (tx != null) {
							session.abortTransEx(tx);
						} else {
							session.abortTrans();
						}
					} catch (DfException e) {
						FilenameMapper.log.warn("Failed to rollback the read-only transaction", e);
					}

				}

				if (dedupEnabled) {
					final long memPost = runtime.maxMemory() - runtime.freeMemory();
					FilenameMapper.log.info("Memory consumption: {}", memPost - memPre);
					FilenameMapper.log.info("Found the following naming conflicts:");
					deduplicator.showConflicts(FilenameMapper.log);
					FilenameMapper.log.info("Will resolve any conflicts using the pattern [{}]", resolverPattern);
					final Map<String, Object> resolverMap = new HashMap<String, Object>();
					// This is the only one that never changes...so we add it once and never again
					resolverMap.put("fixChar", Tools.coalesce(fixChar, FilenameMapper.DEFAULT_FIX_CHAR).toString());
					long fixes = deduplicator.fixConflicts(new ConflictResolver() {
						@Override
						public String resolveConflict(String entryId, String currentName, long count) {
							resolverMap.put("id", entryId);
							resolverMap.put("name", currentName);
							resolverMap.put("count", count);
							String newName = StrSubstitutor.replace(resolverPattern, resolverMap);
							if (fixer != null) {
								// Make sure we use a clean name...
								newName = fixer.fixName(newName, fixChar, fixLength);
							}
							return newName;
						}
					});
					FilenameMapper.log.info("Conflicts fixed: {}", fixes);
					deduplicator.processRenamedEntries(new Processor() {
						@Override
						public void processEntry(String entryId, String entryName) {
							String key = FilenameMapper.generateKey(entryId);
							if (key != null) {
								finalMap.setProperty(key, entryName);
							}
						}
					});
				}

				// Output the properties...
				if (finalMap.isEmpty()) {
					FilenameMapper.log.warn("No mappings to output, will not generate a mapping file");
				} else {
					FilenameMapper.log.info("Outputting the properties map to [{}]", targetFile.getAbsolutePath());
					OutputStream out = new FileOutputStream(targetFile);
					try {
						finalMap.storeToXML(out, null);
					} finally {
						IOUtils.closeQuietly(out);
					}
					FilenameMapper.log.info("Output {} mappings to [{}]", finalMap.size(),
						targetFile.getAbsolutePath());
				}
			} finally {
				dfcPool.releaseSession(session);
			}

			FilenameMapper.log.info("File generation completed");
			return 0;
		} finally {
			dfcPool.close();
		}
	}
}