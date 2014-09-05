package com.delta.cmsmf.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.mail.MessagingException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.cmsobjects.DctmObjectType;
import com.delta.cmsmf.utils.CMSMFUtils;

/**
 * The Class AppCounter keeps running tab of number of various types of objects processed during
 * export or
 * import step. This class implements singleton design pattern to maintain single set of properties
 * through
 * out the execution.
 * <p>
 * This class contains a counter for each type of object that cmsmf application is able to handle.
 *
 * @author Shridev Makim 6/15/2010
 */
public class AppCounter {

	/** The logger object used for logging. */
	private final Logger logger = Logger.getLogger(getClass());

	/** The singleton instance. */
	private static AppCounter INSTANCE;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Map<DctmObjectType, AtomicInteger> counters;
	private final List<DctmObjectType> reportingOrder;

	/**
	 * Instantiates a new App counter. Private constructor to prevent
	 * new instances being created.
	 */
	private AppCounter() {
		Map<DctmObjectType, AtomicInteger> counters = new EnumMap<DctmObjectType, AtomicInteger>(DctmObjectType.class);
		for (DctmObjectType v : DctmObjectType.values()) {
			counters.put(v, new AtomicInteger(0));
		}
		this.counters = Collections.unmodifiableMap(counters);
		List<DctmObjectType> reportingOrder = new ArrayList<DctmObjectType>(this.counters.size());
		reportingOrder.add(DctmObjectType.DCTM_DOCUMENT);
		reportingOrder.add(DctmObjectType.DCTM_FOLDER);
		reportingOrder.add(DctmObjectType.DCTM_USER);
		reportingOrder.add(DctmObjectType.DCTM_GROUP);
		reportingOrder.add(DctmObjectType.DCTM_ACL);
		reportingOrder.add(DctmObjectType.DCTM_TYPE);
		reportingOrder.add(DctmObjectType.DCTM_FORMAT);
		this.reportingOrder = Collections.unmodifiableList(reportingOrder);
	}

	/**
	 * Gets the singleton instance of App counter class.
	 *
	 * @return the object counter
	 */
	public synchronized static AppCounter getObjectCounter() {
		if (AppCounter.INSTANCE == null) {
			AppCounter.INSTANCE = new AppCounter();
		}
		return AppCounter.INSTANCE;
	}

	private AtomicInteger getAtomic(DctmObjectType type) {
		if (type == null) { throw new IllegalArgumentException("Must provide a type to retrieve the counter for"); }
		final Lock lock = this.lock.readLock();
		lock.lock();
		try {
			return this.counters.get(type);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Prints the counters to a log file.
	 */
	public void printCounters() {
		final Lock lock = this.lock.readLock();
		lock.lock();
		try {
			for (DctmObjectType t : this.reportingOrder) {
				this.logger.info(String.format("Total nbr of %ss processed: %d", t.getName(), getAtomic(t).get()));
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Email counters.
	 *
	 * @param exportDQLQuery
	 */
	public void emailCounters(String exportImportStep, String exportDQLQuery) {
		final Lock lock = this.lock.readLock();
		lock.lock();
		try {
			StringBuffer emailMsg = new StringBuffer("Following is a report from " + exportImportStep + " step. \n");

			// If this is a export step, email the dql query used in the export.
			if (exportImportStep.equals("Export")) {
				emailMsg.append("\n The export query used was: " + exportDQLQuery + "\n");
			}
			for (DctmObjectType t : this.reportingOrder) {
				emailMsg.append(String.format("%n\t Total nbr of %ss processed: %d", t.getName(), getAtomic(t).get()));
			}

			try {
				CMSMFUtils.postCmsmfMail("CMSMF " + exportImportStep + " Report", emailMsg.toString());
			} catch (MessagingException e) {
				this.logger.error("Error sending CMSMF " + exportImportStep + " report", e);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Increments a counter for a gievn object type.
	 *
	 * @param dctmObjectType
	 *            the dctm object type
	 */
	public int incrementCounter(DctmObjectType dctmObjectType) {
		final Lock lock = this.lock.readLock();
		lock.lock();
		try {
			AtomicInteger counter = getAtomic(dctmObjectType);
			int count = counter.incrementAndGet();
			if (dctmObjectType == DctmObjectType.DCTM_DOCUMENT) {
				if ((count % 100) == 0) {
					if (this.logger.isEnabledFor(Level.INFO)) {
						this.logger.info("INFO:: Processed " + count + " documents so far.");
					}
				}
			}
			return count;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Resets all of the counters to 0.
	 */
	public void resetCounters() {
		final Lock lock = this.lock.writeLock();
		lock.lock();
		try {
			for (DctmObjectType v : DctmObjectType.values()) {
				getAtomic(v).set(0);
			}
		} finally {
			lock.unlock();
		}
	}
}