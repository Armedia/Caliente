package com.armedia.calienteng;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.dfc.util.DctmQuery;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class EventRegistration implements Comparable<EventRegistration> {

	private static final Logger LOG = LoggerFactory.getLogger(EventRegistration.class);

	public static Comparator<EventRegistration> BASIC_COMPARATOR = new Comparator<EventRegistration>() {
		@Override
		public int compare(EventRegistration a, EventRegistration b) {
			if (a == b) { return 0; }
			if (a == null) { return -1; }
			if (b == null) { return -1; }
			int r = Tools.compare(a.userName, b.userName);
			if (r != 0) { return r; }
			r = Tools.compare(a.event, b.event);
			if (r != 0) { return r; }
			return 0;
		}
	};

	private final IDfId id;
	private final IDfId registeredId;
	private final String userName;
	private final String event;
	private final String message;
	private final int priority;
	private final boolean sendMail;

	/**
	 * @param id
	 * @param registeredId
	 * @param event
	 * @param message
	 * @param priority
	 * @param sendMail
	 */
	public EventRegistration(IDfId id, IDfId registeredId, String userName, String event, String message, int priority,
		boolean sendMail) {
		this.id = id;
		this.registeredId = registeredId;
		this.userName = userName;
		this.event = event;
		this.message = message;
		this.priority = priority;
		this.sendMail = sendMail;
	}

	public IDfId getId() {
		return this.id;
	}

	public IDfId getRegisteredId() {
		return this.registeredId;
	}

	public String getUserName() {
		return this.userName;
	}

	public String getEvent() {
		return this.event;
	}

	public String getMessage() {
		return this.message;
	}

	public int getPriority() {
		return this.priority;
	}

	public boolean isSendMail() {
		return this.sendMail;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.id.getId(), this.registeredId.getId(), this.userName, this.event,
			this.message, this.priority, this.sendMail);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		EventRegistration other = EventRegistration.class.cast(obj);
		if (!Tools.equals(this.id, other.id)) { return false; }
		if (!Tools.equals(this.registeredId, other.registeredId)) { return false; }
		if (!Tools.equals(this.userName, other.userName)) { return false; }
		if (!Tools.equals(this.event, other.event)) { return false; }
		if (!Tools.equals(this.message, other.message)) { return false; }
		if (this.priority != other.priority) { return false; }
		if (this.sendMail != other.sendMail) { return false; }
		return true;
	}

	@Override
	public int compareTo(EventRegistration o) {
		return EventRegistration.BASIC_COMPARATOR.compare(this, o);
	}

	public static boolean registerEvent(IDfPersistentObject object, String message, String event, int priority,
		boolean sendMail) throws DfException {
		if (object == null) {
			throw new IllegalArgumentException("Must provide an object for which to register the event");
		}
		final IDfSession session = object.getSession();
		final IDfLocalTransaction tx;
		if (session.isTransactionActive()) {
			tx = session.beginTransEx();
		} else {
			tx = null;
		}
		try {
			object.registerEvent(message, event, priority, sendMail);
			if (tx != null) {
				session.commitTransEx(tx);
			}
			return true;
		} catch (DfException e) {
			if (tx != null) {
				try {
					session.abortTransEx(tx);
				} catch (DfException e2) {
					if (EventRegistration.LOG.isDebugEnabled()) {
						EventRegistration.LOG.warn(
							"Exception caught aborting the transaction for event registration: [{}, {}, {}, {}, {}]",
							object.getObjectId(), message, event, priority, sendMail, e2);
					}
				}
			}
			if (Tools.equals("DM_EVENT_E_EVENT_ALREADY_REGISTERED", e.getMessageId())) { return false; }
			throw e;
		}
	}

	public static boolean unregisterEvent(IDfSession session, IDfId id, String event) throws DfException {
		return EventRegistration.unregisterEvent(session, id, event, null);
	}

	public static boolean unregisterEvent(IDfSession session, IDfId id, String event, String user) throws DfException {
		if (id == null) {
			throw new IllegalArgumentException("Must provide an object ID for which to unregister the event");
		}
		final IDfLocalTransaction tx;
		if (session.isTransactionActive()) {
			tx = session.beginTransEx();
		} else {
			tx = null;
		}
		try {
			final IDfPersistentObject obj = session.getObject(id);
			if (user != null) {
				obj.unRegisterEventEx(event, user);
			} else {
				obj.unRegisterEvent(event);
			}
			if (tx != null) {
				session.commitTransEx(tx);
			}
			return true;
		} catch (DfException e) {
			if (tx != null) {
				try {
					session.abortTransEx(tx);
				} catch (DfException e2) {
					if (EventRegistration.LOG.isDebugEnabled()) {
						EventRegistration.LOG.warn(
							"Exception caught aborting the transaction for event unregistration: [{}, {}]", id, event,
							e2);
					}
				}
			}
			if (Tools.equals("DM_EVENT_E_NOT_REGISTERED", e.getMessageId())) { return false; }
			throw e;
		}
	}

	/**
	 * <p>
	 * Returns the number of events for which the given user has registered on the given object. If
	 * the user is {@code null}, the current session's user is used instead. The current session is
	 * obtained via {@link IDfTypedObject#getSession()}.
	 * </p>
	 *
	 * @param object
	 * @return the number of events for which the given user has registered on the given object
	 * @throws DfException
	 */
	public static int getRegisteredCount(IDfPersistentObject object, String user) throws DfException {
		if (object == null) {
			throw new IllegalArgumentException("Must provide an object for which to check event registration");
		}
		final IDfSession session = object.getSession();
		if (user == null) {
			user = session.getLoginUserName();
		}
		String dql = "select count(*) as counter from dmi_registry where registered_id = %s and user_name = %s";
		dql = String.format(dql, DfUtils.quoteString(object.getObjectId().getId()), DfUtils.quoteString(user));
		try (DctmQuery query = new DctmQuery(session, dql, DctmQuery.Type.DF_EXECREAD_QUERY)) {
			if (!query.hasNext()) {
				// This should be impossible, but still cover for it...
				throw new DfException("DQL Query somehow returned no results, even though it's a count(*) query");
			}
			return query.next().getInt("counter");
		}
	}

	/**
	 * <p>
	 * Returns the number of events for which the current session's user has registered on the given
	 * object. The current session is obtained via {@link IDfTypedObject#getSession()}.
	 * </p>
	 *
	 * @param object
	 * @return the number of events for which the current session's user has registered on the given
	 *         object
	 * @throws DfException
	 */
	public static int getRegisteredCount(IDfPersistentObject object) throws DfException {
		return EventRegistration.getRegisteredCount(object, null);
	}

	/**
	 * <p>
	 * Returns {@code true} if the given user has registered to receive notification of the given
	 * event, {@code false} otherwise. If the given user is {@code null}, the current session's (as
	 * returned by {@link IDfTypedObject#getSession()}) user (from
	 * {@link IDfSession#getLoginUserName()}) is used instead.
	 * </p>
	 *
	 * @param object
	 * @param event
	 * @param user
	 * @return {@code true} if the given user has registered to receive notification of the given
	 *         event, {@code false} otherwise
	 * @throws DfException
	 */
	public static boolean isRegistered(IDfPersistentObject object, String event, String user) throws DfException {
		if (object == null) {
			throw new IllegalArgumentException("Must provide an object for which to check event registration");
		}
		if (event == null) { throw new IllegalArgumentException("Must provide an event to check for"); }
		final IDfSession session = object.getSession();
		if (user == null) {
			user = session.getLoginUserName();
		}
		String dql = "select r_object_id from dmi_registry where registered_id = %s and event = %s and user_name = %s";
		dql = String.format(dql, DfUtils.quoteString(object.getObjectId().getId()), DfUtils.quoteString(event),
			DfUtils.quoteString(user));
		try (DctmQuery query = new DctmQuery(session, dql, DctmQuery.Type.DF_EXECREAD_QUERY)) {
			return query.hasNext();
		}
	}

	/**
	 * <p>
	 * Returns {@code true} if the current session's user has registered to receive notification of
	 * the given event, {@code false} otherwise. The current session is identified via
	 * {@link IDfTypedObject#getSession()}.
	 * </p>
	 *
	 * @param object
	 * @param event
	 * @return {@code true} if the current session's user has registered to receive notification of
	 *         the given event, {@code false} otherwise
	 * @throws DfException
	 */
	public static boolean isRegistered(IDfPersistentObject object, String event) throws DfException {
		return EventRegistration.isRegistered(object, event, null);
	}

	/**
	 * <p>
	 * Returns all the registered events for the current session's user. The current session is
	 * obtained via {@link IDfTypedObject#getSession()}.
	 * </p>
	 *
	 * @param object
	 * @return returns all the registered events for the current session's user.
	 * @throws DfException
	 */
	public static Set<EventRegistration> getRegisteredForCurrentUser(IDfPersistentObject object) throws DfException {
		return EventRegistration.getRegisteredForUser(object, null);
	}

	private static EventRegistration loadRegistration(IDfTypedObject obj) throws DfException {
		IDfId id = obj.getId("r_object_id");
		IDfId registeredId = obj.getId("registered_id");
		String userName = obj.getString("user_name");
		String event = obj.getString("event");
		String message = obj.getString("message");
		int priority = obj.getInt("priority");
		boolean sendMail = obj.getBoolean("sendmail");
		return new EventRegistration(id, registeredId, userName, event, message, priority, sendMail);
	}

	/**
	 * <p>
	 * Returns all the registered events for the given user. If the user is {@code null}, then the
	 * current session's user is assumed.
	 * </p>
	 *
	 * @param object
	 * @param user
	 * @return all the registered events for the given user. If no user is given, then the current
	 *         session's user is assumed.
	 * @throws DfException
	 */
	public static Set<EventRegistration> getRegisteredForUser(IDfPersistentObject object, String user)
		throws DfException {
		if (object == null) {
			throw new IllegalArgumentException("Must provide an object whose event registrations to analyze");
		}
		final IDfSession session = object.getSession();
		if (user == null) {
			user = session.getLoginUserName();
		}
		String dql = "select * from dmi_registry where registered_id = %s and user_name = %s order by event";
		dql = String.format(dql, DfUtils.quoteString(object.getObjectId().getId()), DfUtils.quoteString(user));
		try (DctmQuery query = new DctmQuery(session, dql, DctmQuery.Type.DF_EXECREAD_QUERY)) {
			Set<EventRegistration> ret = new TreeSet<>();
			query.forEachRemaining((o) -> ret.add(EventRegistration.loadRegistration(o)));
			return ret;
		}
	}

	/**
	 * <p>
	 * Returns a map that contains all registered events for all users for the given object, indexed
	 * by username.
	 * </p>
	 * <p>
	 * The returned maps and sets are ordered (TreeMap and TreeSet).
	 * </p>
	 *
	 * @param object
	 * @return a map that contains all registered events for all users for the given object, indexed
	 *         by username.
	 * @throws DfException
	 */
	public static Map<String, Set<EventRegistration>> getAllRegistered(IDfPersistentObject object) throws DfException {
		if (object == null) {
			throw new IllegalArgumentException("Must provide an object whose event registrations to analyze");
		}
		final IDfSession session = object.getSession();
		String dql = "select event from dmi_registry where registered_id = %s order by user_name, event";
		dql = String.format(dql, DfUtils.quoteString(object.getObjectId().getId()));
		try (DctmQuery query = new DctmQuery(session, dql, DctmQuery.Type.DF_EXECREAD_QUERY)) {
			Map<String, Set<EventRegistration>> ret = new TreeMap<>();
			String user = null;
			Set<EventRegistration> s = null;
			while (query.hasNext()) {
				IDfTypedObject o = query.next();
				String userName = o.getString("user_name");
				if (!Tools.equals(user, userName)) {
					if (s != null) {
						ret.put(user, s);
					}

					user = userName;
					s = new TreeSet<>();
				}
				s.add(EventRegistration.loadRegistration(o));
			}
			if ((user != null) && (s != null)) {
				ret.put(user, s);
			}
			return ret;
		}
	}
}