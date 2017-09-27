
package com.armedia.caliente.engine.transform.xml;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionCustomAction.t", propOrder = {
	"className"
})
public class ActionCustomActionT extends ConditionalActionT {

	private static class ConstantTransformationFactory implements TransformationFactory {
		private final Class<?> klass;

		public ConstantTransformationFactory(Class<?> klass) {
			this.klass = klass;
		}

		@Override
		public <V> Transformation getTransformationInstance(TransformationContext<V> ctx) {
			try {
				return Transformation.class.cast(this.klass.newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeTransformationException(String.format("Failed to instantiate a transformation of class [%s]",
					this.klass.getCanonicalName()), e);
			}
		}
	}

	private static final ConcurrentMap<String, TransformationFactory> FACTORIES = new ConcurrentHashMap<>();

	private static <V> TransformationFactory getFactory(final String className) throws Exception {

		final Class<?> c = Class.forName(className);
		if (Transformation.class.isAssignableFrom(c)) {
			// It's a condition, so try to get an instance right off the bat!
			return ConcurrentUtils.createIfAbsent(ActionCustomActionT.FACTORIES, className,
				new ConcurrentInitializer<TransformationFactory>() {
					@Override
					public TransformationFactory get() throws ConcurrentException {
						return new ConstantTransformationFactory(c);
					}
				});
		}

		if (TransformationFactory.class.isAssignableFrom(c)) {
			// It's a factory, so we use it to get an instance
			return ConcurrentUtils.createIfAbsent(ActionCustomActionT.FACTORIES, className,
				new ConcurrentInitializer<TransformationFactory>() {
					@Override
					public TransformationFactory get() throws ConcurrentException {
						try {
							return TransformationFactory.class.cast(c.newInstance());
						} catch (InstantiationException | IllegalAccessException e) {
							throw new ConcurrentException(e);
						}
					}
				});
		}

		// If it's not a factory nor a condition, we don't know what to do with it...
		throw new RuntimeTransformationException(
			String.format("The class [%s] is neither a Transformation nor a TransformationFactory", className));
	}

	@XmlElement(name = "class-name", required = true)
	protected ExpressionT className;

	public ExpressionT getClassName() {
		return this.className;
	}

	public void setClassName(ExpressionT value) {
		this.className = value;
	}

	@Override
	protected <V> void applyTransformation(TransformationContext<V> ctx) {
		ExpressionT classNameExp = getClassName();
		if (classNameExp == null) { throw new RuntimeTransformationException("No classname expression given to evaluate"); }
		String className = Tools.toString(classNameExp.evaluate(ctx));
		if (className == null) { throw new RuntimeTransformationException(
			String.format("The given %s expression did not return a string value: %s", classNameExp.getLang(),
				classNameExp.getValue())); }

		try {
			ActionCustomActionT.getFactory(className).getTransformationInstance(ctx).apply(ctx);
		} catch (Exception e) {
			throw new RuntimeTransformationException(e);
		}
	}
}