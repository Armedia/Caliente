package com.armedia.caliente.tools.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;

public class DataSourceDescriptorTest {

	private static class TestDataSourceDescriptor<DS extends DataSource> extends DataSourceDescriptor<DS> {

		private final Runnable closer;

		private TestDataSourceDescriptor(DS dataSource, boolean managedTransactions) {
			this(dataSource, managedTransactions, null);
		}

		private TestDataSourceDescriptor(DS dataSource, boolean managedTransactions, Runnable closer) {
			super(dataSource, managedTransactions);
			this.closer = closer;
		}

		@Override
		public void close() {
			if (this.closer != null) {
				this.closer.run();
			}
		}
	}

	@Test
	void testDataSourceDescriptor() throws Exception {
		DataSource ds = null;
		DataSourceDescriptor<?> desc = null;
		InvocationHandler handler = null;
		Class<?>[] interfaces = {
			DataSource.class
		};

		desc = new TestDataSourceDescriptor<>(ds, false);
		Assertions.assertNull(desc.getDataSource());
		Assertions.assertFalse(desc.isManagedTransactions());

		desc = new TestDataSourceDescriptor<>(ds, true);
		Assertions.assertNull(desc.getDataSource());
		Assertions.assertTrue(desc.isManagedTransactions());

		handler = (Object proxy, Method method, Object[] args) -> {
			return null;
		};

		ds = DataSource.class
			.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, handler));

		desc = new TestDataSourceDescriptor<>(ds, false);
		Assertions.assertSame(ds, desc.getDataSource());
		Assertions.assertFalse(desc.isManagedTransactions());

		desc = new TestDataSourceDescriptor<>(ds, true);
		Assertions.assertSame(ds, desc.getDataSource());
		Assertions.assertTrue(desc.isManagedTransactions());
	}

	@Test
	void testClose() {
		DataSource ds = null;
		DataSourceDescriptor<?> desc = null;
		InvocationHandler handler = null;
		final AtomicBoolean invoked = new AtomicBoolean(false);
		Class<?>[] interfaces = {
			DataSource.class
		};

		handler = (Object proxy, Method method, Object[] args) -> {
			return null;
		};

		ds = DataSource.class
			.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, handler));

		invoked.set(false);
		desc = new TestDataSourceDescriptor<>(ds, false, () -> invoked.set(true));
		Assertions.assertSame(ds, desc.getDataSource());
		Assertions.assertFalse(desc.isManagedTransactions());
		Assertions.assertFalse(invoked.get());
		desc.close();
		Assertions.assertTrue(invoked.get());
	}

}
