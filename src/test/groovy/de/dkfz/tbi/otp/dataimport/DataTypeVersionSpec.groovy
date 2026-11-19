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

import grails.testing.gorm.DataTest
import org.springframework.validation.FieldError
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.dataimport.DataImportDomainFactory

class DataTypeVersionSpec extends Specification implements DataTest, DataImportDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataType,
                DataTypeVersion,
                Key,
        ]
    }

    /**
     * @param keys map of name to key type
     * @return a schema with one property of each key and a version with matching keys
     */
    private DataTypeVersion createVersionWithKeys(Map<String, KeyType> keys) {
        DataType dataType = createDataType()
        String properties = keys.collect { String name, KeyType keyType ->
            """"${name}": {"type": "string", "otpKeyType": "${keyType}"}"""
        }.join(", ")
        String required = keys.findAll { it.value == KeyType.PROJECT }.keySet().collect { "\"${it}\"" }.join(", ")
        return createDataTypeVersion([
                dataType     : dataType,
                rawJsonSchema: """{"type": "object", "properties": {${properties}}, "required": [${required}]}""",
                keys         : keys.collect { String name, KeyType keyType ->
                    createKey(name: name, keyType: keyType, dataType: dataType)
                } as Set<Key>,
        ], false)
    }

    void "validate, when version is created from the default schema, then it is valid"() {
        expect:
        createDataTypeVersion([:], false).validate()
    }

    void "validate, when dataType, dataTypeVersion and schema already exist, then it is invalid"() {
        given:
        DataTypeVersion existing = createDataTypeVersion()

        expect:
        TestCase.assertValidateError(createDataTypeVersion([
                dataType       : existing.dataType,
                dataTypeVersion: existing.dataTypeVersion,
                keys           : new HashSet<Key>(existing.keys),
        ], false), 'dataTypeVersion', 'unique', existing.dataTypeVersion)
    }

    void "validate, when the same version number is used for another data type, then it is valid"() {
        given:
        DataTypeVersion existing = createDataTypeVersion()

        expect:
        createDataTypeVersion([dataTypeVersion: existing.dataTypeVersion], false).validate()
    }

    @Unroll
    void "validate, when dataTypeVersion is #version, then it is invalid"() {
        expect:
        TestCase.assertValidateError(createDataTypeVersion([dataTypeVersion: version], false), 'dataTypeVersion', 'min.notmet', version)

        where:
        version << [0, -1]
    }

    void "validate, when dataTypeVersion is 1, then it is valid"() {
        expect:
        createDataTypeVersion([dataTypeVersion: 1], false).validate()
    }

    @Unroll
    void "validate, when schema is invalid (#name), then it is invalid with code #code"() {
        given:
        DataTypeVersion dataTypeVersion = createDataTypeVersion([rawJsonSchema: schema, keys: [] as Set<Key>], false)

        expect:
        TestCase.assertAtLeastExpectedValidateError(dataTypeVersion, 'rawJsonSchema', code, schema)
        !dataTypeVersion.errors.getFieldError('keys')

        where:
        name               | schema                                                                         || code
        'null'             | null                                                                           || 'nullable'
        'no json'          | '{'                                                                            || DataImportSchemaValidator.INVALID_JSON
        'meta-schema'      | '{"type": "notAType", "properties": {"p": {"otpKeyType": "PROJECT"}}}'         || DataImportSchemaValidator.INVALID_SCHEMA
        'remote reference' | '{"type": "object", "properties": {"p": {"$ref": "https://otp.invalid/x"}}}'   || DataImportSchemaValidator.INVALID_SCHEMA
        'no object'        | '{"type": "string"}'                                                           || DataImportSchemaValidator.NO_OBJECT_SCHEMA
        'no properties'    | '{"type": "object"}'                                                           || DataImportSchemaValidator.NO_PROPERTIES
        'no key type'      | '{"type": "object", "properties": {"p": {"type": "string"}}}'                  || DataImportSchemaValidator.INVALID_KEY_TYPE
        'not required'     | '{"type": "object", "properties": {"p": {"otpKeyType": "PROJECT"}}}'           || DataImportSchemaValidator.PROJECT_NOT_REQUIRED
    }

    void "validate, when a key belongs to another data type, then it is invalid"() {
        given:
        DataTypeVersion dataTypeVersion = createDataTypeVersion([:], false)
        Key project = dataTypeVersion.keys.find { it.name == 'project' }
        dataTypeVersion.keys.remove(project)
        Key otherProject = createKey(name: 'project', keyType: KeyType.PROJECT)
        dataTypeVersion.keys.add(otherProject)

        expect:
        TestCase.assertValidateError(dataTypeVersion, 'keys', DataTypeVersionValidator.WRONG_DATA_TYPE, dataTypeVersion.keys)
        dataTypeVersion.errors.getFieldError('keys').arguments.last() == "project (${otherProject.dataType.name})".toString()
    }

    void "validate, when the schema is invalid at several locations, then there is one error for each location"() {
        given:
        String schema = '{"type": "object", "properties": {"p": {"otpKeyType": "PROJECT"}, "x": {"otpKeyType": "UNKNOWN"}}}'
        DataTypeVersion dataTypeVersion = createDataTypeVersion([rawJsonSchema: schema, keys: [] as Set<Key>], false)

        when:
        dataTypeVersion.validate()

        then:
        List<FieldError> errors = dataTypeVersion.errors.getFieldErrors('rawJsonSchema')
        errors*.code == [DataImportSchemaValidator.PROJECT_NOT_REQUIRED, DataImportSchemaValidator.INVALID_KEY_TYPE]
        errors*.arguments*.getAt(0) == ['#/properties/p', '#/properties/x/otpKeyType']
        errors*.codes*.getAt(0) == [
                "dataTypeVersion.rawJsonSchema.${DataImportSchemaValidator.PROJECT_NOT_REQUIRED}".toString(),
                "dataTypeVersion.rawJsonSchema.${DataImportSchemaValidator.INVALID_KEY_TYPE}".toString(),
        ]
        errors*.rejectedValue.unique() == [schema]
    }

    @Unroll
    void "validate, when keys do not match the schema (#name), then it is invalid"() {
        given:
        DataTypeVersion dataTypeVersion = createDataTypeVersion([:], false)
        change(dataTypeVersion)

        expect:
        TestCase.assertValidateError(dataTypeVersion, 'keys', DataTypeVersionValidator.KEYS_NOT_MATCHING_SCHEMA, dataTypeVersion.keys)

        where:
        name                | change
        'key missing'       | { DataTypeVersion v -> v.keys.removeAll { it.name == 'sampleId' } }
        'additional key'    | { DataTypeVersion v -> v.keys.add(createKey(name: 'other', dataType: v.dataType)) }
        'other key type'    | { DataTypeVersion v -> v.keys.find { it.name == 'sampleId' }.keyType = KeyType.DIRECTORY }
        'no keys'           | { DataTypeVersion v -> v.keys = null }
        'key without name'  | { DataTypeVersion v -> v.keys.add(createKey([name: null, dataType: v.dataType], false)) }
    }

    void "validate, when keys contain null, then it is invalid"() {
        given:
        DataTypeVersion dataTypeVersion = createDataTypeVersion([:], false)
        dataTypeVersion.keys.add(null)

        expect:
        TestCase.assertValidateError(dataTypeVersion, 'keys', DataTypeVersionValidator.NULL_KEY, dataTypeVersion.keys)
    }

    @Unroll
    void "validate, when two keys have the name 'file' with the key types FILE and #otherKeyType, then it is invalid"() {
        given:
        DataTypeVersion dataTypeVersion = createVersionWithKeys([project: KeyType.PROJECT, file: KeyType.FILE])
        dataTypeVersion.keys.add(createKey([name: 'file', keyType: otherKeyType, dataType: dataTypeVersion.dataType], false))

        expect:
        TestCase.assertValidateError(dataTypeVersion, 'keys', DataTypeVersionValidator.DUPLICATE_KEY_NAMES, dataTypeVersion.keys)
        dataTypeVersion.errors.getFieldError('keys').arguments.last() == 'file'

        where:
        otherKeyType << [KeyType.FILE, KeyType.DATA]
    }

    @Unroll
    void "validate, when keys are #keys, then it is invalid with code #code"() {
        given:
        DataTypeVersion dataTypeVersion = createVersionWithKeys(keys)

        expect:
        TestCase.assertValidateError(dataTypeVersion, 'keys', code, dataTypeVersion.keys)

        where:
        keys                                                                                     || code
        [file: KeyType.FILE]                                                                     || DataTypeVersionValidator.NOT_EXACTLY_ONE_PROJECT
        [p1: KeyType.PROJECT, p2: KeyType.PROJECT, file: KeyType.FILE]                           || DataTypeVersionValidator.NOT_EXACTLY_ONE_PROJECT
        [p: KeyType.PROJECT, d: KeyType.DATA]                                                    || DataTypeVersionValidator.NO_FILE_OR_DIRECTORY
        [p: KeyType.PROJECT, dir: KeyType.DIRECTORY, md5: KeyType.MD5SUM]                        || DataTypeVersionValidator.MD5SUM_WITHOUT_SINGLE_FILE
        [p: KeyType.PROJECT, f1: KeyType.FILE, f2: KeyType.FILE, md5: KeyType.MD5SUM]            || DataTypeVersionValidator.MD5SUM_WITHOUT_SINGLE_FILE
        [p: KeyType.PROJECT, f: KeyType.FILE, md5a: KeyType.MD5SUM, md5b: KeyType.MD5SUM]        || DataTypeVersionValidator.MULTIPLE_MD5SUM
        [p: KeyType.PROJECT, dir: KeyType.DIRECTORY, md5a: KeyType.MD5SUM, md5b: KeyType.MD5SUM] || DataTypeVersionValidator.MULTIPLE_MD5SUM
    }

    @Unroll
    void "validate, when keys are #keys, then it is valid"() {
        expect:
        createVersionWithKeys(keys).validate()

        where:
        keys << [
                [p: KeyType.PROJECT, f: KeyType.FILE],
                [p: KeyType.PROJECT, dir: KeyType.DIRECTORY],
                [p: KeyType.PROJECT, f1: KeyType.FILE, f2: KeyType.FILE, dir: KeyType.DIRECTORY, d: KeyType.DATA],
                [p: KeyType.PROJECT, f: KeyType.FILE, dir: KeyType.DIRECTORY, md5: KeyType.MD5SUM],
        ]
    }
}
