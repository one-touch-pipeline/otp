package de.dkfz.tbi.otp.utils

import static org.springframework.util.Assert.*

class CollectionUtils {

    /**
     * Ensures that a collection contains exactly one element.
     * @return The only element in the collection.
     * @throws AssertionError If the collection does not contain exactly one element.
     */
    public static <T> T exactlyOneElement(final Collection<T> collection, String customErrorMessage = null) throws AssertionError {
        return singleElement(collection, false, customErrorMessage)
    }

    /**
     * Ensures that a collection contains at most one element.
     * @return The only element in the collection or <code>null</code> if the collection is empty.
     * @throws AssertionError If the collection does not contain at most one element.
     */
    public static <T> T atMostOneElement(final Collection<T> collection, String customErrorMessage = null) throws AssertionError {
        return singleElement(collection, true, customErrorMessage)
    }

    /**
     * Ensures that a collection contains only one element.
     * @param allowNone Whether this method shall succeed if the collection is empty.
     * @return The only element in the collection or <code>null</code> if the collection is empty and
     * <code>allowNone</code> is <code>true</code>.
     * @throws AssertionError If the collection contains an unexpected number of elements.
     */
    public static <T> T singleElement(
            final Collection<T> collection, final boolean allowNone = true, String customErrorMessage = null) throws AssertionError {
        notNull collection
        final int size = collection.size()
        if (size == 1) {
            return collection.iterator().next()
        } else if (size == 0 && allowNone) {
            return null
        } else {
            String defaultMessage = "Collection contains ${size} elements. Expected 1."
            String message = (customErrorMessage ? "${customErrorMessage}\n${defaultMessage}" : defaultMessage)
            throw new AssertionError(message)
        }
    }

    /**
     * Returns whether two collections contain the same set of elements. Each collection must not contain any two
     * elements which are equal.
     */
    public static <T> boolean containSame(final Collection<? extends T> c1, final Collection<? extends T> c2) {
        final c1Set = c1.toSet()
        assert c1Set.size() == c1.size() : "c1 contains elements which are equal."
        final c2Set = c2.toSet()
        assert c2Set.size() == c2.size() : "c2 contains elements which are equal."
        return c1Set == c2Set
    }

    /**
     * Returns the value for the specified key of the given map.
     * In case the map does not contain specified key, it gets defined with the given default value.
     * @param d default value
     */
    public static <K, V> V getOrPut(Map<K, V> map, K key, V d) {
        V value = map.get(key)
        if (value != null || map.containsKey(key)) {
            return value
        } else {
            map.put(key, d)
            return d
        }
    }
}
