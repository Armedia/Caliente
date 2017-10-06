package com.armedia.caliente.engine.xml.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.transform.TestObjectFacade;
import com.armedia.caliente.engine.transform.TestTransformationContext;
import com.armedia.caliente.engine.transform.TransformationCompletedException;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.xml.Comparison;
import com.armedia.caliente.engine.xml.Expression;
import com.armedia.caliente.engine.xml.RegularExpression;
import com.armedia.caliente.engine.xml.actions.AbortTransformation;
import com.armedia.caliente.engine.xml.actions.AttributeSet;
import com.armedia.caliente.engine.xml.actions.EndTransformation;
import com.armedia.caliente.engine.xml.actions.MapAttributeValue;
import com.armedia.caliente.engine.xml.actions.MapValueCase;
import com.armedia.caliente.engine.xml.actions.SecondarySubtypeAdd;
import com.armedia.caliente.engine.xml.actions.SecondarySubtypeRemove;
import com.armedia.caliente.engine.xml.actions.SecondarySubtypeReplace;
import com.armedia.caliente.engine.xml.actions.SubtypeReplace;
import com.armedia.caliente.engine.xml.actions.SubtypeSet;
import com.armedia.caliente.store.CmfDataType;

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
		try {
			action.apply(ctx);
			Assert.fail("Did not fail with a null expression");
		} catch (TransformationException e) {
			// All is well
		}

		String value = UUID.randomUUID().toString();
		action.setValue(new Expression(value));

		action.apply(ctx);

		Assert.assertEquals(value, object.getSubtype());

		action.setValue(new Expression(
			"                                                       \n   \t   \n                       \t \t   \n                               "));
		try {
			action.apply(ctx);
			Assert.fail("Did not fail with a blank-valued expression");
		} catch (TransformationException e) {
			// All is well
		}

		action.setValue(new Expression());
		try {
			action.apply(ctx);
			Assert.fail("Did not fail with a null-valued expression");
		} catch (TransformationException e) {
			// All is well
		}
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

		action.setRegex(new RegularExpression("^dctm:"));

		action.apply(ctx);
		Assert.assertEquals("test_subtype_value", object.getSubtype());

		// Reset the value...
		object.setSubtype("dctm:test_subtype_value");

		action.setReplacement(new Expression("alfresco||"));

		action.apply(ctx);
		Assert.assertEquals("alfresco||test_subtype_value", object.getSubtype());

		action.setRegex(new RegularExpression("(test_)(subtype)(_value)"));
		action.setReplacement(new Expression("$3$1$2"));
		action.apply(ctx);
		Assert.assertEquals("alfresco||_valuetest_subtype", object.getSubtype());
	}

	@Test
	public void testSecondarySubtypeAdd() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();

		SecondarySubtypeAdd action = new SecondarySubtypeAdd();
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().isEmpty());

		action.setValue(new Expression());
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().isEmpty());

		String testValue = UUID.randomUUID().toString();
		action.setValue(new Expression(testValue));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().isEmpty());
		Assert.assertTrue(object.getSecondarySubtypes().contains(testValue));
	}

	@Test
	public void testSecondarySubtypeRemove() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();

		SecondarySubtypeRemove action = new SecondarySubtypeRemove();
		action.setValue(new Expression());

		// First things first: remove by equals
		Set<String> values = new TreeSet<>();
		values.addAll(Arrays.asList("a", "b", "c"));

		action.setComparison(Comparison.EQ);
		object.getSecondarySubtypes().addAll(values);
		for (String s : values) {
			Assert.assertTrue(object.getSecondarySubtypes().contains(s));
			action.setValue(new Expression(s));
			action.apply(ctx);
			Assert.assertFalse(object.getSecondarySubtypes().contains(s));
		}

		action.setComparison(Comparison.EQI);
		object.getSecondarySubtypes().addAll(values);
		for (String s : values) {
			Assert.assertTrue(s, object.getSecondarySubtypes().contains(s));
			action.setValue(new Expression(s.toUpperCase()));
			action.apply(ctx);
			Assert.assertFalse(s, object.getSecondarySubtypes().contains(s));
		}

		action.setComparison(Comparison.NEQ);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("a"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NEQI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("B"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("b"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.LT);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("b"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("b"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.LTI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("C"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.GT);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("a"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.GTI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("A"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NGE);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("b"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("b"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NGEI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("C"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NLE);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("a"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NLEI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("A"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.LE);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("b"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.LEI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("A"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("b"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.GE);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("a"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.GEI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("b"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NGT);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("b"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NGTI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("C"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NLT);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("a"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NLTI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("B"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("c"));

		values.clear();
		object.getSecondarySubtypes().clear();

		values.addAll(Arrays.asList("first_value", "second_secondary", "first_secondary", "last_value"));

		action.setComparison(Comparison.SW);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("first_"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.SWI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("SeCoNd_"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NSW);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("first_"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NSWI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("SeCoNd_"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.EW);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("_value"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.EWI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("_SeCoNdArY"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NEW);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("_secondary"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NEWI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("_vAlUe"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.CN);
		object.getSecondarySubtypes().addAll(values);
		action.setValue(new Expression("_va"));
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.CNI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("RsT"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NCN);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("seco"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NCNI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("SeCo"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.RE);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("t_[sv]"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.REI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("T_[Sv]"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NRE);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("e$"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NREI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("^F"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.GLOB);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("first*"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.GLOBI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("*sEcO*"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NGLOB);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("last*"));
		action.apply(ctx);
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NGLOBI);
		object.getSecondarySubtypes().addAll(values);
		Assert.assertEquals(values, object.getSecondarySubtypes());
		action.setValue(new Expression("*sT_v?L?e"));
		action.apply(ctx);
		Assert.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assert.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assert.assertTrue(object.getSecondarySubtypes().contains("last_value"));
	}

	@Test
	public void testSecondarySubtypeReplace() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();

		SecondarySubtypeReplace action = new SecondarySubtypeReplace();
		action.apply(ctx);

		Set<String> secondaries = object.getSecondarySubtypes();
		Set<String> values = new TreeSet<>(Arrays.asList("abc123hij", "123def789", "1b3d5f7h9j"));
		secondaries.addAll(values);

		try {
			action.apply(ctx);
			Assert.fail("Did not fail with neither a regex or a replacement");
		} catch (TransformationException e) {
			// All is well
		}

		action.setRegex(new RegularExpression("\\d"));

		secondaries.clear();
		secondaries.addAll(values);
		action.apply(ctx);
		Assert.assertTrue(secondaries.contains("abchij"));
		Assert.assertTrue(secondaries.contains("def"));
		Assert.assertTrue(secondaries.contains("bdfhj"));

		action.setReplacement(new Expression("X"));

		secondaries.clear();
		secondaries.addAll(values);
		action.apply(ctx);
		Assert.assertTrue(secondaries.contains("abcXXXhij"));
		Assert.assertTrue(secondaries.contains("XXXdefXXX"));
		Assert.assertTrue(secondaries.contains("XbXdXfXhXj"));

		secondaries.clear();

		action.setRegex(new RegularExpression("[ABCDEFGHIJ]").setCaseSensitive(true));
		secondaries.addAll(values);
		action.apply(ctx);
		Assert.assertEquals(values, secondaries);

		action.setRegex(new RegularExpression("[ABCDEFGHIJ]").setCaseSensitive(false));
		action.apply(ctx);
		Assert.assertTrue(secondaries.contains("XXX123XXX"));
		Assert.assertTrue(secondaries.contains("123XXX789"));
		Assert.assertTrue(secondaries.contains("1X3X5X7X9X"));
	}

	@Test
	public void testAttributeSet() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();

		AttributeSet action = new AttributeSet();
		try {
			action.apply(ctx);
			Assert.fail("Did not fail with a null name");
		} catch (TransformationException e) {
			// All is well
		}

		final String attributeName = "testAttribute";
		final String attributeValue = UUID.randomUUID().toString();

		action.setName(new Expression(attributeName));
		action.setValue(new Expression(attributeValue));

		Assert.assertFalse(object.getAtt().containsKey(attributeName));
		action.apply(ctx);
		Assert.assertTrue(object.getAtt().containsKey(attributeName));
		Assert.assertEquals(attributeValue, object.getAtt().get(attributeName).getValue());
	}

	@Test
	public void testMapAttributeValue() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();

		MapAttributeValue action = new MapAttributeValue();
		action.apply(ctx);

		List<MapValueCase> cases = action.getCases();

		List<Pair<String, String>> data = new ArrayList<>();
		data.add(Pair.of("alpha", "OMEGA"));
		data.add(Pair.of("James Bond", "Ernst Stavro Blofeld"));
		data.add(Pair.of("Kingsman", "Statesman"));
		data.add(Pair.of("123", "998877"));

		for (Pair<String, String> p : data) {
			MapValueCase c = new MapValueCase();
			c.setValue(new Expression(p.getLeft()));
			c.setReplacement(new Expression(p.getRight()));
			cases.add(c);
		}
		// final String defaultValue = UUID.randomUUID().toString();
		// action.setDefaultValue(new Expression(defaultValue));

		final String attributeName = UUID.randomUUID().toString();
		action.setName(new Expression(attributeName));
		TypedValue tv = new TypedValue(attributeName, CmfDataType.STRING, true);
		object.getAtt().put(attributeName, tv);
		Set<String> expected = new HashSet<>();
		for (Pair<String, String> p : data) {
			expected.add(p.getRight());
			tv.getValues().add(p.getLeft());
			String fixed = String.format("nochange-%s", p.getLeft());
			expected.add(fixed);
			tv.getValues().add(fixed);
		}

		action.apply(ctx);
		tv = object.getAtt().get(attributeName);
		Assert.assertNotNull(tv);
		Assert.assertFalse(tv.isEmpty());
		Assert.assertEquals(expected.size(), tv.getSize());
		for (Object o : tv.getValues()) {
			Assert.assertTrue(o.toString(), expected.contains(o));
		}
	}
}