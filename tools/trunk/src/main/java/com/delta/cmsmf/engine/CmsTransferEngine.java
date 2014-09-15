package com.delta.cmsmf.engine;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.delta.cmsmf.cms.CmsAttributeMapper;
import com.delta.cmsmf.cms.CmsTransferContext;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

public abstract class CmsTransferEngine {

	protected final class Context implements CmsTransferContext {

		private final String rootId;
		private final IDfSession session;
		private final CmsAttributeMapper mapper;
		private final Map<String, IDfValue> values = new HashMap<String, IDfValue>();

		public Context(String rootId, IDfSession session, CmsAttributeMapper mapper) {
			this.rootId = rootId;
			this.session = session;
			this.mapper = mapper;
		}

		@Override
		public String getRootObjectId() {
			return this.rootId;
		}

		@Override
		public IDfSession getSession() {
			return this.session;
		}

		@Override
		public CmsAttributeMapper getAttributeMapper() {
			return this.mapper;
		}

		private void assertValidName(String name) {
			if (name == null) { throw new IllegalArgumentException("Must provide a value name"); }
		}

		@Override
		public IDfValue getValue(String name) {
			assertValidName(name);
			return this.values.get(name);
		}

		@Override
		public IDfValue setValue(String name, IDfValue value) {
			assertValidName(name);
			if (value == null) { return clearValue(name); }
			return this.values.put(name, value);
		}

		@Override
		public IDfValue clearValue(String name) {
			assertValidName(name);
			return this.values.remove(name);
		}

		@Override
		public boolean hasValue(String name) {
			assertValidName(name);
			return this.values.containsKey(name);
		}

	}

	protected final Logger log = Logger.getLogger(getClass());

	public static final int DEFAULT_BACKLOG_SIZE = 100;
	public static final int MAX_BACKLOG_SIZE = 1000;
	public static final int DEFAULT_THREAD_COUNT = 4;
	public static final int MAX_THREAD_COUNT = 32;

	private final int backlogSize;
	private final int threadCount;

	public CmsTransferEngine() {
		this(CmsTransferEngine.DEFAULT_THREAD_COUNT);
	}

	public CmsTransferEngine(int threadCount) {
		this(threadCount, CmsTransferEngine.DEFAULT_BACKLOG_SIZE);
	}

	public CmsTransferEngine(int threadCount, int backlogSize) {
		if (threadCount <= 0) {
			threadCount = 1;
		}
		if (threadCount > CmsTransferEngine.MAX_THREAD_COUNT) {
			threadCount = CmsTransferEngine.MAX_THREAD_COUNT;
		}
		if (backlogSize <= 0) {
			backlogSize = 10;
		}
		if (backlogSize > CmsTransferEngine.MAX_BACKLOG_SIZE) {
			backlogSize = CmsTransferEngine.MAX_BACKLOG_SIZE;
		}
		this.threadCount = threadCount;
		this.backlogSize = backlogSize;
	}

	public int getBacklogSize() {
		return this.backlogSize;
	}

	public int getThreadCount() {
		return this.threadCount;
	}
}