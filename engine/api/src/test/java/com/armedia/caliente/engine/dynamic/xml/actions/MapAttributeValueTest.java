package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.engine.dynamic.TestObjectFacade;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.engine.dynamic.xml.actions.AttributeActions.MapValue;
import com.armedia.caliente.store.CmfValue;

public class MapAttributeValueTest {
	@Test
	public void test() throws ActionException {
		TestObjectContext ctx = new TestObjectContext();
		TestObjectFacade object = ctx.getDynamicObject();

		MapValue action = new MapValue();
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
		DynamicValue tv = new DynamicValue(attributeName, CmfValue.Type.STRING, true);
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
		Assertions.assertNotNull(tv);
		Assertions.assertFalse(tv.isEmpty());
		Assertions.assertEquals(expected.size(), tv.getSize());
		for (Object o : tv.getValues()) {
			Assertions.assertTrue(expected.contains(o), o.toString());
		}
	}
}