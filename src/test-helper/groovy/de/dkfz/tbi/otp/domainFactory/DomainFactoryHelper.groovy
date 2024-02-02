/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.domainFactory

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.CollectionUtils

trait DomainFactoryHelper {

    static int getNextId() {
        return DomainFactory.counter++
    }

    def <T> T createDomainObject(Class<T> domainClass, Map defaultProperties, Map parameterProperties, boolean saveAndValidate = true) {
        return DomainFactory.createDomainObject(domainClass, defaultProperties, parameterProperties, saveAndValidate)
    }

    /**
     * @Deprecated please use {@link #findOrCreateDomainObject(Class, Map, Map, Map, boolean)} instead
     */
    @Deprecated
    def <T> T findOrCreateDomainObject(Class<T> domainClass, Map defaultProperties, Map parameterProperties, boolean saveAndValidate = true) {
        return DomainFactory.findOrCreateDomainObject(domainClass, defaultProperties, parameterProperties, saveAndValidate)
    }

    /**
     * Check first for an object and if not exist yet, create it.
     *
     * @param domainClass The domain to search for
     * @param requiredSearchProperties The properties used for searching
     * @param defaultCreationProperties The default properties needed to create an object
     * @param additionalCreationProperties Additional properties to use for creation
     * @param saveAndValidate flag to indicate, if the object should be validated and saved
     * @return the found or created object
     */
    def <T> T findOrCreateDomainObject(Class<T> domainClass, Map<String, ?> requiredSearchProperties, Map<String, ?> defaultCreationProperties,
                                       Map<String, ?> additionalCreationProperties, boolean saveAndValidate = true) {
        assert requiredSearchProperties: 'Required search properties are missing.'

        return CollectionUtils.atMostOneElement(domainClass.findAllWhere(requiredSearchProperties)) ?:
                createDomainObject(domainClass, defaultCreationProperties, requiredSearchProperties + additionalCreationProperties, saveAndValidate)
    }
}
