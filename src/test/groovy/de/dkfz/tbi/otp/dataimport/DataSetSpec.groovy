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
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

class DataSetSpec extends Specification implements DataTest, DataImportDomainFactory {

    /** schema with a key of type MD5SUM, but without a pattern, so that only the check of the key type applies */
    private static final String MD5SUM_SCHEMA = '''{"type": "object", "properties": {
        "project": {"otpKeyType": "PROJECT"}, "file": {"otpKeyType": "FILE"}, "md5": {"otpKeyType": "MD5SUM"}}, "required": ["project"]}'''

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataSet,
                DataType,
                DataTypeVersion,
                Key,
                ProcessingPriority,
                Project,
                Ticket,
        ]
    }

    void "validate, when data matches the schema, then it is valid"() {
        expect:
        createDataSet([:], false).validate()
    }

    @Unroll
    void "validate, when #property is null, then it is invalid"() {
        expect:
        TestCase.assertAtLeastExpectedValidateError(createDataSet([(property): null], false), property, 'nullable', null)

        where:
        property << ['ticket', 'project', 'dataTypeVersion', 'rawJsonData']
    }

    @Unroll
    void "validate, when data is #name, then it is invalid with code #code"() {
        given:
        DataSet dataSet = createDataSet([rawJsonData: data], false)

        expect:
        TestCase.assertAtLeastExpectedValidateError(dataSet, 'rawJsonData', code, data)

        where:
        name                     | data                                                || code
        'no json'                | '{"project": '                                      || DataImportSchemaValidator.INVALID_JSON
        'no object'              | '["project", "/file"]'                              || DataImportSchemaValidator.NO_OBJECT
        'missing a required key' | '{"project": "project"}'                            || DataImportSchemaValidator.NOT_MATCHING_SCHEMA
        'of a wrong type'        | '{"project": "project", "file": 5}'                 || DataImportSchemaValidator.NOT_MATCHING_SCHEMA
        'violating a pattern'    | '{"project": "p", "file": "/f", "md5": "invalid"}'  || DataImportSchemaValidator.NOT_MATCHING_SCHEMA
        'with a duplicate key'   | '{"project": "p", "file": "../f", "file": "/f"}'    || DataImportSchemaValidator.INVALID_JSON
    }

    @Unroll
    void "validate, when the data type version is #name and data is '#data', then only the JSON object is checked"() {
        given:
        DataSet dataSet = createDataSet([dataTypeVersion: dataTypeVersion(), rawJsonData: data], false)

        when:
        dataSet.validate()

        then:
        dataSet.errors.getFieldError('rawJsonData')?.code == code

        where:
        name                | dataTypeVersion                                                       | data          || code
        'missing'           | { null }                                                              | '{"a": '      || DataImportSchemaValidator.INVALID_JSON
        'missing'           | { null }                                                              | '[1]'         || DataImportSchemaValidator.NO_OBJECT
        'missing'           | { null }                                                              | '{"a": 1}'    || null
        'of invalid schema' | { createDataTypeVersion([rawJsonSchema: '{"type": "array"}'], false) } | '{"a": '      || DataImportSchemaValidator.INVALID_JSON
        'of invalid schema' | { createDataTypeVersion([rawJsonSchema: '{"type": "array"}'], false) } | '[1]'         || DataImportSchemaValidator.NO_OBJECT
        'of invalid schema' | { createDataTypeVersion([rawJsonSchema: '{"type": "array"}'], false) } | '{"a": 1}'    || null
    }

    void "validate, when data does not match the schema at several locations, then there is one error for each location"() {
        given:
        String data = '{"project": 1, "file": "/f", "md5": "invalid"}'
        DataSet dataSet = createDataSet([rawJsonData: data], false)

        when:
        dataSet.validate()

        then:
        List<FieldError> errors = dataSet.errors.getFieldErrors('rawJsonData')
        errors*.code.unique() == [DataImportSchemaValidator.NOT_MATCHING_SCHEMA]
        errors*.arguments*.getAt(0).toSet() == ['#/project', '#/md5'] as Set
        errors*.codes*.getAt(0).unique() == ["dataSet.rawJsonData.${DataImportSchemaValidator.NOT_MATCHING_SCHEMA}".toString()]
    }

    void "validate, when several values are not absolute paths, then there is one error for each key"() {
        given:
        DataTypeVersion dataTypeVersion = createDataTypeVersion(
                rawJsonSchema: '''{"type": "object", "properties": {
                    "project": {"otpKeyType": "PROJECT"}, "file": {"otpKeyType": "FILE"}, "dir": {"otpKeyType": "DIRECTORY"}}, "required": ["project"]}''',
        )
        DataSet dataSet = createDataSet([dataTypeVersion: dataTypeVersion, rawJsonData: '{"project": "p", "file": "relative", "dir": "/a/../b"}'], false)

        when:
        dataSet.validate()

        then:
        List<FieldError> errors = dataSet.errors.getFieldErrors('rawJsonData')
        errors*.code == [DataSetValidator.NO_ABSOLUTE_PATH, DataSetValidator.NO_ABSOLUTE_PATH]
        errors*.arguments*.getAt(0) == ['#/dir', '#/file']
    }

    void "validate, when the data type version has no keys, then it fails at the keys and not with an exception"() {
        given:
        DataSet dataSet = createDataSet([dataTypeVersion: createDataTypeVersion([keys: null], false)], false)

        when:
        dataSet.validate()

        then:
        dataSet.errors.getFieldError('rawJsonData') == null
    }

    void "validate, when data contains additional keys, then it is valid"() {
        expect:
        createDataSet([rawJsonData: '{"project": "p", "file": "/path/file", "additional": 1}'], false).validate()
    }

    @Unroll
    void "validate, when the value of a key of type FILE or DIRECTORY is '#path', then it is invalid"() {
        given:
        DataType dataType = createDataType()
        DataTypeVersion dataTypeVersion = createDataTypeVersion(
                dataType: dataType,
                rawJsonSchema: '''{"type": "object", "properties": {
                    "project": {"otpKeyType": "PROJECT"}, "file": {"otpKeyType": "FILE"}, "dir": {"otpKeyType": "DIRECTORY"}}, "required": ["project"]}''',
        )
        String data = """{"project": "p", "file": "/valid/file", "dir": ${path}}"""

        expect:
        TestCase.assertValidateError(createDataSet([dataTypeVersion: dataTypeVersion, rawJsonData: data], false), 'rawJsonData', DataSetValidator.NO_ABSOLUTE_PATH, data)

        where:
        path << ['"relative/dir"', '"/with/../dots"', '"/with/./dot"', '"/ends/with/.."', '"/ends/with/."', '"/.."', '""', '5', '["/a"]']
    }

    @Unroll
    void "validate, when the value of a key of type FILE or DIRECTORY is #name, then it is valid"() {
        given:
        DataTypeVersion dataTypeVersion = createDataTypeVersion(
                rawJsonSchema: '''{"type": "object", "properties": {
                    "project": {"otpKeyType": "PROJECT"}, "file": {"otpKeyType": "FILE"}, "dir": {"otpKeyType": "DIRECTORY"}}, "required": ["project"]}''',
        )

        expect:
        createDataSet([dataTypeVersion: dataTypeVersion, rawJsonData: """{"project": "p", "file": "/valid/file"${dir}}"""], false).validate()

        where:
        name             | dir
        'missing'        | ''
        'null'           | ', "dir": null'
        'absolute path'  | ', "dir": "/valid/dir"'
        'dots in names'  | ', "dir": "/.hidden/..dir/a..b/file.txt."'
    }

    @Unroll
    void "validate, when the value of a key of type MD5SUM is '#md5', then it is invalid"() {
        given:
        DataTypeVersion dataTypeVersion = createDataTypeVersion(rawJsonSchema: MD5SUM_SCHEMA)
        String data = """{"project": "p", "file": "/valid/file", "md5": ${md5}}"""

        expect:
        TestCase.assertValidateError(createDataSet([dataTypeVersion: dataTypeVersion, rawJsonData: data], false), 'rawJsonData', DataSetValidator.INVALID_MD5SUM, data)

        where:
        md5 << ['"0123456789abcdef"', '"0123456789ABCDEF0123456789ABCDEF"', '"0123456789abcdef0123456789abcdeg"', '""', '5', '["0123456789abcdef0123456789abcdef"]']
    }

    @Unroll
    void "validate, when the value of a key of type MD5SUM is #name, then it is valid"() {
        given:
        DataTypeVersion dataTypeVersion = createDataTypeVersion(rawJsonSchema: MD5SUM_SCHEMA)

        expect:
        createDataSet([dataTypeVersion: dataTypeVersion, rawJsonData: """{"project": "p", "file": "/valid/file"${md5}}"""], false).validate()

        where:
        name           | md5
        'missing'      | ''
        'null'         | ', "md5": null'
        'valid md5sum' | ', "md5": "0123456789abcdef0123456789abcdef"'
    }

    void "validate, when a path and a md5sum are invalid, then there is one error for each"() {
        given:
        DataTypeVersion dataTypeVersion = createDataTypeVersion(rawJsonSchema: MD5SUM_SCHEMA)
        DataSet dataSet = createDataSet([dataTypeVersion: dataTypeVersion, rawJsonData: '{"project": "p", "file": "relative", "md5": "invalid"}'], false)

        when:
        dataSet.validate()

        then:
        List<FieldError> errors = dataSet.errors.getFieldErrors('rawJsonData')
        errors*.code == [DataSetValidator.NO_ABSOLUTE_PATH, DataSetValidator.INVALID_MD5SUM]
        errors*.arguments*.getAt(0) == ['#/file', '#/md5']
    }
}
