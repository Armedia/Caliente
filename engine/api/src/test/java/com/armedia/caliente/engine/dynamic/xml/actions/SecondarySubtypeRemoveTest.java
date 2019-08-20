package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.engine.dynamic.TestObjectFacade;
import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.engine.dynamic.xml.Expression;

public class SecondarySubtypeRemoveTest {
	@Test
	public void test() throws ActionException {
		TestObjectContext ctx = new TestObjectContext();
		TestObjectFacade object = ctx.getDynamicObject();

		SecondarySubtypeRemove action = new SecondarySubtypeRemove();
		action.setName(new Expression());

		// First things first: remove by equals
		Set<String> values = new TreeSet<>();
		values.addAll(Arrays.asList("a", "b", "c"));

		action.setComparison(Comparison.EQ);
		object.getSecondarySubtypes().addAll(values);
		for (String s : values) {
			Assertions.assertTrue(object.getSecondarySubtypes().contains(s));
			action.setName(new Expression(s));
			action.apply(ctx);
			Assertions.assertFalse(object.getSecondarySubtypes().contains(s));
		}

		action.setComparison(Comparison.EQI);
		object.getSecondarySubtypes().addAll(values);
		for (String s : values) {
			Assertions.assertTrue(object.getSecondarySubtypes().contains(s), s);
			action.setName(new Expression(s.toUpperCase()));
			action.apply(ctx);
			Assertions.assertFalse(object.getSecondarySubtypes().contains(s), s);
		}

		action.setComparison(Comparison.NEQ);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("a"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NEQI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("B"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("b"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.LT);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("b"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("b"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.LTI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("C"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.GT);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("a"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.GTI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("A"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NGE);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("b"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("b"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NGEI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("C"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NLE);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("a"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NLEI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("A"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.LE);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("b"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.LEI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("A"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("b"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.GE);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("a"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.GEI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("b"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NGT);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("b"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NGTI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("C"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NLT);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("a"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("c"));

		action.setComparison(Comparison.NLTI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("B"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("a"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("b"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("c"));

		values.clear();
		object.getSecondarySubtypes().clear();

		values.addAll(Arrays.asList("first_value", "second_secondary", "first_secondary", "last_value"));

		action.setComparison(Comparison.SW);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("first_"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.SWI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("SeCoNd_"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NSW);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("first_"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NSWI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("SeCoNd_"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.EW);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("_value"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.EWI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("_SeCoNdArY"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NEW);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("_secondary"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NEWI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("_vAlUe"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.CN);
		object.getSecondarySubtypes().addAll(values);
		action.setName(new Expression("_va"));
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.CNI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("RsT"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NCN);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("seco"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NCNI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("SeCo"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.RE);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("t_[sv]"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.REI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("T_[Sv]"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NRE);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("e$"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NREI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("^F"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.GLOB);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("first*"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.GLOBI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("*sEcO*"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NGLOB);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("last*"));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("last_value"));

		action.setComparison(Comparison.NGLOBI);
		object.getSecondarySubtypes().addAll(values);
		Assertions.assertEquals(values, object.getSecondarySubtypes());
		action.setName(new Expression("*sT_v?L?e"));
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().contains("first_value"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("second_secondary"));
		Assertions.assertFalse(object.getSecondarySubtypes().contains("first_secondary"));
		Assertions.assertTrue(object.getSecondarySubtypes().contains("last_value"));
	}
}