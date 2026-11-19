/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.dataimport

import grails.gorm.hibernate.annotation.ManagedEntity
import groovy.transform.ToString

import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.validation.ImmutablePropertyValidator

/**
 * A key of a {@link DataType}, defined by a property of the row schema of a {@link DataTypeVersion}.
 * The name is unique within a data type; different data types can have keys with the same name.
 *
 * All properties are fixed after the creation, since a key is shared by the versions of the data type.
 */
@ManagedEntity
@ToString(includeNames = true, includePackage = false)
class Key implements Entity {

    String name
    KeyType keyType
    DataType dataType

    static Closure constraints = {
        name blank: false, unique: 'dataType', validator: ImmutablePropertyValidator.validator('name')
        keyType validator: ImmutablePropertyValidator.validator('keyType')
        dataType validator: ImmutablePropertyValidator.validator('dataType')
    }

    static Closure mapping = {
        table 'data_type_key'
        name updatable: false
        keyType updatable: false
        dataType index: 'data_type_key_data_type_idx', updatable: false
    }
}
