package com.mostlycertain.jupiter.db;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

final class AnnotationUtil {
    private AnnotationUtil() {
        // Private so instances can not be created
    }

    /**
     * Get an annotation from an element.
     *
     * If the annotation is marked {@link Repeatable repeatable}, return all the instances of the
     * annotation.
     *
     * @param element         Element to get the annotations from.
     * @param annotationClass Annotation type to get.
     * @param <A>             Annotation type to get.
     * @return Annotations or empty stream if no annotations are set.
     */
    static <A extends Annotation> Stream<A> getAnnotations(
            final AnnotatedElement element,
            final Class<A> annotationClass
    ) {
        final Repeatable repeatable = annotationClass.getAnnotation(Repeatable.class);

        if (repeatable == null) {
            return singleAnnotation(element, annotationClass);
        }

        try {
            final Annotation repeatedAnnotation = element.getAnnotation(repeatable.value());

            if (repeatedAnnotation != null) {
                @SuppressWarnings("unchecked") final A[] result = (A[]) repeatedAnnotation.getClass().getMethod("value").invoke(repeatedAnnotation);

                return Stream.of(result);
            } else {
                return singleAnnotation(element, annotationClass);
            }
        } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static <A extends Annotation> Stream<A> singleAnnotation(
            final AnnotatedElement element,
            final Class<A> annotationClass
    ) {
        final A annotation = element.getAnnotation(annotationClass);

        return annotation == null ? Stream.empty() : Stream.of(annotation);
    }
}
