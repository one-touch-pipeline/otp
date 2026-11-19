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

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

@Transactional
class DataTypeVersionService {

    static final String KEY_TYPE_CHANGED = "dataTypeVersion.keys.${DataImportSchemaValidator.CODE_PREFIX}key.type.changed"

    /**
     * Creates a new version of the data type with the given row schema.
     *
     * The keys are taken from the properties of the schema: existing keys of the data type are reused, missing keys are created.
     * The key type of an existing key can not be changed.
     *
     * @throws ValidationException if the schema or the resulting keys are invalid; then nothing is saved
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    DataTypeVersion createDataTypeVersion(DataType dataType, int version, String rawJsonSchema) {
        DataTypeVersion dataTypeVersion = new DataTypeVersion(
                dataType: dataType,
                dataTypeVersion: version,
                rawJsonSchema: rawJsonSchema,
                keys: [] as Set<Key>,
        )
        if (DataImportSchemaValidator.isValidRowSchema(rawJsonSchema)) {
            Map<String, Key> existingKeys = findKeys(dataType).collectEntries { Key key -> [(key.name): key] } as Map<String, Key>
            Map<String, KeyType> keyTypes = DataImportSchemaValidator.extractKeyTypes(rawJsonSchema)

            List<String> changedKeyTypes = keyTypes.findAll { String name, KeyType keyType ->
                existingKeys[name] && existingKeys[name].keyType != keyType
            }.keySet().sort()
            if (changedKeyTypes) {
                dataTypeVersion.errors.rejectValue('keys', KEY_TYPE_CHANGED, [changedKeyTypes.join(", ")] as Object[],
                        "The key type of the existing keys ${changedKeyTypes} can not be changed")
                throw new ValidationException("Validation error occurred", dataTypeVersion.errors)
            }

            dataTypeVersion.keys = keyTypes.collect { String name, KeyType keyType ->
                existingKeys[name] ?: new Key(name: name, keyType: keyType, dataType: dataType)
            } as Set<Key>
        }
        return dataTypeVersion.save(flush: true)
    }

    @CompileDynamic
    private List<Key> findKeys(DataType dataType) {
        return Key.findAllByDataType(dataType)
    }
}
