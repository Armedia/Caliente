package com.armedia.caliente.cli.parser;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.cli.MutableParameter;
import com.armedia.caliente.cli.Parameter;

public class DuplicateParameterDefinitionExceptionTest {
	@Test
	public void testConstructor() {
		Parameter existing = new MutableParameter();
		Parameter incoming = new MutableParameter();

		try {
			new DuplicateParameterException(null, null, null);
			Assert.fail("Did not fail with a null definitions");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			new DuplicateParameterException(null, null, incoming);
			Assert.fail("Did not fail with a null incoming definition");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			new DuplicateParameterException(null, existing, null);
			Assert.fail("Did not fail with a null existing definition");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			new DuplicateParameterException(null, existing, existing);
			Assert.fail("Did not fail the same definition given twice");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		DuplicateParameterException e = new DuplicateParameterException(null, existing, incoming);
		Assert.assertSame(existing, e.getExisting());
		Assert.assertSame(incoming, e.getIncoming());
		Assert.assertNull(e.getMessage());

		String message = UUID.randomUUID().toString();
		e = new DuplicateParameterException(message, existing, incoming);
		Assert.assertSame(existing, e.getExisting());
		Assert.assertSame(incoming, e.getIncoming());
		Assert.assertEquals(message, e.getMessage());
	}
}