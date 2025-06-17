package com.unloadbrain.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface GenerateEntity {

    String packageName() default "";

    String entityName() default "DefaultEntity";

    String tableName() default "default_table_name";
}