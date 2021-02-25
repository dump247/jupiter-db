package com.mostlycertain.jupiter.db;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AnnotationUtilTest.NonRepeatable
@AnnotationUtilTest.RepeatableUsedOnce
@AnnotationUtilTest.RepeatableUsedMultiple
@AnnotationUtilTest.RepeatableUsedMultiple
class AnnotationUtilTest {
    @Test
    void nonRepeatableNotUsed() {
        assertEquals(0, AnnotationUtil.getAnnotations(AnnotationUtilTest.class, NonRepeatableNotUsed.class).count());
    }

    @Test
    void nonRepeatable() {
        assertEquals(1, AnnotationUtil.getAnnotations(AnnotationUtilTest.class, NonRepeatable.class).count());
    }

    @Test
    void repeatableNotUsed() {
        assertEquals(0, AnnotationUtil.getAnnotations(AnnotationUtilTest.class, RepeatableNotUsed.class).count());
    }

    @Test
    void repeatableUsedOnce() {
        assertEquals(1, AnnotationUtil.getAnnotations(AnnotationUtilTest.class, RepeatableUsedOnce.class).count());
    }

    @Test
    void repeatableUsedMultiple() {
        assertEquals(2, AnnotationUtil.getAnnotations(AnnotationUtilTest.class, RepeatableUsedMultiple.class).count());
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NonRepeatableNotUsed {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NonRepeatable {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(RepeatableNotUseds.class)
    public @interface RepeatableNotUsed {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RepeatableNotUseds {
        RepeatableNotUsed[] value();
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(RepeatableUsedOnces.class)
    public @interface RepeatableUsedOnce {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RepeatableUsedOnces {
        RepeatableUsedOnce[] value();
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(RepeatableUsedMultiples.class)
    public @interface RepeatableUsedMultiple {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RepeatableUsedMultiples {
        RepeatableUsedMultiple[] value();
    }
}
