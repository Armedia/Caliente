
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
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.armedia.calienteng.EventRegistration;
import com.armedia.commons.utilities.LazyFormatter;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQueueItem;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;

public class TestLoop {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static DfcSessionPool POOL = null;

	// @BeforeAll
	public static void setUpBeforeClass() throws Exception {
		if (TestLoop.POOL == null) {
			TestLoop.POOL = new DfcSessionPool("documentum", "dmadmin2", "ArM3D!A");
			// TestLoop.POOL = new DfcSessionPool("dctmvm01", "dctmadmin", "123");
			// TestLoop.POOL = new DfcSessionPool("armrdreponew", "dmadmin", "ArM3D!A");
		}
	}

	// @AfterAll
	public static void tearDownAfterClass() throws Exception {
		if (TestLoop.POOL != null) {
			try {
				TestLoop.POOL.close();
			} finally {
				TestLoop.POOL = null;
			}
		}
	}

	// TODO: What about versions? How does that work?
	/*
	// Base event response algorithm
	switch (action) {
		case CREATE:
			addWatcher(obj);
			break;
	
		case UPDATE:
			boolean updateRecord = true;
			Record existing = getExistingRecord(obj);
			Record newRecord = new Record(obj);
			if (existing == null) {
				addWatcher(obj);
			} else {
				if (!existing.getFullPath().equals(newRecord.getFullPath())) {
					if (getExistingRecordByPath(newRecord.getFullPath()) != null) {
						// report the collision
						removeWatcher(obj);
						updateRecord = false;
					}
				}
				if (updateRecord) {
					updateWatchRecord(obj);
				}
			}
			break;
	
		case DELETE:
			removeWatcher(obj);
			break;
	}
	 */

	private static enum EventMode {
		//
		TYPE, OBJECT,
		//
		;
	}

	private static enum Event {
		// On "watched" objects

		// These two result in updates
		dm_checkin(EventMode.OBJECT),
		dm_save(EventMode.OBJECT),

		// This one results in a delete
		dm_destroy(EventMode.OBJECT),

		// This can either be a delete or an update
		dm_unlink(EventMode.OBJECT),

		// This can result in an insert or an update
		dm_link(EventMode.OBJECT, EventMode.TYPE),

		// These result in an update
		dm_mark(EventMode.OBJECT),
		dm_branch(EventMode.OBJECT),

		// This can either be a delete or an update
		dm_prune(EventMode.OBJECT),

		// These may need to be removed, b/c they give us nothing
		/*
		dm_checkout(EventMode.OBJECT),
		dm_fetch(EventMode.OBJECT),
		dm_lock(EventMode.OBJECT),
		dm_unlock(EventMode.OBJECT),
		dm_status(EventMode.OBJECT),
		dm_archive(EventMode.OBJECT),
		dm_archive_done(EventMode.OBJECT),
		dm_restore(EventMode.OBJECT),
		dm_restore_done(EventMode.OBJECT),
		 */
		//
		;

		private final Set<EventMode> modes;

		private Event(EventMode... modes) {
			if ((modes == null) || (modes.length == 0)) {
				throw new IllegalArgumentException(String.format("Must specify the object modes for Event.%s", name()));
			}
			Set<EventMode> s = EnumSet.noneOf(EventMode.class);
			for (EventMode m : modes) {
				if (m == null) {
					continue;
				}
				s.add(m);
			}
			if (s.isEmpty()) {
				throw new IllegalArgumentException(
					String.format("Must specify at least one non-null mode Event.%s", name()));
			}
			this.modes = Tools.freezeSet(s);
		}

	}

	private static final String CALIENTE_STR = "$caliente$";
	private static final TimeZone TZ = TimeZone.getTimeZone("UTC");
	private static final String DATE_PATTERN = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.getPattern();

	private static final String INSERT_STATUS_DQL = //
		"    insert into dm_dbo.caliente_status " + //
			"select ${chronicleId}, ${type}, ${path}, ${name}, ${mtime}, ${depth} " + //
			"  from sys.dual " + //
			" where not exists ( " + //
			"          select r_object_id " + //
			"            from dm_dbo.caliente_status " + //
			"           where i_chronicle_id = ${chronicleId} " + //
			"              or ( object_path = ${path} and object_name = ${name} ) " + //
			" )";

	private static final String UPDATE_STATUS_SQL = //
		"    update dm_dbo.caliente_status " + //
			"   set object_path = ${path}, " + //
			"   set object_name = ${name}, " + //
			"   set modified = ${mtime}, " + //
			"   set path_depth = ${depth} " + //
			" where i_chronicle_id = ${chronicleId}";

	@SuppressWarnings("unused")
	private static final String DELETE_STATUS_SQL = //
		"    delete from dm_dbo.caliente_status " + //
			"   where i_chronicle_id = ${chronicleId}";

	private static final String INSERT_VERSION_DQL = //
		"    insert into dm_dbo.caliente_version " + //
			"select ${objectId}, ${chronicleId}, ${versionLabel} " + //
			"  from sys.dual " + //
			" where not exists ( " + //
			"          select r_object_id " + //
			"            from dm_dbo.caliente_version " + //
			"           where r_object_id = ${objectId} " + //
			" )";

	private static final String GET_STATUS_DQL = //
		"    select i_chronicle_id " + //
			"  from dm_dbo.caliente_version " + //
			" where r_object_id = ${objectId}";

	@SuppressWarnings("unused")
	private static final String DELETE_VERSION_SQL = //
		"    delete from dm_dbo.caliente_version " + //
			"   where r_object_id = ${objectId}";

	private static class Payload {
		private final Event event;
		private final IDfTime date;
		private final IDfId objectId;
		private final IDfSysObject object;

		private Payload(IDfQueueItem item) throws DfException {
			this(item, null);
		}

		private Payload(IDfQueueItem item, IDfSysObject object) throws DfException {
			this.event = Event.valueOf(item.getEvent());
			this.date = item.getDateSent();
			this.objectId = item.getItemId();
			this.object = object;
		}
	}

	@SuppressWarnings("unused")
	private void deleteWatch(IDfSession session, IDfId id) throws DfException {
		final IDfLocalTransaction tx = session.beginTransEx();
		boolean ok = false;
		try {
			boolean unregister = false;
			Map<String, String> parameters = new HashMap<>();
			parameters.put("objectId", DfcUtils.quoteString(id.getId()));
			try (DfcQuery query = new DfcQuery(session,
				StringSubstitutor.replace(TestLoop.GET_STATUS_DQL, parameters))) {
				if (!query.hasNext()) {
					// No chronicle...can't delete the watch!
				} else {
					int deleted = query.next().getInt("rows_deleted");
					if (deleted == 1) {
						unregister = true;
					}
				}
			}
			if (unregister) {
				for (Event e : Event.values()) {
					if (e.modes.contains(EventMode.OBJECT)) {
						EventRegistration.unregisterEvent(session, id, e.name());
					}
				}
			}
			session.commitTransEx(tx);
			ok = true;
		} catch (DfIdNotFoundException e) {
			if (!ok) {
				session.abortTransEx(tx);
			}
		}
	}

	private void addTypeWatch(IDfType obj) throws DfException {
		final IDfSession session = obj.getSession();
		final IDfLocalTransaction tx = session.beginTransEx();
		boolean ok = false;
		try {
			for (Event e : Event.values()) {
				if (e.modes.contains(EventMode.TYPE)) {
					EventRegistration.registerEvent(obj, TestLoop.CALIENTE_STR, e.name(), 1, false);
				}
			}
			session.commitTransEx(tx);
			ok = true;
		} finally {
			if (!ok) {
				try {
					session.abortTransEx(tx);
				} catch (DfException e) {
					this.log.warn("Failed to abort the transaction adding the TYPE watch for {}", obj.getDescription(),
						e);
				}
			}
		}
	}

	private void addObjectWatch(IDfSysObject obj) throws DfException {
		final IDfSession session = obj.getSession();
		final IDfLocalTransaction tx = session.beginTransEx();
		boolean ok = false;
		try {
			boolean register = false;
			// It's a single object, so we have to insert a record into the watcher table(s)
			final IDfSysObject so = IDfSysObject.class.cast(obj);
			final IDfType type = so.getType();
			final int archetype = (type.isInstanceOf("dm_folder") ? 1 : 2);

			IDfFolder parent = IDfFolder.class.cast(session.getObject(so.getFolderId(0)));
			final String objectPath = parent.getFolderPath(0);

			// Do the insert
			Date modifiedDate = so.getModifyDate().getDate();
			String modifiedStr = DateFormatUtils.format(modifiedDate, TestLoop.DATE_PATTERN, TestLoop.TZ);

			final int depth = StringUtils.countMatches(objectPath, "/");
			Map<String, String> parameters = new HashMap<>();
			parameters.put("chronicleId", DfcUtils.quoteString(obj.getChronicleId().getId()));
			parameters.put("objectId", DfcUtils.quoteString(obj.getObjectId().getId()));
			parameters.put("type", String.valueOf(archetype));
			parameters.put("path", DfcUtils.quoteString(objectPath));
			parameters.put("name", DfcUtils.quoteString(so.getObjectName()));
			parameters.put("mtime", DfcUtils.quoteString(modifiedStr));
			parameters.put("depth", String.valueOf(depth));
			try (DfcQuery query = new DfcQuery(session,
				StringSubstitutor.replace(TestLoop.INSERT_STATUS_DQL, parameters))) {
				if (query.hasNext()) {
					int inserted = query.next().getInt("rows_inserted");
					if (inserted == 1) {
						register = true;
					} else {
						// Duplicate record
						this.log.warn("Duplicate status record detected - either by object ID or by path: {}",
							parameters);
					}
				}
			}

			try (DfcQuery query = new DfcQuery(session,
				StringSubstitutor.replace(TestLoop.INSERT_VERSION_DQL, parameters))) {
				if (query.hasNext()) {
					int inserted = query.next().getInt("rows_inserted");
					if (inserted == 1) {
						register = true;
					} else {
						// Duplicate record
						this.log.warn("Duplicate version record detected - either by object ID or by path: {}",
							parameters);
					}
				}
			}

			if (register) {
				for (Event e : Event.values()) {
					if (e.modes.contains(EventMode.OBJECT)) {
						EventRegistration.registerEvent(so, TestLoop.CALIENTE_STR, e.name(), 1, false);
					}
				}
			}
			session.commitTransEx(tx);
			ok = true;
		} finally {
			if (!ok) {
				try {
					session.abortTransEx(tx);
				} catch (DfException e) {
					this.log.warn("Failed to abort the transaction adding the OBJECT watch for {} [{}]",
						obj.getClass().getSimpleName(), obj.getObjectId(), e);
				}
			}
		}
	}

	private void registerWatchers(IDfFolder cabinet) throws DfException {
		final IDfSession session = cabinet.getSession();
		final String dql = String.format(
			"select r_object_id from dm_sysobject (ALL) where folder(ID(%s), DESCEND) and ( type(dm_document) or type(dm_folder) )",
			DfcUtils.quoteString(cabinet.getObjectId().getId()));
		boolean ok = false;
		final IDfLocalTransaction tx = session.beginTransEx();
		try {
			try (DfcQuery query = new DfcQuery(session, dql)) {
				String[] types = {
					"dm_folder", "dm_document"
				};
				// Register type-wide events
				for (String t : types) {
					IDfType type = session.getType(t);
					addTypeWatch(type);
				}

				while (query.hasNext()) {
					IDfTypedObject c = query.next();
					final IDfId id = c.getId("r_object_id");
					final IDfSysObject so = IDfSysObject.class.cast(session.getObject(id));
					addObjectWatch(so);
				}
				session.commitTransEx(tx);
				ok = true;
			}
		} finally {
			if (!ok) {
				session.abortTransEx(tx);
			}
		}
	}

	// @Test
	public void test() throws Throwable {
		final TimeZone utc = TimeZone.getTimeZone("UTC");
		final String datePattern = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.getPattern();
		final IDfSession session = TestLoop.POOL.acquireSession();

		final IDfFolder ligeroTests = IDfFolder.class.cast(
			session.getObjectByQualification("dm_folder where object_name = 'CMSMFTests' and folder('/Dctm Imports')"));
		session.beginTrans();
		try {
			registerWatchers(ligeroTests);
		} finally {
			session.commitTrans();
		}

		try {

			final List<Payload> payloads = new ArrayList<>();

			long current = 0;
			forever: for (;;) {
				payloads.clear();

				this.log.info("Checking for events...");
				try (DfcQuery query = new DfcQuery(session,
					String.format("select r_object_id from dmi_queue_item where delete_flag != 1 and message = %s",
						DfcUtils.quoteString(TestLoop.CALIENTE_STR)))) {
					item: while (query.hasNext()) {
						IDfId id = query.next().getId("r_object_id");
						final IDfQueueItem i;
						try {
							i = IDfQueueItem.class.cast(session.getObject(id));
						} catch (DfIdNotFoundException e) {
							// No such item...
							e.hashCode();
							continue forever;
						}

						final String eventName = i.getEvent();
						final Event event;
						try {
							event = Event.valueOf(eventName);
						} catch (IllegalArgumentException e) {
							// We're not interested in this event
							this.log.warn("Ignoring [{}] from [{}] for {}[{}] ({})", eventName, id.getId(),
								i.getItemType(), i.getItemId(), i.getItemName());
							continue item;
						}

						// Ok so this is an event type we're interested in...
						this.log.info("Stowing [{}] from [{}] for {}[[}] ({}){}", event, id.getId(), i.getItemType(),
							i.getItemId(), i.getItemName(), Tools.NL);
						if (!i.isDeleteFlag()) {
							session.dequeue(id);
						}
						IDfPersistentObject o = null;
						try {
							o = session.getObject(i.getItemId());
						} catch (DfIdNotFoundException e) {
							// Not found...must be a delete event
						} catch (DfException e) {
							// Not found!?!
							e.hashCode();
						}

						switch (event) {
							case dm_destroy:
								Assertions.assertNull(o);
								payloads.add(new Payload(i));
								break;
							case dm_checkin:
							case dm_save:
							case dm_unlink:
							case dm_link:
								Assertions.assertNotNull(o);
								if (IDfFolder.class.isInstance(o) || IDfDocument.class.isInstance(o)) {
									IDfSysObject so = IDfSysObject.class.cast(o);
									// Identify the object
									payloads.add(new Payload(i, so));
								}
								break;
							// case dm_mark:
							// case dm_branch:
							// case dm_prune:
							default:
								break;
						}
					}

					if (payloads.isEmpty()) {
						// No items, so sleep for a given period then poll again
						this.log.info("No events found, sleeping for 5s");
						Thread.sleep(5 * 1000);
						continue forever;
					}

					payloads.size();
					for (Payload payload : payloads) {
						final IDfSysObject so = payload.object;
						if (so == null) {
							// Object must be deleted
							new DfcQuery(session,
								String.format("delete from dm_dbo.caliente_status where r_object_id = %s",
									DfcUtils.quoteString(payload.objectId.getId())));
						} else {
							// Object needs to be created or updated
							// Identify the object
							final String path;
							if (so.getFolderIdCount() == 0) {
								path = "/";
							} else {
								IDfId parentFolder = so.getFolderId(0);
								IDfFolder parent = null;
								try {
									parent = IDfFolder.class.cast(session.getObject(parentFolder));
								} catch (DfIdNotFoundException e) {
									// Not found!?!
									e.hashCode();
									continue;
								}
								path = parent.getFolderPath(0);
							}

							final String dateStr = DateFormatUtils.format(payload.date.getDate(), datePattern, utc);
							this.log.info("Event #{}: [{}] from [{}] at [{}]{}", LazyFormatter.of("%08d", ++current),
								payload.event, path, dateStr, Tools.NL);

							if (dateStr.hashCode() != utc.hashCode()) {
								continue;
							}

							if (payload.event == Event.dm_link) {
								addObjectWatch(so);
							} else {
								// TODO: How to tell if the new path/name won't collide with an
								// existing one? What to do then?
								// TODO: What if the new name/path means it shouldn't be watched
								// anymore?
								Map<String, String> parameters = new HashMap<>();
								parameters.put("name", DfcUtils.quoteString(so.getObjectName()));
								parameters.put("path", DfcUtils.quoteString(path));
								parameters.put("mtime", DfcUtils.quoteString(dateStr));
								parameters.put("depth", String.valueOf(StringUtils.countMatches(path, "/")));
								parameters.put("chronicleId", DfcUtils.quoteString(so.getChronicleId().getId()));
								try (DfcQuery updateResult = new DfcQuery(session,
									StringSubstitutor.replace(TestLoop.UPDATE_STATUS_SQL, parameters))) {
									updateResult.forEachRemaining((o) -> {
										String dump = o.dump();
										dump.hashCode();
									});
								}
							}
						}
					}
				}
			}

		} finally {
			TestLoop.POOL.releaseSession(session);
		}
	}

	// @Test
	public void generateData() throws Exception {

		final IDfSession session = TestLoop.POOL.acquireSession();
		try {
			final IDfLocalTransaction tx;
			if (session.isTransactionActive()) {
				tx = session.beginTransEx();
			} else {
				session.beginTrans();
				tx = null;
			}
			boolean ok = false;
			try {
				IDfFolder parent = session.getFolderByPath("/CMSMFTests/Specials");

				IDfSysObject obj = IDfSysObject.class.cast(session.newObject("sysobject_child_test"));
				obj.setObjectName("SysObject Child Test.bin");
				obj.setContentType("binary");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				baos.write(obj.getObjectName().getBytes());
				obj.setContent(baos);
				for (int i = 0; i < 10; i++) {
					obj.setRepeatingString("property_one", i, UUID.randomUUID().toString());
				}
				obj.setInt("property_two", 10);
				obj.link(parent.getObjectId().getId());
				obj.save();

				obj = IDfSysObject.class.cast(session.newObject("sysobject_grandchild_test"));
				obj.setObjectName("SysObject Grandchild Test.bin");
				obj.setContentType("binary");
				baos = new ByteArrayOutputStream();
				baos.write(obj.getObjectName().getBytes());
				obj.setContent(baos);
				for (int i = 0; i < 10; i++) {
					obj.setRepeatingString("property_one", i, UUID.randomUUID().toString());
				}
				obj.setInt("property_two", 10);
				obj.setTime("property_three", new DfTime(new Date()));
				obj.link(parent.getObjectId().getId());
				obj.save();

				if (tx != null) {
					session.commitTransEx(tx);
				} else {
					session.commitTrans();
				}
				ok = true;
			} finally {
				if (!ok) {
					if (tx != null) {
						session.abortTransEx(tx);
					} else {
						session.abortTrans();
					}
				}
			}
		} finally {
			TestLoop.POOL.releaseSession(session);
		}
	}
}