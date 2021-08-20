package com.armedia.caliente.engine.tools;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.commons.utilities.Tools;

public class UUIDOrganizerTest {

	@Test
	public void testParser() {
		Object[][] data = {
			{
				"abc", null
			}, {
				"01234567-89ab-cdef-0123-456789abcdef", UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")
			}, {
				"1234567-89ab-cdef-0123-456789abcdef", null
			}, {
				"0123456x-89ab-cdef-0123-456789abcdef", null
			}, {
				"01234567-89ab-cdef-0123-456789abcdef;1.0", UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")
			}, {
				"https://somewhere/01234567-89ab-cdef-0123-456789abcdef/some-other-crap",
				UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")
			},
		};

		for (Object[] c : data) {

			final String source = Tools.toString(c[0]);
			final Object expectedResult = c[1];

			UUID result = null;
			try {
				result = UUIDOrganizer.parseUuid(source);
				Assertions.assertEquals(expectedResult, result);
			} catch (Throwable t) {
				if (Class.class.isInstance(expectedResult)) {
					// We're good ... it's an exception
					Class<?> tclass = Class.class.cast(expectedResult);
					Assertions.assertTrue(tclass.isInstance(t),
						"The exception of type " + t.getClass().getName() + " is not a subtype of " + tclass.getName());
				} else {
					Assertions.fail("Expected UUID " + expectedResult + ", but instead got an exception", t);
				}
			}
		}
	}
}