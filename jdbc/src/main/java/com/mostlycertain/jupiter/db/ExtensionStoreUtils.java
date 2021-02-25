package com.mostlycertain.jupiter.db;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ExtensionStoreUtils {
    private ExtensionStoreUtils() {
        // Private so instances can not be created
    }

    static <T> Optional<T> get(
            final ExtensionContext.Store store,
            final String key,
            final Class<T> valueClass
    ) {
        return Optional.ofNullable(store.get(key, valueClass));
    }

    @SuppressWarnings("unchecked")
    static <T> void addToList(
            final ExtensionContext.Store store,
            final String key,
            final T value
    ) {
        store.getOrComputeIfAbsent(key, k -> new ArrayList<T>(), List.class)
                .add(value);
    }

    @SuppressWarnings("unchecked")
    static <T> List<T> getList(
            final ExtensionContext.Store store,
            final String key
    ) {
        return Collections.unmodifiableList(
                store.getOrDefault(
                        key,
                        List.class,
                        Collections.emptyList()));
    }
}
