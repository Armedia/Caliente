package com.armedia.caliente.engine.transform.xml.actions;

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
import com.armedia.caliente.engine.transform.xml.Comparison;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.caliente.engine.transform.xml.RegularExpression;
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
		String value = UUID.randomUUID().toString();
		action.setValue(new Expression(value));

		action.apply(ctx);

		Assert.assertEquals(value, object.getSubtype());

		action.setValue(new Expression(
			"                                                       \n   \t   \n                       \t \t   \n                               "));
		action.apply(ctx);
		// Value is an empty string, so no change...
		Assert.assertEquals(value, object.getSubtype());

		action.setValue(new Expression());
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
	public void testDecoratorAdd() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();

		DecoratorAdd action = new DecoratorAdd();
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().isEmpty());

		action.setDecorator(new Expression());
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().isEmpty());

		String testValue = UUID.randomUUID().toString();
		action.setDecorator(new Expression(testValue));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().isEmpty());
		Assert.assertTrue(object.getDecorators().contains(testValue));
	}

	@Test
	public void testDecoratorRemove() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();

		DecoratorRemove action = new DecoratorRemove();
		action.setDecorator(new Expression());

		// First things first: remove by equals
		Set<String> values = new TreeSet<>();
		values.addAll(Arrays.asList("a", "b", "c"));

		action.setComparison(Comparison.EQ);
		object.getDecorators().addAll(values);
		for (String s : values) {
			Assert.assertTrue(object.getDecorators().contains(s));
			action.setDecorator(new Expression(s));
			action.apply(ctx);
			Assert.assertFalse(object.getDecorators().contains(s));
		}

		action.setComparison(Comparison.EQI);
		object.getDecorators().addAll(values);
		for (String s : values) {
			Assert.assertTrue(s, object.getDecorators().contains(s));
			action.setDecorator(new Expression(s.toUpperCase()));
			action.apply(ctx);
			Assert.assertFalse(s, object.getDecorators().contains(s));
		}

		action.setComparison(Comparison.NEQ);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("a"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NEQI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("B"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertTrue(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.LT);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("b"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertTrue(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.LTI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("C"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.GT);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("a"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.GTI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("A"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NGE);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("b"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertTrue(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NGEI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("C"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NLE);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("a"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NLEI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("A"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.LE);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("b"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.LEI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("A"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertTrue(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.GE);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("a"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.GEI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("b"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NGT);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("b"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertTrue(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NGTI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("C"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NLT);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("a"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		action.setComparison(Comparison.NLTI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("B"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("a"));
		Assert.assertFalse(object.getDecorators().contains("b"));
		Assert.assertFalse(object.getDecorators().contains("c"));

		values.clear();
		object.getDecorators().clear();

		values.addAll(Arrays.asList("first_value", "second_decorator", "first_decorator", "last_value"));

		action.setComparison(Comparison.SW);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("first_"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.SWI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("SeCoNd_"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NSW);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("first_"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NSWI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("SeCoNd_"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.EW);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("_value"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.EWI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("_DeCoRaToR"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NEW);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("_decorator"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NEWI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("_vAlUe"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.CN);
		object.getDecorators().addAll(values);
		action.setDecorator(new Expression("_va"));
		Assert.assertEquals(values, object.getDecorators());
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.CNI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("RsT"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NCN);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("seco"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NCNI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("DeCo"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.RE);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("t_[dv]"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.REI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("T_[Dv]"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NRE);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("e$"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NREI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("^F"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertTrue(object.getDecorators().contains("first_decorator"));
		Assert.assertFalse(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.GLOB);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("first*"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertTrue(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.GLOBI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("*dEcO*"));
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NGLOB);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("last*"));
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().contains("first_value"));
		Assert.assertFalse(object.getDecorators().contains("second_decorator"));
		Assert.assertFalse(object.getDecorators().contains("first_decorator"));
		Assert.assertTrue(object.getDecorators().contains("last_value"));

		action.setComparison(Comparison.NGLOBI);
		object.getDecorators().addAll(values);
		Assert.assertEquals(values, object.getDecorators());
		action.setDecorator(new Expression("*sT_v?L?e"));
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
		Set<String> values = new TreeSet<>(Arrays.asList("abc123hij", "123def789", "1b3d5f7h9j"));
		decorators.addAll(values);

		try {
			action.apply(ctx);
			Assert.fail("Did not fail with neither a regex or a replacement");
		} catch (TransformationException e) {
			// All is well
		}

		action.setRegex(new RegularExpression("\\d"));

		decorators.clear();
		decorators.addAll(values);
		action.apply(ctx);
		Assert.assertTrue(decorators.contains("abchij"));
		Assert.assertTrue(decorators.contains("def"));
		Assert.assertTrue(decorators.contains("bdfhj"));

		action.setReplacement(new Expression("X"));

		decorators.clear();
		decorators.addAll(values);
		action.apply(ctx);
		Assert.assertTrue(decorators.contains("abcXXXhij"));
		Assert.assertTrue(decorators.contains("XXXdefXXX"));
		Assert.assertTrue(decorators.contains("XbXdXfXhXj"));

		decorators.clear();

		action.setRegex(new RegularExpression("[ABCDEFGHIJ]").setCaseSensitive(true));
		decorators.addAll(values);
		action.apply(ctx);
		Assert.assertEquals(values, decorators);

		action.setRegex(new RegularExpression("[ABCDEFGHIJ]").setCaseSensitive(false));
		action.apply(ctx);
		Assert.assertTrue(decorators.contains("XXX123XXX"));
		Assert.assertTrue(decorators.contains("123XXX789"));
		Assert.assertTrue(decorators.contains("1X3X5X7X9X"));
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