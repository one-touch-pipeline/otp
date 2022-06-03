/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.utils

import static org.springframework.util.Assert.notNull

class CollectionUtils {

    /**
     * Ensures that a collection contains exactly one element.
     * @return The only element in the collection.
     * @throws AssertionError If the collection does not contain exactly one element.
     */
    static <T> T exactlyOneElement(final Collection<T> collection, String customErrorMessage = null) throws AssertionError {
        return singleElement(collection, false, customErrorMessage)
    }

    /**
     * Ensures that a collection contains at most one element.
     * @return The only element in the collection or <code>null</code> if the collection is empty.
     * @throws AssertionError If the collection does not contain at most one element.
     */
    static <T> T atMostOneElement(final Collection<T> collection, String customErrorMessage = null) throws AssertionError {
        return singleElement(collection, true, customErrorMessage)
    }

    /**
     * Ensures that a collection contains only one element.
     * @param allowNone Whether this method shall succeed if the collection is empty.
     * @return The only element in the collection or <code>null</code> if the collection is empty and
     * <code>allowNone</code> is <code>true</code>.
     * @throws AssertionError If the collection contains an unexpected number of elements.
     */
    static <T> T singleElement(
            final Collection<T> collection, final boolean allowNone = true, String customErrorMessage = null) throws AssertionError {
        notNull collection
        final int size = collection.size()
        if (size == 1) {
            return collection.iterator().next()
        } else if (size == 0 && allowNone) {
            return null
        }
        String defaultMessage = "Collection contains ${size} elements. Expected 1"
        String message = (customErrorMessage ? "${customErrorMessage}\n${defaultMessage}" : defaultMessage)
        throw new AssertionError(message)
    }

    /**
     * Ensures that a collection contains one or more elements.
     * @return The collection if the collection is not empty.
     * @throws AssertionError if the collection does not contain any elements.
     */
    static <T> Collection<T> notEmpty(final Collection<T> collection, String customErrorMessage = null) throws AssertionError {
        assert !collection.isEmpty(): (customErrorMessage ?: "Collection contains 0 elements. Expected 1 or more")
        return collection
    }

    /**
     * Returns whether two collections contain the same set of elements. Each collection must not contain any two
     * elements which are equal.
     */
    static <T> boolean containSame(final Collection<? extends T> c1, final Collection<? extends T> c2) {
        final c1Set = c1.toSet()
        assert c1Set.size() == c1.size(): "c1 contains elements which are equal."
        final c2Set = c2.toSet()
        assert c2Set.size() == c2.size(): "c2 contains elements which are equal."
        return c1Set == c2Set
    }

    /**
     * Returns the value for the specified key of the given map.
     * In case the map does not contain specified key, it gets defined with the given default value.
     * @param d default value
     */
    static <K, V> V getOrPut(Map<K, V> map, K key, V d) {
        V value = map.get(key)
        if (value != null || map.containsKey(key)) {
            return value
        }
        map.put(key, d)
        return d
    }
}
