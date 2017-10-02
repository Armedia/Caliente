package com.armedia.caliente.engine.transform.xml.actions;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.transform.TestObjectFacade;
import com.armedia.caliente.engine.transform.TestTransformationContext;
import com.armedia.caliente.engine.transform.TransformationCompletedException;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Comparison;
import com.armedia.caliente.engine.transform.xml.Expression;

public class ActionsTest {

	@Test
	public void testAbortTransformation() {
		try {
			new AbortTransformation().apply(new TestTransformationContext());
			Assert.fail("Failed to abort the transformation");
		} catch (TransformationException e) {
			// All is well
		}
	}

	@Test
	public void testEndTransformation() throws TransformationException {
		try {
			new EndTransformation().apply(new TestTransformationContext());
			Assert.fail("Failed to end the transformation");
		} catch (TransformationCompletedException e) {
			// All is well
		}
	}

	@Test
	public void testSubtypeSet() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();
		Assert.assertNull(object.getSubtype());

		SubtypeSet action = new SubtypeSet();
		Expression e = new Expression();
		e.setLang(null);
		String value = UUID.randomUUID().toString();
		e.setScript(value);
		action.setSubtype(e);

		action.apply(ctx);

		Assert.assertEquals(value, object.getSubtype());

		e.setScript(
			"                                                       \n   \t   \n                       \t \t   \n                               ");
		action.apply(ctx);
		// Value is an empty string, so no change...
		Assert.assertEquals(value, object.getSubtype());

		e.setScript(null);
		action.apply(ctx);
		// Value is a null string, so no change...
		Assert.assertEquals(value, object.getSubtype());
	}

	@Test
	public void testSubtypeReplace() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();
		object.setSubtype("dctm:test_subtype_value");

		SubtypeReplace action = new SubtypeReplace();

		try {
			action.apply(ctx);
			Assert.fail("Did not fail with neither a regex or a replacement");
		} catch (TransformationException e) {
			// All is well
		}

		Expression regex = new Expression();
		action.setRegex(regex);
		regex.setLang(null);
		regex.setScript("^dctm:");

		action.apply(ctx);
		Assert.assertEquals("test_subtype_value", object.getSubtype());

		// Reset the value...
		object.setSubtype("dctm:test_subtype_value");

		Expression replacement = new Expression();
		action.setReplacement(replacement);
		replacement.setLang(null);
		replacement.setScript("alfresco||");

		action.apply(ctx);
		Assert.assertEquals("alfresco||test_subtype_value", object.getSubtype());

		regex.setScript("(test_)(subtype)(_value)");
		replacement.setScript("$3$1$2");
		action.apply(ctx);
		Assert.assertEquals("alfresco||_valuetest_subtype", object.getSubtype());
	}

	@Test
	public void testDecoratorAdd() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();

		DecoratorAdd action = new DecoratorAdd();
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().isEmpty());

		Expression e = new Expression();
		action.setDecorator(e);
		e.setLang(null);
		e.setScript(null);
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().isEmpty());

		String testValue = UUID.randomUUID().toString();
		e.setScript(testValue);
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().isEmpty());
		Assert.assertTrue(object.getDecorators().contains(testValue));
	}

	@Test
	public void testDecoratorRemove() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();

		DecoratorRemove action = new DecoratorRemove();
		Expression e = new Expression();
		action.setDecorator(e);

		// First things first: remove by equals
		Set<String> values = new TreeSet<>();
		values.addAll(Arrays.asList("a", "b", "c"));

		action.setComparison(Comparison.EQ);
		object.getDecorators().addAll(values);
		for (String s : values) {
			e.setLang(null);
			e.setScript(s);
			Assert.assertTrue(object.getDecorators().contains(s));
			action.apply(ctx);
			Assert.assertFalse(object.getDecorators().contains(s));
		}

		action.setComparison(Comparison.EQI);
		object.getDecorators().addAll(values);
		for (String s : values) {
			e.setLang(null);
			e.setScript(s.toUpperCase());
			Assert.assertTrue(s, object.getDecorators().contains(s));
			action.apply(ctx);
			Assert.assertFalse(s, object.getDecorators().contains(s));
		}

		action.setComparison(Comparison.NEQ);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("a");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NEQI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("B");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertTrue(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.LT);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("b");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertTrue(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.LTI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("C");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.GT);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("a");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.GTI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("A");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NGE);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("b");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertTrue(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NGEI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("C");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NLE);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("a");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NLEI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("A");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.LE);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("b");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.LEI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("A");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertTrue(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.GE);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("a");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.GEI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("b");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NGT);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("b");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NGTI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("C");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NLT);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("a");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NLTI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("B");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		values.clear();
		object.getDecorators().clear();

		values.addAll(Arrays.asList("first_value", "second_decorator", "first_decorator", "last_value"));

		action.setComparison(Comparison.SW);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("first_");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.SWI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("SeCoNd_");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NSW);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("first_");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NSWI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("SeCoNd_");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.EW);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("_value");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.EWI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("_DeCoRaToR");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NEW);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("_decorator");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NEWI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("_vAlUe");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.CN);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("_va");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.CNI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("RsT");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NCN);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("seco");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NCNI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("DeCo");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.RE);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("t_[dv]");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.REI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("T_[Dv]");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NRE);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("e$");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NREI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("^F");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.GLOB);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("first*");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.GLOBI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("*dEcO*");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NGLOB);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("last*");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NGLOBI);
		object.getDecorators().addAll(values);
		e.setLang(null);
		e.setScript("*sT_v?L?e");
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));
	}

	@Test
	public void testDecoratorReplace() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();

		DecoratorReplace action = new DecoratorReplace();
		action.apply(ctx);

		Set<String> decorators = object.getDecorators();
		Set<String> values = new TreeSet<>(Arrays.asList("abc123hij", "123def789", "1b3d5f7h9k"));
		decorators.addAll(values);

		try {
			action.apply(ctx);
			Assert.fail("Did not fail with neither a regex or a replacement");
		} catch (TransformationException e) {
			// All is well
		}

		Expression regex = new Expression();
		action.setRegex(regex);
		regex.setLang(null);
		regex.setScript("\\d");

		decorators.addAll(values);
		action.apply(ctx);
		Assert.assertTrue(decorators.contains("abchij"));
		Assert.assertTrue(decorators.contains("def"));
		Assert.assertTrue(decorators.contains("bdfhk"));

		Expression replacement = new Expression();
		action.setReplacement(replacement);
		replacement.setLang(null);
		replacement.setScript("X");

		decorators.addAll(values);
		action.apply(ctx);
		Assert.assertTrue(decorators.contains("abcXXXhij"));
		Assert.assertTrue(decorators.contains("XXXdefXXX"));
		Assert.assertTrue(decorators.contains("XbXdXfXhXk"));

	}
}