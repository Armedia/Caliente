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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.datagen.data.csv.CSVDataRecordManager;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.BinaryMemoryBuffer;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

public class Main {
	protected static final int DEFAULT_THREADS = (Runtime.getRuntime().availableProcessors() / 2);
	protected static final String DFC_PROPERTIES_PROP = "dfc.properties.file";
	protected static final String DEFAULT_NAME_FORMAT = "${type}-[${id}]";

	private static final Random RANDOM = new Random(System.currentTimeMillis());
	private static final Pattern SIZE_PATTERN = Pattern.compile("^([1-9][0-9]*)([mk]b?)?$", Pattern.CASE_INSENSITIVE);

	private static final int KB = 1024;
	private static final int MB = Main.KB * Main.KB;
	private static final int BUFFER_CHUNK_SIZE = 16 * Main.KB;
	private static final int MIN_DOC_SIZE = 1;
	private static final int MAX_DOC_SIZE = 16 * Main.MB;

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
					this.log.warn(String.format("The file [%s] does not exist", f.getAbsolutePath()));
				}
				return null;
			}
			if (!f.isFile()) {
				if (this.log.isDebugEnabled()) {
					this.log.warn(String.format("The file [%s] is not a regular file", f.getAbsolutePath()));
				}
				return null;
			}
			if (!f.canRead()) {
				if (this.log.isDebugEnabled()) {
					this.log.warn(String.format("The file [%s] cannot be read", f.getAbsolutePath()));
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
				log.info("Datastream information will be loaded from [{}]", url.toString());
			} else {
				log.warn("No datastream information was supplied");
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
				log.info("Metadata values for type [{}] will be loaded from [{}]", type, url.toString());
			} else {
				log.warn("No metadata file found for object type [{}]", type);
			}
			return url;
		}
	}

	private static Integer parseInt(String cliValue) {
		Matcher m = Main.SIZE_PATTERN.matcher(cliValue);
		if (!m.matches()) { return null; }

		int v = Integer.valueOf(m.group(1));
		String suffix = m.group(2);
		if (suffix != null) {
			switch (suffix.charAt(0)) {
				case 'm':
					v *= Main.MB;
					break;
				case 'k':
					v *= Main.KB;
					break;
				default:
					break;
			}
		}
		return v;
	}

	public static final void main(String... args) {
		if (!CLIParam.parse(args)) {
			// If the parameters didn't parse, we fail.
			return;
		}

		final Logger log = LoggerFactory.getLogger(Main.class);
		final boolean debug = CLIParam.debug.isPresent();

		List<String> objectTypes = CLIParam.object_types.getAllString();
		if ((objectTypes == null) || objectTypes.isEmpty()) {
			objectTypes = Collections.emptyList();
		}

		if (CLIParam.dfc_prop.isPresent()) {
			File f = new File(CLIParam.dfc_prop.getString("dfc.properties"));
			try {
				f = f.getCanonicalFile();
			} catch (IOException e) {
				// Do nothing...stay with the non-canonical path
				f = f.getAbsoluteFile();
			}
			String error = null;
			if ((error == null) && !f.exists()) {
				error = "does not exist";
			}
			if ((error == null) && !f.isFile()) {
				error = "is not a regular file";
			}
			if ((error == null) && !f.canRead()) {
				error = "cannot be read";
			}
			if (error == null) {
				System.setProperty(Main.DFC_PROPERTIES_PROP, f.getAbsolutePath());
			} else {
				log.warn("The DFC properties file [{}] {} - will continue using DFC defaults", f.getAbsolutePath(),
					error);
			}
		}

		try {
			final String nameFormat = CLIParam.name_format.getString(Main.DEFAULT_NAME_FORMAT);
			if (!StringUtils.equals(nameFormat, Main.DEFAULT_NAME_FORMAT)) {
				// If a non-default name format is provided, we have to do QA on it
				final Set<String> provided = new HashSet<String>();
				StrSubstitutor subs = new StrSubstitutor(new StrLookup<String>() {
					@Override
					public String lookup(String key) {
						provided.add(key);
						return "";
					}
				});
				subs.replace(nameFormat);
				if (!provided.contains("id") && !provided.contains("uuid")) {
					log.error(
						"The name format must contain one of ${id} or ${uuid} markers, to avoid object duplicity");
					return;
				}
			}

			final int minDocSize;
			final int maxDocSize;
			{
				String minStr = CLIParam.document_min_size.getString(String.valueOf(Main.MIN_DOC_SIZE)).toLowerCase();
				Integer min = Main.parseInt(minStr);
				if (min == null) {
					log.error("Bad number format (--document-min-size): [{}]", minStr);
					return;
				}
				min = Tools.ensureBetween(Main.MIN_DOC_SIZE, min, Main.MAX_DOC_SIZE);

				String maxStr = CLIParam.document_max_size.getString(String.valueOf(Main.MAX_DOC_SIZE)).toLowerCase();
				Integer max = Main.parseInt(maxStr);
				if (max == null) {
					log.error("Bad number format (--document-max-size): [{}]", maxStr);
					return;
				}
				max = Tools.ensureBetween(Main.MIN_DOC_SIZE, max, Main.MAX_DOC_SIZE);

				if (min > max) {
					// Flip them around
					int l = min;
					min = max;
					max = l;
				}
				minDocSize = min;
				maxDocSize = max;
			}

			final BinaryMemoryBuffer BASE_BUFFER = new BinaryMemoryBuffer(Main.BUFFER_CHUNK_SIZE);
			try {
				final int chunkCount = (maxDocSize / BASE_BUFFER.getChunkSize());
				byte[] buf = new byte[BASE_BUFFER.getChunkSize()];
				for (int i = 0; i < chunkCount; i++) {
					Main.RANDOM.nextBytes(buf);
					try {
						BASE_BUFFER.write(buf);
					} catch (IOException e) {
						// Can't continue...
						log.warn("Unexpected exception writing to memory", e);
						return;
					}
				}
				final int chunkRemainder = (maxDocSize % BASE_BUFFER.getChunkSize());
				Main.RANDOM.nextBytes(buf);
				try {
					BASE_BUFFER.write(buf, 0, chunkRemainder);
				} catch (IOException e) {
					// Can't continue...
					log.warn("Unexpected exception writing to memory", e);
					return;
				}
			} finally {
				IOUtils.closeQuietly(BASE_BUFFER);
			}

			log.info("Random data buffer of {} bytes ready", BASE_BUFFER.getCurrentSize());

			final String user = CLIParam.user.getString();
			final String password = CLIParam.password.getString();
			final String docbase = CLIParam.docbase.getString();

			final DfcSessionPool pool = new DfcSessionPool(docbase, user, password);

			try {
				final IDfSession mainSession;
				try {
					mainSession = pool.acquireSession();
				} catch (Exception e) {
					String msg = String.format("Failed to open a session to docbase [%s] as user [%s]", docbase, user);
					if (debug) {
						log.error(msg, e);
					} else {
						log.error("{}: {}", msg, e.getMessage());
					}
					return;
				}

				final String target = CLIParam.target.getString();
				final int treeDepth = CLIParam.tree_depth.getInteger(1);
				final int folderCount = CLIParam.folder_count.getInteger(1);
				final int documentCount = CLIParam.document_count.getInteger(1);

				try {
					final IDfFolder root = NodeGenerator.ensureFolder(mainSession, target);
					final NodeGenerator generator = new NodeGenerator(mainSession, objectTypes, new CSVRM());

					final int threads = CLIParam.threads.getInteger(Main.DEFAULT_THREADS);
					final ExecutorService executor = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS,
						new ArrayBlockingQueue<Runnable>(threads, true));

					final BlockingQueue<IDfId> queue = new ArrayBlockingQueue<IDfId>(10000, true);
					final List<Future<Integer>> futures = new ArrayList<Future<Integer>>(threads);
					for (int i = 0; i < threads; i++) {
						futures.add(executor.submit(new Callable<Integer>() {
							@Override
							public Integer call() {
								final IDfSession session;
								try {
									session = pool.acquireSession();
								} catch (Exception e) {
									String msg = "Failed to obtain a client session for processing";
									if (debug) {
										log.error(msg, e);
									} else {
										log.error("{}: {}", msg, e.getMessage());
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
												log.error(msg, e);
											} else {
												log.error(msg);
											}
											return null;
										}

										if ((id == null) || id.isNull()) {
											log.info("Thread completed");
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
												size += Main.RANDOM.nextInt(maxDocSize - minDocSize);
											}
											total += generator.generateDocuments(folder, documentCount, nameFormat,
												BASE_BUFFER.getInputStream(), size);
										} catch (DfException e) {
											log.error(String.format(
												"Failed to generate the documents at ID [%s] with path [%s]",
												id.getId(), path), e);
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
						size += Main.RANDOM.nextInt(maxDocSize - minDocSize);
					}
					int total = generator.generateDocuments(root, documentCount, nameFormat,
						BASE_BUFFER.getInputStream(), size);
					try {
						total += generator.generateFolders(queue, root, folderCount, treeDepth, nameFormat);
					} catch (InterruptedException e) {
						String msg = "Main thread interrupted while generating the folder tree";
						if (debug) {
							log.error(msg, e);
						} else {
							log.error(msg);
						}
					} finally {
						// The last items should be null IDs to ensure that the threads end
						// cleanly when they reach them
						for (int i = 0; i < threads; i++) {
							try {
								queue.put(DfId.DF_NULLID);
							} catch (InterruptedException e) {
								String msg = "Main thread interrupted while submitting the end markers";
								if (debug) {
									log.error(msg, e);
								} else {
									log.error(msg);
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
								log.warn(msg, e);
							} else {
								log.warn(msg);
							}
							break;
						} catch (ExecutionException e) {
							String msg = "Exception raised from a Document generator";
							if (debug) {
								log.error(msg, e);
							} else {
								log.error("{}: {}", msg, e.getMessage());
							}
						}
					}

					if (finished != futures.size()) {
						log.error("Did not finish processing: processed only {} out of {} generators", finished,
							futures.size());
					}

					log.info("Generated {} objects", total);
				} finally {
					pool.releaseSession(mainSession);
				}
			} finally {
				pool.close();
			}
		} catch (DfException e) {
			log.error("Documentum exception caught", e);
		}
	}
}