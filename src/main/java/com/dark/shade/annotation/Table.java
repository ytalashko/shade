package com.dark.shade.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Table annotation
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Table {

  String name() default "";
}
