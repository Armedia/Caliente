package com.armedia.caliente.cli.filenamemapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.filenamemapper.FilenameDeduplicator.FilenameCollisionResolver;
import com.armedia.caliente.cli.filenamemapper.FilenameDeduplicator.IdValidator;
import com.armedia.caliente.cli.filenamemapper.FilenameDeduplicator.RenamedEntryProcessor;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.tools.dfc.DctmCrypto;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

class FilenameMapper {

	private static final Character DEFAULT_FIX_CHAR = '_';

	private static final String DEFAULT_TARGET = "filenamemap.xml";

	private static final String DEFAULT_DEDUP_PATTERN = "${name}${fixChar}${id}";

	private static final String ALL_DQL = //
		"         select r_object_id, object_name, i_folder_id " + //
			"       from dm_sysobject " + //
			"      where not folder('/Integration', DESCEND) " + //
			"        and not folder('/dm_bof_registry', DESCEND) " + //
			"        and not folder('/Resources', DESCEND) " + //
			"        and not folder('/System', DESCEND) " + //
			"        and not folder('/Temp', DESCEND) " + //
			"        and not folder('/Templates', DESCEND) ";

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final DfcLaunchHelper dfcLaunchHelper;

	FilenameMapper(DfcLaunchHelper dfcLaunchHelper) {
		this.dfcLaunchHelper = dfcLaunchHelper;
	}

	private final IdValidator idValidator = new IdValidator() {

		@Override
		public boolean isValidId(CmfObjectRef id) {
			if (id == null) { return false; }
			if (id.getType() == null) { return false; }
			if (StringUtils.isEmpty(id.getId())) { return false; }
			return (decodeType(id.getId()) != null);
		}

	};

	private CmfType decodeType(String idString) {
		if (idString == null) {
			throw new IllegalArgumentException("Must provide an ID to decode the information from");
		}
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
		new StringSubstitutor((key) -> {
			found.add(key);
			return key;
		}).replace(pattern);
		return found.contains("id");
	}

	private String generateKey(CmfObjectRef entryId) {
		if (entryId == null) { return null; }
		return String.format("%s # %s", entryId.getType().name(), entryId.getId());
	}

	private FilenameFixer configureFilenameFixer(OptionValues cli) throws CliParameterException {
		if (cli.isPresent(CLIParam.no_fix)) { return null; }
		final FilenameFixer.Mode fixerModel;
		final Character fixChar;
		final boolean fixLength;
		if (cli.isPresent(CLIParam.fix_mode)) {
			String fixMode = cli.getString(CLIParam.fix_mode, "");
			try {
				fixerModel = FilenameFixer.Mode.valueOf(fixMode);
			} catch (IllegalArgumentException e) {
				throw new CliParameterException(String.format("Invalid fix mode specified: [%s]", fixMode));
			}
		} else {
			fixerModel = FilenameFixer.Mode.getDefault();
		}

		fixLength = !cli.isPresent(CLIParam.no_length_fix);

		if (cli.isPresent(CLIParam.no_char_fix)) {
			fixChar = null;
		} else {
			if (cli.isPresent(CLIParam.fix_char)) {
				String s = cli.getString(CLIParam.fix_char);
				if (StringUtils.isEmpty(s)) {
					throw new CliParameterException("The fix_char string must be at least 1 character long");
				}
				fixChar = s.charAt(0);
				if (!fixerModel.isValidFixChar(fixChar)) {
					throw new CliParameterException(String.format(
						"The character [%s] is not a valid fix character for fix mode %s", fixChar, fixerModel.name()));
				}
			} else {
				fixChar = FilenameMapper.DEFAULT_FIX_CHAR;
			}
		}
		return new FilenameFixer(fixerModel, fixChar, fixLength);
	}

	protected int run(OptionValues cli) throws Exception {
		final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
		final String dctmUser = this.dfcLaunchHelper.getDfcUser(cli);
		final String dctmPass = this.dfcLaunchHelper.getDfcPassword(cli);

		final File targetFile;
		{
			String targetStr = cli.getString(CLIParam.target, FilenameMapper.DEFAULT_TARGET);
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

		final boolean dedupEnabled = !cli.isPresent(CLIParam.no_dedup);
		final String resolverPattern;
		if (dedupEnabled) {
			resolverPattern = cli.getString(CLIParam.dedup_pattern, FilenameMapper.DEFAULT_DEDUP_PATTERN);
			if (!checkDedupPattern(resolverPattern)) {
				this.log.error("Illegal deduplication pattern - doesn't contain ${id}: [{}]", resolverPattern);
				return 1;
			}
		} else {
			resolverPattern = FilenameMapper.DEFAULT_DEDUP_PATTERN;
		}

		final FilenameFixer fixer;
		try {
			fixer = configureFilenameFixer(cli);
		} catch (CliParameterException e) {
			this.log.error(e.getMessage());
			return 1;
		}

		final DfcSessionPool dfcPool;
		try {
			dfcPool = new DfcSessionPool(docbase, dctmUser, new DctmCrypto().decrypt(dctmPass));
		} catch (DfException e) {
			this.log.error(String.format("Failed to open the session pool to docbase [%s] as [%s]", docbase, dctmUser),
				e);
			return 1;
		}

		try {
			final FilenameDeduplicator deduplicator = new FilenameDeduplicator(this.idValidator,
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
					collection = DfUtils.executeQuery(session, FilenameMapper.ALL_DQL);
					long count = 0;
					final long start = System.currentTimeMillis();
					this.log.info("Query ready, iterating over the results");
					memPre = runtime.maxMemory() - runtime.freeMemory();
					final boolean folderIdRepeating = collection.isAttrRepeating("i_folder_id");
					while (collection.next()) {
						String entryId = collection.getString("r_object_id");
						String name = collection.getString("object_name");

						// First things first: make sure the filename is fixed
						if (fixer != null) {
							final String oldName = name;
							// Empty names get modified into their object IDs...
							if (StringUtils.isEmpty(name)) {
								name = entryId;
							}
							name = fixer.fixName(name);
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
							CmfObjectRef entryRef = newObjectRef(entryId);
							if (folderIdRepeating) {
								final int c = collection.getValueCount("i_folder_id");
								for (int i = 0; i < c; i++) {
									String containerId = collection.getRepeatingString("i_folder_id", i);
									CmfObjectRef containerRef = newObjectRef(CmfType.FOLDER, containerId);
									if ((containerRef != null) && (entryRef != null)) {
										deduplicator.addEntry(containerRef, entryRef, name);
									}
								}
							} else {
								String containerId = collection.getString("i_folder_id");
								CmfObjectRef containerRef = newObjectRef(CmfType.FOLDER, containerId);
								if ((containerRef != null) && (entryRef != null)) {
									deduplicator.addEntry(containerRef, entryRef, name);
								}
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
					resolverMap.put("fixChar",
						Tools.coalesce(fixer != null ? fixer.getFixChar() : null, FilenameMapper.DEFAULT_FIX_CHAR)
							.toString());
					long fixes = deduplicator.fixConflicts(new FilenameCollisionResolver() {
						@Override
						public String generateUniqueName(CmfObjectRef entryId, String currentName, long count) {
							// Empty names get modified into their object IDs...
							if (StringUtils.isEmpty(currentName)) {
								currentName = entryId.getId();
							}
							resolverMap.put("typeName", entryId.getType().name());
							resolverMap.put("typeOrdinal", entryId.getType().ordinal());
							resolverMap.put("id", entryId.getId());
							resolverMap.put("name", currentName);
							resolverMap.put("count", count);
							String newName = StringSubstitutor.replace(resolverPattern, resolverMap);
							if (fixer != null) {
								// Make sure we use a clean name...
								newName = fixer.fixName(newName);
							}
							return newName;
						}
					});
					this.log.info("Conflicts fixed: {}", fixes);
					deduplicator.processRenamedEntries(new RenamedEntryProcessor() {
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
					try (OutputStream out = new FileOutputStream(targetFile)) {
						finalMap.storeToXML(out, null);
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