package com.dark.shade.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Column annotation
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Column {

  String name() default "";

  boolean unique() default false;

  boolean nullable() default true;
}
