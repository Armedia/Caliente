package com.armedia.caliente.engine.cmis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.runtime.util.AbstractPageFetcher;
import org.apache.chemistry.opencmis.client.runtime.util.CollectionPageIterable;
import org.apache.chemistry.opencmis.client.runtime.util.EmptyItemIterable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CmisPagingIteratorTest {

	private static final AbstractPageFetcher.Page<String> EMPTY_PAGE = new AbstractPageFetcher.Page<>(
		Collections.emptyList(), 0, false);

	@Test
	public void test() {

		// Null doesn't fail ... just causes an empty iterator
		{
			CmisPagingIterator<String> it = new CmisPagingIterator<>(null);
			Assertions.assertNull(it.getResults());
			Assertions.assertFalse(it.hasNext());
			Assertions.assertThrows(NoSuchElementException.class, () -> it.next());
		}
		{
			ItemIterable<String> iit = new EmptyItemIterable<>();
			CmisPagingIterator<String> it = new CmisPagingIterator<>(iit);
			Assertions.assertSame(iit, it.getResults());
			Assertions.assertFalse(it.hasNext());
			Assertions.assertThrows(NoSuchElementException.class, () -> it.next());
		}
		{
			final int numPages = 100;
			final int numItems = 1000;
			final int maxCount = (numPages * numItems);
			List<List<String>> pages = new ArrayList<>(numPages);
			for (int p = 0; p < numPages; p++) {
				List<String> l = new ArrayList<>(numItems);
				for (int i = 0; i < numItems; i++) {
					l.add(String.format("%d:%d", p, i));
				}
				pages.add(l);
			}

			AbstractPageFetcher<String> fetcher = new AbstractPageFetcher<String>(numPages * numItems) {
				@Override
				protected Page<String> fetchPage(long skipCount) {
					// Is this the right thing to do?
					if (skipCount >= maxCount) { return CmisPagingIteratorTest.EMPTY_PAGE; }
					List<String> l = pages.get((int) (skipCount / numItems));
					return new Page<>(l, l.size(), skipCount < maxCount);
				}
			};

			ItemIterable<String> results = new CollectionPageIterable<>(fetcher);
			CmisPagingIterator<String> it = new CmisPagingIterator<>(results);
			for (int p = 0; p < numPages; p++) {
				for (int i = 0; i < numItems; i++) {
					Assertions.assertEquals((p * numItems) + i, it.getCount());
					final String current = String.format("%d:%d", p, i);
					Assertions.assertTrue(it.hasNext(), current);
					Assertions.assertTrue(it.hasNext(), current);
					String item = it.next();
					// The next item must precisely match our expected element
					Assertions.assertEquals(current, item);
				}
			}
		}
	}
}