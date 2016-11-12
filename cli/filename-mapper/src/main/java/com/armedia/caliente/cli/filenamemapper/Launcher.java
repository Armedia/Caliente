package com.armedia.caliente.cli.filenamemapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
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

import com.armedia.caliente.cli.filenamemapper.FilenameDeduplicator.ConflictResolver;
import com.armedia.caliente.cli.filenamemapper.FilenameDeduplicator.IdValidator;
import com.armedia.caliente.cli.filenamemapper.FilenameDeduplicator.Processor;
import com.armedia.caliente.cli.filenamemapper.tools.DfUtils;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.launcher.LaunchParameterSet;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.ParameterDefinition;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

public class Launcher extends AbstractLauncher implements LaunchParameterSet {

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

	public static enum Fixer {

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
						String.format("Forbidden characters must be a valid pattern for %s", name()), e);
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
			if (srcName == null) { throw new IllegalArgumentException("Must provide a name to fix"); }
			// If no fix is desired, return the same value
			if ((fixChar == null) && !fixLength) { return srcName; }

			// First, fix the characters
			if ((fixChar != null) && (this.forbiddenChars != null)) {
				srcName = srcName.replaceAll(this.forbiddenChars, fixChar.toString());
			}

			srcName = fixFinalName(srcName, Tools.coalesce(fixChar, Launcher.DEFAULT_FIX_CHAR));

			// Now, fix the length
			if ((this.maxLength > 0) && (srcName.length() >= this.maxLength) && fixLength) {
				srcName = fixLength(srcName);
			}

			return srcName;
		}

		public static Fixer getDefault() {
			// Find the default for this platform...
			return SystemUtils.IS_OS_WINDOWS ? WIN : UNIX;
		}
	}

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);

	@Override
	public Collection<? extends ParameterDefinition> getParameterDefinitions(CommandLineValues commandLine) {
		return Arrays.asList(CLIParam.values());
	}

	@Override
	protected Collection<? extends LaunchParameterSet> getLaunchParameterSets(CommandLineValues cli, int pass) {
		if (pass > 0) { return null; }
		return Arrays.asList(this, this.dfcLaunchHelper);
	}

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(CommandLineValues cli) {
		return Arrays.asList(this.dfcLaunchHelper);
	}

	@Override
	protected String getProgramName(int pass) {
		return "Caliente Filename Mapper and Deduplicator";
	}

	private final IdValidator<CmfObjectRef> idValidator = new IdValidator<CmfObjectRef>() {

		@Override
		public boolean isValidId(CmfObjectRef id) {
			if (id == null) { return false; }
			if (id.getType() == null) { return false; }
			if (StringUtils.isEmpty(id.getId())) { return false; }
			return (decodeType(id.getId()) != null);
		}

	};

	private CmfType decodeType(String idString) {
		if (idString == null) { throw new IllegalArgumentException(
			"Must provide an ID to decode the information from"); }
		// TODO: Not happy with this hardcoded table - maybe find a way to dynamically populate this
		// from the installation?
		final IDfId id = new DfId(idString);
		switch (id.getTypePart()) {
			case IDfId.DM_STORE:
				return CmfType.DATASTORE;

			case IDfId.DM_USER:
				return CmfType.USER;

			case IDfId.DM_GROUP:
				return CmfType.GROUP;

			case IDfId.DM_ACL:
				return CmfType.ACL;

			case IDfId.DM_TYPE:
				return CmfType.TYPE;

			case IDfId.DM_FORMAT:
				return CmfType.FORMAT;

			case IDfId.DM_CABINET: // fall-through, both folders and cabinets map as folders
			case IDfId.DM_FOLDER:
				return CmfType.FOLDER;

			case IDfId.DM_SYSOBJECT:
			case IDfId.DM_DOCUMENT:
				return CmfType.DOCUMENT;

			default:
				return null;
		}
	}

	private CmfObjectRef newObjectRef(CmfType type, String idString) {
		if (type == null) {
			// If we weren't given a type, then we try to identify it
			type = decodeType(idString);
			// If we failed to identify the type, then we can't return a reference
			if (type == null) { return null; }
		}
		return new CmfObjectRef(type, idString);
	}

	private CmfObjectRef newObjectRef(String idString) {
		return newObjectRef(null, idString);
	}

	private boolean checkDedupPattern(String pattern) {
		final Set<String> found = new HashSet<>();
		new StrSubstitutor(new StrLookup<String>() {
			@Override
			public String lookup(String key) {
				found.add(key);
				return key;
			}
		}).replace(pattern);
		return found.contains("id");
	}

	private String generateKey(CmfObjectRef entryId) {
		if (entryId == null) { return null; }
		return String.format("%s # %s", entryId.getType().name(), entryId.getId());
	}

	@Override
	protected int run(CommandLineValues cli) throws Exception {
		final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
		final String dctmUser = this.dfcLaunchHelper.getDfcUser(cli);
		final String dctmPass = this.dfcLaunchHelper.getDfcPassword(cli);

		final File targetFile;
		{
			String targetStr = cli.getString(CLIParam.target, Launcher.DEFAULT_TARGET);
			if (StringUtils.isEmpty(targetStr)) {
				this.log.error("Invalid empty filename given");
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
		final boolean dedupEnabled = !cli.isPresent(CLIParam.no_dedup);
		final String resolverPattern;
		if (dedupEnabled) {
			resolverPattern = cli.getString(CLIParam.dedup_pattern, Launcher.DEFAULT_DEDUP_PATTERN);
			if (!checkDedupPattern(resolverPattern)) {
				this.log.error("Illegal deduplication pattern - doesn't contain ${id}: [{}]", resolverPattern);
				return 1;
			}
		} else {
			resolverPattern = Launcher.DEFAULT_DEDUP_PATTERN;
		}

		if (cli.isPresent(CLIParam.no_fix)) {
			fixer = null;
			fixChar = null;
			fixLength = false;
		} else {
			if (cli.isPresent(CLIParam.fix_mode)) {
				String fixMode = cli.getString(CLIParam.fix_mode, "");
				try {
					fixer = Fixer.valueOf(fixMode);
				} catch (IllegalArgumentException e) {
					this.log.error("Invalid fix mode specified: [{}]", fixMode);
					return 1;
				}
			} else {
				fixer = Fixer.getDefault();
			}

			fixLength = !cli.isPresent(CLIParam.no_length_fix);

			if (cli.isPresent(CLIParam.no_char_fix)) {
				fixChar = null;
			} else {
				if (cli.isPresent(CLIParam.fix_char)) {
					String s = cli.getString(CLIParam.fix_char);
					if (StringUtils.isEmpty(s)) {
						this.log.error("The fix_char string must be at least 1 character long");
						return 1;
					}
					fixChar = s.charAt(0);
					if (!fixer.isValidFixChar(fixChar)) {
						this.log.error("The character [{}] is not a valid fix character for fix mode {}", fixChar,
							fixer.name());
						return 1;
					}
				} else {
					fixChar = Launcher.DEFAULT_FIX_CHAR;
				}
			}
		}

		final DfcSessionPool dfcPool;
		try {
			dfcPool = new DfcSessionPool(docbase, dctmUser, dctmPass);
		} catch (DfException e) {
			this.log.error(String.format("Failed to open the session pool to docbase [%s] as [%s]", docbase, dctmUser),
				e);
			return 1;
		}

		try {
			final FilenameDeduplicator<CmfObjectRef> deduplicator = new FilenameDeduplicator<>(this.idValidator,
				cli.isPresent(CLIParam.ignore_case));

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

					this.log.info("Executing the main query");
					collection = DfUtils.executeQuery(session, Launcher.ALL_DQL);
					long count = 0;
					final long start = System.currentTimeMillis();
					this.log.info("Query ready, iterating over the results");
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
								String key = generateKey(newObjectRef(entryId));
								if (key != null) {
									finalMap.setProperty(key, name);
								}
							}
						}

						if (dedupEnabled) {
							CmfObjectRef containerRef = newObjectRef(CmfType.FOLDER, containerId);
							CmfObjectRef entryRef = newObjectRef(entryId);
							if ((containerRef != null) && (entryRef != null)) {
								deduplicator.addEntry(containerRef, entryRef, name);
							}
						}

						if ((++count % 1000) == 0) {
							double duration = (System.currentTimeMillis() - start);
							double metric = ((count * 1000) / duration);
							this.log.info("Loaded {} entries (~{}/s)", count, (long) metric);
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
						this.log.warn("Failed to rollback the read-only transaction", e);
					}

				}

				if (dedupEnabled) {
					final long memPost = runtime.maxMemory() - runtime.freeMemory();
					this.log.info("Memory consumption: {}", memPost - memPre);
					this.log.info("Found the following naming conflicts:");
					deduplicator.showConflicts(this.log);
					this.log.info("Will resolve any conflicts using the pattern [{}]", resolverPattern);
					final Map<String, Object> resolverMap = new HashMap<>();
					// This is the only one that never changes...so we add it once and never again
					resolverMap.put("fixChar", Tools.coalesce(fixChar, Launcher.DEFAULT_FIX_CHAR).toString());
					long fixes = deduplicator.fixConflicts(new ConflictResolver<CmfObjectRef>() {
						@Override
						public String resolveConflict(CmfObjectRef entryId, String currentName, long count) {
							resolverMap.put("typeName", entryId.getType().name());
							resolverMap.put("typeOrdinal", entryId.getType().ordinal());
							resolverMap.put("id", entryId.getId());
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
					this.log.info("Conflicts fixed: {}", fixes);
					deduplicator.processRenamedEntries(new Processor<CmfObjectRef>() {
						@Override
						public void processEntry(CmfObjectRef entryId, String entryName) {
							String key = generateKey(entryId);
							if (key != null) {
								finalMap.setProperty(key, entryName);
							}
						}
					});
				}

				// Output the properties...
				if (finalMap.isEmpty()) {
					this.log.warn("No mappings to output, will not generate a mapping file");
				} else {
					this.log.info("Outputting the properties map to [{}]", targetFile.getAbsolutePath());
					OutputStream out = new FileOutputStream(targetFile);
					try {
						finalMap.storeToXML(out, null);
					} finally {
						IOUtils.closeQuietly(out);
					}
					this.log.info("Output {} mappings to [{}]", finalMap.size(), targetFile.getAbsolutePath());
				}
			} finally {
				dfcPool.releaseSession(session);
			}

			this.log.info("File generation completed");
			return 0;
		} finally {
			dfcPool.close();
		}
	}
}