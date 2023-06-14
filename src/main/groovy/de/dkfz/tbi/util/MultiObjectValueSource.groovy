/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.util

import groovy.transform.CompileDynamic

import java.time.LocalDate

/**
 * This class allows to access a property over multiple objects by name in a safe way.
 *
 * The constructor takes an array of objects to use as potential sources for the field.
 * You can then query all the objects for a property and get a single value or a list
 * of values with getByFieldName and getAllByFieldName respectively.
 *
 * Maps are treated in a special way, in which it does not consider the actual attributes
 * of the object but rather the keys of the Map.
 *
 * The objects are iterated in the order they were given in. Subsequently they have an
 * internal priority, with the first object having the highest.
 */
class MultiObjectValueSource {

    final private Collection<Object> objects

    MultiObjectValueSource(Object... objects) {
        this.objects = objects.findAll()
    }

    @CompileDynamic
    private static Object getPropertyByNameHelper(Object object, String fieldName) {
        if (object instanceof Map) {
            return object.getOrDefault(fieldName, null)
        }
        return object.hasProperty(fieldName) ? object."$fieldName" : null
    }

    /**
     * Iterates the objects and returns the first value it can find in the given field.
     *
     * This function ignores null values and treats them as non-existing, thus referring to
     * the next object in the array. If none of the objects provide a value for the field,
     * null is returned.
     *
     * @param fieldName the name of the field you want to get
     * @return the value of the referenced field or null
     */
    Object getByFieldName(String fieldName) {
        Object value = null
        for (object in objects) {
            value = getPropertyByNameHelper(object, fieldName)
            if (value != null) {
                break
            }
        }
        return value
    }

    /**
     * Iterates the objects and returns the value of the given property for each object as a list.
     *
     * @param fieldName the name of the field you want to get
     * @return list of properties
     */
    List<Object> getAllByFieldName(String fieldName) {
        return objects.collect { Object object ->
            getPropertyByNameHelper(object, fieldName)
        }
    }

    LocalDate getFieldAsLocalDate(String fieldName) {
        Object value = getByFieldName(fieldName)
        switch (value?.class) {
            case String:
                return LocalDate.parse(value as String)
            case LocalDate:
                return value as LocalDate
            case null:
                return null
            default:
                throw new UnsupportedOperationException("${value.class} is not handled for conversion to LocalDate")
        }
    }
}
