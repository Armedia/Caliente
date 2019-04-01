package com.armedia.caliente.cli.datagen;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.datagen.data.csv.CSVDataRecordManager;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.tools.dfc.DctmCrypto;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.BinaryMemoryBuffer;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

public class DataGen {
	private static final String DEFAULT_NAME_FORMAT = "${type}-[${id}]";

	private static final Random RANDOM = new Random(System.currentTimeMillis());
	private static final Pattern SIZE_PATTERN = Pattern.compile("^([1-9][0-9]*)([mk]b?)?$", Pattern.CASE_INSENSITIVE);

	private static final int KB = 1024;
	private static final int MB = DataGen.KB * DataGen.KB;
	private static final int BUFFER_CHUNK_SIZE = 16 * DataGen.KB;
	private static final int MIN_DOC_SIZE = 1;
	private static final int MAX_DOC_SIZE = 16 * DataGen.MB;

	private static class CSVRM extends CSVDataRecordManager {

		private final File baseDir;

		private CSVRM() {
			this(null);
		}

		private CSVRM(String baseDir) {
			if (StringUtils.isEmpty(baseDir)) {
				baseDir = ".";
			}
			this.baseDir = Tools.canonicalize(new File(baseDir));
		}

		private URL findFile(String name) throws IOException {
			if (StringUtils.isEmpty(name)) { return null; }
			File f = new File(this.baseDir, name);
			if (!f.exists()) {
				if (this.log.isDebugEnabled()) {
					this.log.warn("The file [{}] does not exist", f.getAbsolutePath());
				}
				return null;
			}
			if (!f.isFile()) {
				if (this.log.isDebugEnabled()) {
					this.log.warn("The file [{}] is not a regular file", f.getAbsolutePath());
				}
				return null;
			}
			if (!f.canRead()) {
				if (this.log.isDebugEnabled()) {
					this.log.warn("The file [{}] cannot be read", f.getAbsolutePath());
				}
				return null;
			}
			f = Tools.canonicalize(f);
			return f.toURI().toURL();
		}

		@Override
		protected URL findStreamRecords() throws IOException {
			URL url = findFile("datagen.stream-records.csv");
			if (url != null) {
				this.log.info("Datastream information will be loaded from [{}]", url.toString());
			} else {
				this.log.warn("No datastream information was supplied");
			}
			return url;
		}

		@Override
		protected URL findTypeRecords(String type) throws IOException {
			if (type == null) {
				type = "(none)";
			}
			URL url = findFile(String.format("datagen.%s.csv", type));
			if (url != null) {
				this.log.info("Metadata values for type [{}] will be loaded from [{}]", type, url.toString());
			} else {
				this.log.warn("No metadata file found for object type [{}]", type);
			}
			return url;
		}
	}

	private static Integer parseInt(String cliValue) {
		Matcher m = DataGen.SIZE_PATTERN.matcher(cliValue);
		if (!m.matches()) { return null; }

		int v = Integer.valueOf(m.group(1));
		String suffix = m.group(2);
		if (suffix != null) {
			switch (suffix.charAt(0)) {
				case 'm':
					v *= DataGen.MB;
					break;
				case 'k':
					v *= DataGen.KB;
					break;
				default:
					break;
			}
		}
		return v;
	}

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ThreadsLaunchHelper threadsParameter;
	private final DfcLaunchHelper dfcLaunchHelper;

	public DataGen(ThreadsLaunchHelper threadsParameter, DfcLaunchHelper dfcLaunchHelper) {
		this.threadsParameter = threadsParameter;
		this.dfcLaunchHelper = dfcLaunchHelper;
	}

	protected int run(OptionValues cli) throws Exception {
		final boolean debug = cli.isPresent(CLIParam.debug);

		List<String> objectTypes = cli.getStrings(CLIParam.object_types);
		if ((objectTypes == null) || objectTypes.isEmpty()) {
			objectTypes = Collections.emptyList();
		}

		try {
			final String nameFormat = cli.getString(CLIParam.name_format, DataGen.DEFAULT_NAME_FORMAT);
			if (!StringUtils.equals(nameFormat, DataGen.DEFAULT_NAME_FORMAT)) {
				// If a non-default name format is provided, we have to do QA on it
				final Set<String> provided = new HashSet<>();
				StringSubstitutor subs = new StringSubstitutor((k) -> {
					provided.add(k);
					return "";
				});
				subs.replace(nameFormat);
				if (!provided.contains("id") && !provided.contains("uuid")) {
					this.log.error(
						"The name format must contain one of ${id} or ${uuid} markers, to avoid object duplicity");
					return 1;
				}
			}

			final int minDocSize;
			final int maxDocSize;
			{
				String minStr = cli.getString(CLIParam.document_min_size, String.valueOf(DataGen.MIN_DOC_SIZE))
					.toLowerCase();
				Integer min = DataGen.parseInt(minStr);
				if (min == null) {
					this.log.error("Bad number format (--document-min-size): [{}]", minStr);
					return 1;
				}
				min = Tools.ensureBetween(DataGen.MIN_DOC_SIZE, min, DataGen.MAX_DOC_SIZE);

				String maxStr = cli.getString(CLIParam.document_max_size, String.valueOf(DataGen.MAX_DOC_SIZE))
					.toLowerCase();
				Integer max = DataGen.parseInt(maxStr);
				if (max == null) {
					this.log.error("Bad number format (--document-max-size): [{}]", maxStr);
					return 1;
				}
				max = Tools.ensureBetween(DataGen.MIN_DOC_SIZE, max, DataGen.MAX_DOC_SIZE);

				if (min > max) {
					// Flip them around
					int l = min;
					min = max;
					max = l;
				}
				minDocSize = min;
				maxDocSize = max;
			}

			final BinaryMemoryBuffer BASE_BUFFER;
			try (BinaryMemoryBuffer buffer = new BinaryMemoryBuffer(DataGen.BUFFER_CHUNK_SIZE)) {
				final int chunkCount = (maxDocSize / buffer.getChunkSize());
				byte[] buf = new byte[buffer.getChunkSize()];
				for (int i = 0; i < chunkCount; i++) {
					DataGen.RANDOM.nextBytes(buf);
					try {
						buffer.write(buf);
					} catch (IOException e) {
						// Can't continue...
						this.log.warn("Unexpected exception writing to memory", e);
						return 1;
					}
				}

				final int chunkRemainder = (maxDocSize % buffer.getChunkSize());
				DataGen.RANDOM.nextBytes(buf);
				try {
					buffer.write(buf, 0, chunkRemainder);
				} catch (IOException e) {
					// Can't continue...
					this.log.warn("Unexpected exception writing to memory", e);
					return 1;
				}

				BASE_BUFFER = buffer;
				BASE_BUFFER.close();
			}

			this.log.info("Random data buffer of {} bytes ready", BASE_BUFFER.getCurrentSize());

			final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
			final String user = this.dfcLaunchHelper.getDfcUser(cli);
			final String password = this.dfcLaunchHelper.getDfcPassword(cli);

			final DfcSessionPool pool = new DfcSessionPool(docbase, user, new DctmCrypto().decrypt(password));

			try {
				final IDfSession mainSession;
				try {
					mainSession = pool.acquireSession();
				} catch (Exception e) {
					String msg = String.format("Failed to open a session to docbase [%s] as user [%s]", docbase, user);
					if (debug) {
						this.log.error(msg, e);
					} else {
						this.log.error("{}: {}", msg, e.getMessage());
					}
					return 1;
				}

				final String target = cli.getString(CLIParam.target);
				final int treeDepth = cli.getInteger(CLIParam.tree_depth, 1);
				final int folderCount = cli.getInteger(CLIParam.folder_count, 1);
				final int documentCount = cli.getInteger(CLIParam.document_count, 1);

				try {
					final IDfFolder root = NodeGenerator.ensureFolder(mainSession, target);
					final NodeGenerator generator = new NodeGenerator(mainSession, objectTypes, new CSVRM());

					final int threads = this.threadsParameter.getThreads(cli);
					final ExecutorService executor = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS,
						new ArrayBlockingQueue<Runnable>(threads, true));

					final BlockingQueue<IDfId> queue = new ArrayBlockingQueue<>(10000, true);
					final List<Future<Integer>> futures = new ArrayList<>(threads);
					for (int i = 0; i < threads; i++) {
						futures.add(executor.submit(new Callable<Integer>() {
							@Override
							public Integer call() {
								final IDfSession session;
								try {
									session = pool.acquireSession();
								} catch (DfException e) {
									String msg = "Failed to obtain a client session for processing";
									if (debug) {
										DataGen.this.log.error(msg, e);
									} else {
										DataGen.this.log.error("{}: {}", msg, e.getMessage());
									}
									return null;
								}
								try {
									int total = 0;
									while (true) {
										IDfId id = null;
										try {
											id = queue.take();
										} catch (InterruptedException e) {
											// Thread was interrupted waiting on the queue, so we
											// exit gracefully
											String msg = "Thread interrupted while waiting on the queue, exiting";
											if (debug) {
												DataGen.this.log.error(msg, e);
											} else {
												DataGen.this.log.error(msg);
											}
											return null;
										}

										if ((id == null) || id.isNull()) {
											DataGen.this.log.info("Thread completed");
											break;
										}
										String path = "<unknown>";
										try {
											final IDfFolder folder = session.getFolderBySpecification(id.getId());
											if (folder == null) {
												continue;
											}
											path = folder.getFolderPath(0);
											int size = minDocSize;
											if (minDocSize != maxDocSize) {
												size += DataGen.RANDOM.nextInt(maxDocSize - minDocSize);
											}
											total += generator.generateDocuments(folder, documentCount, nameFormat,
												BASE_BUFFER.getInputStream(), size);
										} catch (DfException e) {
											DataGen.this.log.error(
												"Failed to generate the documents at ID [{}] with path [{}]",
												id.getId(), path, e);
										}
									}
									return total;
								} finally {
									pool.releaseSession(session);
								}
							}
						}));
					}
					executor.shutdown();

					int size = minDocSize;
					if (minDocSize != maxDocSize) {
						size += DataGen.RANDOM.nextInt(maxDocSize - minDocSize);
					}
					int total = generator.generateDocuments(root, documentCount, nameFormat,
						BASE_BUFFER.getInputStream(), size);
					try {
						total += generator.generateFolders(queue, root, folderCount, treeDepth, nameFormat);
					} catch (InterruptedException e) {
						String msg = "Launcher thread interrupted while generating the folder tree";
						if (debug) {
							this.log.error(msg, e);
						} else {
							this.log.error(msg);
						}
					} finally {
						// The last items should be null IDs to ensure that the threads end
						// cleanly when they reach them
						for (int i = 0; i < threads; i++) {
							try {
								queue.put(DfId.DF_NULLID);
							} catch (InterruptedException e) {
								String msg = "Launcher thread interrupted while submitting the end markers";
								if (debug) {
									this.log.error(msg, e);
								} else {
									this.log.error(msg);
								}
							}
						}
					}

					int finished = 0;
					for (Future<Integer> f : futures) {
						if (f.isCancelled()) {
							continue;
						}
						try {
							Integer value = f.get();
							if (value == null) {
								// This only happens when an exception is raised, and it's already
								// reported, so we continue
								continue;
							}
							total += value.intValue();
							finished++;
						} catch (InterruptedException e) {
							if (Thread.currentThread().isInterrupted()) {
								Thread.currentThread().interrupt();
							}
							String msg = "Thread interrupted retrieving results";
							if (debug) {
								this.log.warn(msg, e);
							} else {
								this.log.warn(msg);
							}
							break;
						} catch (ExecutionException e) {
							String msg = "Exception raised from a Document generator";
							if (debug) {
								this.log.error(msg, e);
							} else {
								this.log.error("{}: {}", msg, e.getMessage());
							}
						}
					}

					if (finished != futures.size()) {
						this.log.error("Did not finish processing: processed only {} out of {} generators", finished,
							futures.size());
					}

					this.log.info("Generated {} objects", total);
					return 0;
				} finally {
					pool.releaseSession(mainSession);
				}
			} finally {
				pool.close();
			}
		} catch (DfException e) {
			this.log.error("Documentum exception caught", e);
			return 1;
		}
	}
}