package com.armedia.caliente.cli.parser;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class DuplicateParameterDefinitionExceptionTest {
	@Test
	public void testConstructor() {
		ParameterDefinition existing = new MutableParameterDefinition();
		ParameterDefinition incoming = new MutableParameterDefinition();

		try {
			new DuplicateParameterDefinitionException(null, null, null);
			Assert.fail("Did not fail with a null definitions");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			new DuplicateParameterDefinitionException(null, null, incoming);
			Assert.fail("Did not fail with a null incoming definition");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			new DuplicateParameterDefinitionException(null, existing, null);
			Assert.fail("Did not fail with a null existing definition");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			new DuplicateParameterDefinitionException(null, existing, existing);
			Assert.fail("Did not fail the same definition given twice");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		DuplicateParameterDefinitionException e = new DuplicateParameterDefinitionException(null, existing, incoming);
		Assert.assertSame(existing, e.getExisting());
		Assert.assertSame(incoming, e.getIncoming());
		Assert.assertNull(e.getMessage());

		String message = UUID.randomUUID().toString();
		e = new DuplicateParameterDefinitionException(message, existing, incoming);
		Assert.assertSame(existing, e.getExisting());
		Assert.assertSame(incoming, e.getIncoming());
		Assert.assertEquals(message, e.getMessage());
	}
}