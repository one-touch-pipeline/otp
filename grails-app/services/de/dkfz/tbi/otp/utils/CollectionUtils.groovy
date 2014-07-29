package de.dkfz.tbi.otp.utils

import static org.springframework.util.Assert.*

class CollectionUtils {

    /**
     * Ensures that a collection contains exactly one element.
     * @return The only element in the collection.
     * @throws AssertionError If the collection does not contain exactly one element.
     */
    public static <T> T exactlyOneElement(final Collection<T> collection) throws AssertionError {
        return singleElement(collection, false)
    }

    /**
     * Ensures that a collection contains at most one element.
     * @return The only element in the collection or <code>null</code> if the collection is empty.
     * @throws AssertionError If the collection does not contain at most one element.
     */
    public static <T> T atMostOneElement(final Collection<T> collection) throws AssertionError {
        return singleElement(collection, true)
    }

    /**
     * Ensures that a collection contains only one element.
     * @param allowNone Whether this method shall succeed if the collection is empty.
     * @return The only element in the collection or <code>null</code> if the collection is empty and
     * <code>allowNone</code> is <code>true</code>.
     * @throws AssertionError If the collection contains an unexpected number of elements.
     */
    public static <T> T singleElement(
            final Collection<T> collection, final boolean allowNone = true) throws AssertionError {
        notNull collection
        final int size = collection.size()
        if (size == 1) {
            return collection.first()
        } else if (size == 0 && allowNone) {
            return null
        } else {
            throw new AssertionError("Collection contains ${size} elements. Expected 1.")
        }
    }
}
