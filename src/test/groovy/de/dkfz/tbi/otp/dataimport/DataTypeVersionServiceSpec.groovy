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
import grails.testing.services.ServiceUnitTest
import grails.validation.ValidationException
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.dataimport.DataImportDomainFactory

class DataTypeVersionServiceSpec extends Specification implements ServiceUnitTest<DataTypeVersionService>, DataTest, DataImportDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataType,
                DataTypeVersion,
                Key,
        ]
    }

    private static String schema(Map<String, KeyType> keys) {
        String properties = keys.collect { String name, KeyType keyType ->
            """"${name}": {"type": "string", "otpKeyType": "${keyType}"}"""
        }.join(", ")
        String required = keys.findAll { it.value == KeyType.PROJECT }.keySet().collect { "\"${it}\"" }.join(", ")
        return """{"type": "object", "properties": {${properties}}, "required": [${required}]}"""
    }

    void "createDataTypeVersion, when schema is valid, then save the version with a key for each property"() {
        given:
        DataType dataType = createDataType()

        when:
        DataTypeVersion dataTypeVersion = service.createDataTypeVersion(dataType, 1, ROW_SCHEMA)

        then:
        dataTypeVersion.id
        dataTypeVersion.dataType == dataType
        dataTypeVersion.dataTypeVersion == 1
        dataTypeVersion.rawJsonSchema == ROW_SCHEMA.trim() // data binding trims strings
        dataTypeVersion.keys.every { it.id && it.dataType == dataType }
        dataTypeVersion.keys.collectEntries { [(it.name): it.keyType] } == [
                project : KeyType.PROJECT,
                file    : KeyType.FILE,
                md5     : KeyType.MD5SUM,
                sampleId: KeyType.DATA,
        ]
        Key.count() == 4
    }

    void "createDataTypeVersion, when keys exist already for the data type, then reuse them and create only the missing keys"() {
        given:
        DataType dataType = createDataType()
        Key project = createKey(name: 'project', keyType: KeyType.PROJECT, dataType: dataType)
        Key file = createKey(name: 'file', keyType: KeyType.FILE, dataType: dataType)
        Key unused = createKey(name: 'unused', keyType: KeyType.DATA, dataType: dataType)

        when:
        DataTypeVersion dataTypeVersion = service.createDataTypeVersion(dataType, 2,
                schema(project: KeyType.PROJECT, file: KeyType.FILE, newKey: KeyType.DATA))

        then:
        dataTypeVersion.keys.size() == 3
        dataTypeVersion.keys.contains(project)
        dataTypeVersion.keys.contains(file)
        !dataTypeVersion.keys.contains(unused)
        Key newKey = dataTypeVersion.keys.find { it.name == 'newKey' }
        newKey.id
        newKey.keyType == KeyType.DATA
        Key.count() == 4
    }

    void "createDataTypeVersion, when a key with the same name exists for another data type, then create a new key"() {
        given:
        DataType dataType = createDataType()
        Key otherProject = createKey(name: 'project', keyType: KeyType.DATA)

        when:
        DataTypeVersion dataTypeVersion = service.createDataTypeVersion(dataType, 1, schema(project: KeyType.PROJECT, file: KeyType.FILE))

        then:
        Key project = dataTypeVersion.keys.find { it.name == 'project' }
        project != otherProject
        project.keyType == KeyType.PROJECT
        project.dataType == dataType
    }

    void "createDataTypeVersion, when the key type of an existing key changes, then throw an error and save nothing"() {
        given:
        DataType dataType = createDataType()
        Key file = createKey(name: 'file', keyType: KeyType.FILE, dataType: dataType)

        when:
        service.createDataTypeVersion(dataType, 2, schema(project: KeyType.PROJECT, file: KeyType.DIRECTORY))

        then:
        ValidationException e = thrown()
        e.errors.getFieldError('keys').code == DataTypeVersionService.KEY_TYPE_CHANGED
        e.errors.getFieldError('keys').arguments == ['file'] as Object[]
        file.refresh().keyType == KeyType.FILE
        Key.count() == 1
        DataTypeVersion.count() == 0
    }

    void "createDataTypeVersion, when schema is invalid, then throw an error and save nothing"() {
        given:
        DataType dataType = createDataType()

        when:
        service.createDataTypeVersion(dataType, 1, '{"type": "object"}')

        then:
        ValidationException e = thrown()
        e.errors.getFieldError('rawJsonSchema').code == DataImportSchemaValidator.NO_PROPERTIES
        Key.count() == 0
        DataTypeVersion.count() == 0
    }

    void "createDataTypeVersion, when the keys violate the rules, then throw an error and save nothing"() {
        given:
        DataType dataType = createDataType()

        when:
        service.createDataTypeVersion(dataType, 1, schema(file: KeyType.FILE, other: KeyType.DATA))

        then:
        ValidationException e = thrown()
        e.errors.getFieldError('keys').code == DataTypeVersionValidator.NOT_EXACTLY_ONE_PROJECT
        Key.count() == 0
        DataTypeVersion.count() == 0
    }

    void "createDataTypeVersion, when the version exists already, then throw an error and save nothing"() {
        given:
        DataTypeVersion existing = createDataTypeVersion()
        int keyCount = Key.count()

        when:
        service.createDataTypeVersion(existing.dataType, existing.dataTypeVersion, schema(project: KeyType.PROJECT, newFile: KeyType.FILE))

        then:
        ValidationException e = thrown()
        e.errors.getFieldError('dataTypeVersion').code == 'unique'
        Key.count() == keyCount
        DataTypeVersion.count() == 1
    }
}
