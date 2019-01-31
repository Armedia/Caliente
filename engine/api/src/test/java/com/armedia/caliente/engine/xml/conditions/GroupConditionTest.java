package com.armedia.caliente.engine.xml.conditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.conditions.ConditionGroup;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupAnd;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupNand;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupNor;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupNot;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupOneof;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupOr;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupXnor;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupXor;
import com.armedia.caliente.engine.transform.TestObjectContext;
import com.armedia.caliente.engine.xml.ConditionTools;

public class GroupConditionTest {

	private static List<Condition> convertToList(Integer[] values, boolean lastIsResult) {
		Objects.requireNonNull(values, "Must provide a non-null array of booleans");
		if (values.length < 2) {
			throw new IllegalArgumentException("The array of booleans must contain at least two elements");
		}
		List<Condition> ret = new ArrayList<>(values.length);
		for (Integer i : values) {
			ret.add( //
				(i == null) //
					? ConditionTools.COND_FAIL //
					: ((i.intValue() != 0) //
						? ConditionTools.COND_TRUE //
						: ConditionTools.COND_FALSE //
					) //
			);
		}
		// The last element is the result, so remove it
		if (lastIsResult) {
			ret.remove(ret.size() - 1);
		}
		return ret;
	}

	public void testGrouped(ConditionGroup c, Integer[][] data) throws ConditionException {
		final String name = c.getClass().getSimpleName();
		final DynamicElementContext ctx = new TestObjectContext();
		for (Integer[] d : data) {
			List<Condition> conditions = GroupConditionTest.convertToList(d, true);
			final boolean expected = (d[5] != 0);
			c.getElements().clear();
			c.getElements().addAll(conditions);
			Assert.assertEquals(String.format("Failed while checking %s against %s", name, Arrays.toString(d)),
				expected, c.check(ctx));
		}
	}

	@Test
	public void testAnd() throws ConditionException {
		Integer[][] data = {
			{
				0, 0, 0, 0, 0, 0
			}, {
				0, 0, 0, 0, 1, 0
			}, {
				0, 0, 0, 1, 0, 0
			}, {
				0, 0, 0, 1, 1, 0
			}, {
				0, 0, 1, 0, 0, 0
			}, {
				0, 0, 1, 0, 1, 0
			}, {
				0, 0, 1, 1, 0, 0
			}, {
				0, 0, 1, 1, 1, 0
			}, {
				0, 1, 0, 0, 0, 0
			}, {
				0, 1, 0, 0, 1, 0
			}, {
				0, 1, 0, 1, 0, 0
			}, {
				0, 1, 0, 1, 1, 0
			}, {
				0, 1, 1, 0, 0, 0
			}, {
				0, 1, 1, 0, 1, 0
			}, {
				0, 1, 1, 1, 0, 0
			}, {
				0, 1, 1, 1, 1, 0
			}, {
				1, 0, 0, 0, 0, 0
			}, {
				1, 0, 0, 0, 1, 0
			}, {
				1, 0, 0, 1, 0, 0
			}, {
				1, 0, 0, 1, 1, 0
			}, {
				1, 0, 1, 0, 0, 0
			}, {
				1, 0, 1, 0, 1, 0
			}, {
				1, 0, 1, 1, 0, 0
			}, {
				1, 0, 1, 1, 1, 0
			}, {
				1, 1, 0, 0, 0, 0
			}, {
				1, 1, 0, 0, 1, 0
			}, {
				1, 1, 0, 1, 0, 0
			}, {
				1, 1, 0, 1, 1, 0
			}, {
				1, 1, 1, 0, 0, 0
			}, {
				1, 1, 1, 0, 1, 0
			}, {
				1, 1, 1, 1, 0, 0
			}, {
				1, 1, 1, 1, 1, 1
			},
		};
		testGrouped(new GroupAnd(), data);
	}

	@Test
	public void testOr() throws ConditionException {
		Integer[][] data = {
			{
				0, 0, 0, 0, 0, 0
			}, {
				0, 0, 0, 0, 1, 1
			}, {
				0, 0, 0, 1, 0, 1
			}, {
				0, 0, 0, 1, 1, 1
			}, {
				0, 0, 1, 0, 0, 1
			}, {
				0, 0, 1, 0, 1, 1
			}, {
				0, 0, 1, 1, 0, 1
			}, {
				0, 0, 1, 1, 1, 1
			}, {
				0, 1, 0, 0, 0, 1
			}, {
				0, 1, 0, 0, 1, 1
			}, {
				0, 1, 0, 1, 0, 1
			}, {
				0, 1, 0, 1, 1, 1
			}, {
				0, 1, 1, 0, 0, 1
			}, {
				0, 1, 1, 0, 1, 1
			}, {
				0, 1, 1, 1, 0, 1
			}, {
				0, 1, 1, 1, 1, 1
			}, {
				1, 0, 0, 0, 0, 1
			}, {
				1, 0, 0, 0, 1, 1
			}, {
				1, 0, 0, 1, 0, 1
			}, {
				1, 0, 0, 1, 1, 1
			}, {
				1, 0, 1, 0, 0, 1
			}, {
				1, 0, 1, 0, 1, 1
			}, {
				1, 0, 1, 1, 0, 1
			}, {
				1, 0, 1, 1, 1, 1
			}, {
				1, 1, 0, 0, 0, 1
			}, {
				1, 1, 0, 0, 1, 1
			}, {
				1, 1, 0, 1, 0, 1
			}, {
				1, 1, 0, 1, 1, 1
			}, {
				1, 1, 1, 0, 0, 1
			}, {
				1, 1, 1, 0, 1, 1
			}, {
				1, 1, 1, 1, 0, 1
			}, {
				1, 1, 1, 1, 1, 1
			},
		};
		testGrouped(new GroupOr(), data);
	}

	@Test
	public void testNot() throws ConditionException {
		GroupNot not = new GroupNot();
		final DynamicElementContext ctx = new TestObjectContext();
		not.setCondition(ConditionTools.COND_FALSE);
		Assert.assertTrue(not.check(ctx));
		not.setCondition(ConditionTools.COND_TRUE);
		Assert.assertFalse(not.check(ctx));
	}

	@Test
	public void testXor() throws ConditionException {
		Integer[][] data = {
			{
				0, 0, 0, 0, 0, 0
			}, {
				0, 0, 0, 0, 1, 1
			}, {
				0, 0, 0, 1, 0, 1
			}, {
				0, 0, 0, 1, 1, 0
			}, {
				0, 0, 1, 0, 0, 1
			}, {
				0, 0, 1, 0, 1, 0
			}, {
				0, 0, 1, 1, 0, 0
			}, {
				0, 0, 1, 1, 1, 1
			}, {
				0, 1, 0, 0, 0, 1
			}, {
				0, 1, 0, 0, 1, 0
			}, {
				0, 1, 0, 1, 0, 0
			}, {
				0, 1, 0, 1, 1, 1
			}, {
				0, 1, 1, 0, 0, 0
			}, {
				0, 1, 1, 0, 1, 1
			}, {
				0, 1, 1, 1, 0, 1
			}, {
				0, 1, 1, 1, 1, 0
			}, {
				1, 0, 0, 0, 0, 1
			}, {
				1, 0, 0, 0, 1, 0
			}, {
				1, 0, 0, 1, 0, 0
			}, {
				1, 0, 0, 1, 1, 1
			}, {
				1, 0, 1, 0, 0, 0
			}, {
				1, 0, 1, 0, 1, 1
			}, {
				1, 0, 1, 1, 0, 1
			}, {
				1, 0, 1, 1, 1, 0
			}, {
				1, 1, 0, 0, 0, 0
			}, {
				1, 1, 0, 0, 1, 1
			}, {
				1, 1, 0, 1, 0, 1
			}, {
				1, 1, 0, 1, 1, 0
			}, {
				1, 1, 1, 0, 0, 1
			}, {
				1, 1, 1, 0, 1, 0
			}, {
				1, 1, 1, 1, 0, 0
			}, {
				1, 1, 1, 1, 1, 1
			},
		};
		testGrouped(new GroupXor(), data);
	}

	@Test
	public void testNand() throws ConditionException {
		Integer[][] data = {
			{
				0, 0, 0, 0, 0, 1
			}, {
				0, 0, 0, 0, 1, 1
			}, {
				0, 0, 0, 1, 0, 1
			}, {
				0, 0, 0, 1, 1, 1
			}, {
				0, 0, 1, 0, 0, 1
			}, {
				0, 0, 1, 0, 1, 1
			}, {
				0, 0, 1, 1, 0, 1
			}, {
				0, 0, 1, 1, 1, 1
			}, {
				0, 1, 0, 0, 0, 1
			}, {
				0, 1, 0, 0, 1, 1
			}, {
				0, 1, 0, 1, 0, 1
			}, {
				0, 1, 0, 1, 1, 1
			}, {
				0, 1, 1, 0, 0, 1
			}, {
				0, 1, 1, 0, 1, 1
			}, {
				0, 1, 1, 1, 0, 1
			}, {
				0, 1, 1, 1, 1, 1
			}, {
				1, 0, 0, 0, 0, 1
			}, {
				1, 0, 0, 0, 1, 1
			}, {
				1, 0, 0, 1, 0, 1
			}, {
				1, 0, 0, 1, 1, 1
			}, {
				1, 0, 1, 0, 0, 1
			}, {
				1, 0, 1, 0, 1, 1
			}, {
				1, 0, 1, 1, 0, 1
			}, {
				1, 0, 1, 1, 1, 1
			}, {
				1, 1, 0, 0, 0, 1
			}, {
				1, 1, 0, 0, 1, 1
			}, {
				1, 1, 0, 1, 0, 1
			}, {
				1, 1, 0, 1, 1, 1
			}, {
				1, 1, 1, 0, 0, 1
			}, {
				1, 1, 1, 0, 1, 1
			}, {
				1, 1, 1, 1, 0, 1
			}, {
				1, 1, 1, 1, 1, 0
			},
		};
		testGrouped(new GroupNand(), data);
	}

	@Test
	public void testNor() throws ConditionException {
		Integer[][] data = {
			{
				0, 0, 0, 0, 0, 1
			}, {
				0, 0, 0, 0, 1, 0
			}, {
				0, 0, 0, 1, 0, 0
			}, {
				0, 0, 0, 1, 1, 0
			}, {
				0, 0, 1, 0, 0, 0
			}, {
				0, 0, 1, 0, 1, 0
			}, {
				0, 0, 1, 1, 0, 0
			}, {
				0, 0, 1, 1, 1, 0
			}, {
				0, 1, 0, 0, 0, 0
			}, {
				0, 1, 0, 0, 1, 0
			}, {
				0, 1, 0, 1, 0, 0
			}, {
				0, 1, 0, 1, 1, 0
			}, {
				0, 1, 1, 0, 0, 0
			}, {
				0, 1, 1, 0, 1, 0
			}, {
				0, 1, 1, 1, 0, 0
			}, {
				0, 1, 1, 1, 1, 0
			}, {
				1, 0, 0, 0, 0, 0
			}, {
				1, 0, 0, 0, 1, 0
			}, {
				1, 0, 0, 1, 0, 0
			}, {
				1, 0, 0, 1, 1, 0
			}, {
				1, 0, 1, 0, 0, 0
			}, {
				1, 0, 1, 0, 1, 0
			}, {
				1, 0, 1, 1, 0, 0
			}, {
				1, 0, 1, 1, 1, 0
			}, {
				1, 1, 0, 0, 0, 0
			}, {
				1, 1, 0, 0, 1, 0
			}, {
				1, 1, 0, 1, 0, 0
			}, {
				1, 1, 0, 1, 1, 0
			}, {
				1, 1, 1, 0, 0, 0
			}, {
				1, 1, 1, 0, 1, 0
			}, {
				1, 1, 1, 1, 0, 0
			}, {
				1, 1, 1, 1, 1, 0
			},
		};
		testGrouped(new GroupNor(), data);
	}

	@Test
	public void testXnor() throws ConditionException {
		Integer[][] data = {
			{
				0, 0, 0, 0, 0, 1
			}, {
				0, 0, 0, 0, 1, 0
			}, {
				0, 0, 0, 1, 0, 0
			}, {
				0, 0, 0, 1, 1, 1
			}, {
				0, 0, 1, 0, 0, 0
			}, {
				0, 0, 1, 0, 1, 1
			}, {
				0, 0, 1, 1, 0, 1
			}, {
				0, 0, 1, 1, 1, 0
			}, {
				0, 1, 0, 0, 0, 0
			}, {
				0, 1, 0, 0, 1, 1
			}, {
				0, 1, 0, 1, 0, 1
			}, {
				0, 1, 0, 1, 1, 0
			}, {
				0, 1, 1, 0, 0, 1
			}, {
				0, 1, 1, 0, 1, 0
			}, {
				0, 1, 1, 1, 0, 0
			}, {
				0, 1, 1, 1, 1, 1
			}, {
				1, 0, 0, 0, 0, 0
			}, {
				1, 0, 0, 0, 1, 1
			}, {
				1, 0, 0, 1, 0, 1
			}, {
				1, 0, 0, 1, 1, 0
			}, {
				1, 0, 1, 0, 0, 1
			}, {
				1, 0, 1, 0, 1, 0
			}, {
				1, 0, 1, 1, 0, 0
			}, {
				1, 0, 1, 1, 1, 1
			}, {
				1, 1, 0, 0, 0, 1
			}, {
				1, 1, 0, 0, 1, 0
			}, {
				1, 1, 0, 1, 0, 0
			}, {
				1, 1, 0, 1, 1, 1
			}, {
				1, 1, 1, 0, 0, 0
			}, {
				1, 1, 1, 0, 1, 1
			}, {
				1, 1, 1, 1, 0, 1
			}, {
				1, 1, 1, 1, 1, 0
			},
		};
		testGrouped(new GroupXnor(), data);
	}

	@Test
	public void testOneof() throws ConditionException {
		Integer[][] data = {
			{
				0, 0, 0, 0, 0, 0
			}, {
				0, 0, 0, 0, 1, 1
			}, {
				0, 0, 0, 1, 0, 1
			}, {
				0, 0, 0, 1, 1, 0
			}, {
				0, 0, 1, 0, 0, 1
			}, {
				0, 0, 1, 0, 1, 0
			}, {
				0, 0, 1, 1, 0, 0
			}, {
				0, 0, 1, 1, 1, 0
			}, {
				0, 1, 0, 0, 0, 1
			}, {
				0, 1, 0, 0, 1, 0
			}, {
				0, 1, 0, 1, 0, 0
			}, {
				0, 1, 0, 1, 1, 0
			}, {
				0, 1, 1, 0, 0, 0
			}, {
				0, 1, 1, 0, 1, 0
			}, {
				0, 1, 1, 1, 0, 0
			}, {
				0, 1, 1, 1, 1, 0
			}, {
				1, 0, 0, 0, 0, 1
			}, {
				1, 0, 0, 0, 1, 0
			}, {
				1, 0, 0, 1, 0, 0
			}, {
				1, 0, 0, 1, 1, 0
			}, {
				1, 0, 1, 0, 0, 0
			}, {
				1, 0, 1, 0, 1, 0
			}, {
				1, 0, 1, 1, 0, 0
			}, {
				1, 0, 1, 1, 1, 0
			}, {
				1, 1, 0, 0, 0, 0
			}, {
				1, 1, 0, 0, 1, 0
			}, {
				1, 1, 0, 1, 0, 0
			}, {
				1, 1, 0, 1, 1, 0
			}, {
				1, 1, 1, 0, 0, 0
			}, {
				1, 1, 1, 0, 1, 0
			}, {
				1, 1, 1, 1, 0, 0
			}, {
				1, 1, 1, 1, 1, 0
			},
		};
		testGrouped(new GroupOneof(), data);
	}
}