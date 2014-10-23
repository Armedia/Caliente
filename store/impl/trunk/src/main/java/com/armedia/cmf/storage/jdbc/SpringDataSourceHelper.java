/**
 *
 */

package com.armedia.cmf.storage.jdbc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>
 * {@code
 * <bean id="applicationContextLocator" class="com.armedia.cmf.storage.jdbc.SpringDataSourceHelper">
 * </bean>
 * }
 * </p>
 *
 * @author diego.rivera@armedia.com
 *
 */
public final class SpringDataSourceHelper implements ApplicationContextAware, InitializingBean {

	private static final Object CONTEXTS_LOCK = new Object();
	private static final Map<String, ApplicationContext> CONTEXTS = new ConcurrentHashMap<String, ApplicationContext>();

	static DataSource locateDataSource(String name) {
		for (ApplicationContext ctx : SpringDataSourceHelper.CONTEXTS.values()) {
			DataSource ds = ctx.getBean(name, DataSource.class);
			if (ds != null) { return ds; }
		}
		return null;
	}

	private ApplicationContext ctx = null;

	@Override
	public void setApplicationContext(ApplicationContext ctx) {
		this.ctx = ctx;
	}

	public ApplicationContext getApplicationContext() {
		return this.ctx;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.ctx == null) { throw new IllegalStateException("ApplicationContext was not set"); }

		String id = this.ctx.getId();
		if (id == null) {
			// If we lack an ID, we generate one just for safety's sake
			id = this.ctx.toString();
		}
		synchronized (SpringDataSourceHelper.CONTEXTS_LOCK) {
			ApplicationContext ctx = SpringDataSourceHelper.CONTEXTS.get(id);
			if (this.ctx == ctx) { return; }
			if (ctx == null) {
				SpringDataSourceHelper.CONTEXTS.put(id, ctx);
			}
		}
	}
}