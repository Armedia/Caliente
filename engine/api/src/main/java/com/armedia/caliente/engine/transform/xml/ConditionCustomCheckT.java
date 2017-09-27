
package com.armedia.caliente.engine.transform.xml;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCustomCheck.t")
public class ConditionCustomCheckT extends ConditionExpressionT {

	private static class ConstantConditionFactory implements ConditionFactory {
		private final Class<?> klass;

		public ConstantConditionFactory(Class<?> klass) {
			this.klass = klass;
		}

		@Override
		public <V> Condition getConditionInstance(TransformationContext<V> ctx) {
			try {
				return Condition.class.cast(this.klass.newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeTransformationException(
					String.format("Failed to instantiate a condition of class [%s]", this.klass.getCanonicalName()), e);
			}
		}
	}

	private static final ConcurrentMap<String, ConditionFactory> FACTORIES = new ConcurrentHashMap<>();

	private static <V> ConditionFactory getFactory(final String className) throws Exception {

		final Class<?> c = Class.forName(className);
		if (Condition.class.isAssignableFrom(c)) {
			// It's a condition, so try to get an instance right off the bat!
			return ConcurrentUtils.createIfAbsent(ConditionCustomCheckT.FACTORIES, className,
				new ConcurrentInitializer<ConditionFactory>() {
					@Override
					public ConditionFactory get() throws ConcurrentException {
						return new ConstantConditionFactory(c);
					}
				});
		}

		if (ConditionFactory.class.isAssignableFrom(c)) {
			// It's a factory, so we use it to get an instance
			return ConcurrentUtils.createIfAbsent(ConditionCustomCheckT.FACTORIES, className,
				new ConcurrentInitializer<ConditionFactory>() {
					@Override
					public ConditionFactory get() throws ConcurrentException {
						try {
							return ConditionFactory.class.cast(c.newInstance());
						} catch (InstantiationException | IllegalAccessException e) {
							throw new ConcurrentException(e);
						}
					}
				});
		}

		// If it's not a factory nor a condition, we don't know what to do with it...
		throw new RuntimeTransformationException(
			String.format("The class [%s] is neither a Condition nor a ConditionFactory", className));
	}

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		String className = Tools.toString(evaluate(ctx));
		if (className == null) { throw new RuntimeTransformationException(
			String.format("The given %s expression did not return a string value: %s", getLang(), getValue())); }

		try {
			ConditionFactory factory = ConditionCustomCheckT.getFactory(className);
			return factory.getConditionInstance(ctx).check(ctx);
		} catch (Exception e) {
			throw new RuntimeTransformationException(e);
		}
	}

}