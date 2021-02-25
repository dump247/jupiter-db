package com.mostlycertain.jupiter.db;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class ExtensionStoreUtils {
    private ExtensionStoreUtils() {
        // Private so instances can not be created
    }

    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> getMap(
            final ExtensionContext.Store store,
            final String key
    ) {
        return Collections.unmodifiableMap(
                store.getOrDefault(
                        key,
                        Map.class,
                        Collections.emptyMap()));
    }

    @SuppressWarnings("unchecked")
    static <T> List<T> loadList(
            final ExtensionContext.Store store,
            final String key
    ) {
        return store.getOrComputeIfAbsent(
                key,
                k -> new ArrayList<T>(),
                List.class);
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
