/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.tools.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
