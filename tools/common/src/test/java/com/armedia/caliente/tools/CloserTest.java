package com.armedia.caliente.tools;

import java.io.Closeable;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CloserTest {

	@Test
	public void testCloseQuietly() throws Exception {
		Closeable c = EasyMock.createStrictMock(Closeable.class);

		Assertions.assertNull(Closer.closeQuietly(null));

		EasyMock.reset(c);
		c.close();
		EasyMock.expectLastCall().once();
		EasyMock.replay(c);
		Assertions.assertNull(Closer.closeQuietly(c));
		EasyMock.verify(c);

		RuntimeException e = new RuntimeException();
		EasyMock.reset(c);
		c.close();
		EasyMock.expectLastCall().andThrow(e).once();
		EasyMock.replay(c);
		Assertions.assertSame(e, Closer.closeQuietly(c));
		EasyMock.verify(c);
	}

}
