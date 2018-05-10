package com.armedia.caliente.engine.dynamic.xml;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({
	TYPE
})
public @interface XmlSchema {
	String value() default XmlBase.DEFAULT_SCHEMA;
}
