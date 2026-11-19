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
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.validation.ImmutablePropertyValidator

/**
 * A version of a {@link DataType}, defined by the JSON schema of one row of an import request.
 *
 * The {@link #keys} correspond to the properties of the {@link #rawJsonSchema}, each with the {@link KeyType} given by
 * {@link DataImportSchemaValidator#KEY_TYPE_KEYWORD}.
 *
 * All properties are fixed after the creation, since existing {@link DataSet}s were validated against them.
 */
@ManagedEntity
@ToString(includeNames = true, includePackage = false, excludes = ['rawJsonSchema'])
class DataTypeVersion implements Entity {

    DataType dataType

    /** the version of the schema */
    int dataTypeVersion

    /** the JSON schema of one row */
    String rawJsonSchema

    /** all keys defined in the schema, independent of whether they are required or optional */
    Set<Key> keys

    static hasMany = [
            keys: Key,
    ]

    static Closure constraints = {
        dataType validator: ImmutablePropertyValidator.validator('dataType')
        dataTypeVersion unique: 'dataType', min: 1, validator: ImmutablePropertyValidator.validator('dataTypeVersion')
        rawJsonSchema validator: { String val, DataTypeVersion obj, Errors errors ->
            if (ImmutablePropertyValidator.isChanged(obj, 'rawJsonSchema')) {
                JsonValidationError.rejectAll([new JsonValidationError(ImmutablePropertyValidator.CHANGED, DataImportSchemaValidator.ROOT, null)],
                        errors, 'dataTypeVersion', 'rawJsonSchema', val)
                return
            }
            JsonValidationError.rejectAll(DataImportSchemaValidator.validateRowSchema(val), errors, 'dataTypeVersion', 'rawJsonSchema', val)
        }
        keys validator: { Set<Key> val, DataTypeVersion version ->
            if (ImmutablePropertyValidator.isChanged(version, 'keys')) {
                return ImmutablePropertyValidator.CHANGED
            }
            return DataTypeVersionValidator.validateKeys(val, version.dataType, version.rawJsonSchema)
        }
    }

    /**
     * The changes of the fixed properties are rejected by the validation and, for the columns, also not written to the database.
     * Hibernate ignores "updatable: false" for the collection keys, therefore their changes are only rejected by the validation.
     */
    static Closure mapping = {
        rawJsonSchema type: 'text', updatable: false
        dataType index: 'data_type_version_data_type_idx', updatable: false
        dataTypeVersion updatable: false
    }
}
