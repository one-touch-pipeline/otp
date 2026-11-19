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
package de.dkfz.tbi.otp.domainFactory.dataimport

import de.dkfz.tbi.otp.dataimport.DataImportSchemaValidator
import de.dkfz.tbi.otp.dataimport.DataSet
import de.dkfz.tbi.otp.dataimport.DataType
import de.dkfz.tbi.otp.dataimport.DataTypeVersion
import de.dkfz.tbi.otp.dataimport.Key
import de.dkfz.tbi.otp.dataimport.KeyType
import de.dkfz.tbi.otp.dataimport.RegisterDataSet
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project

trait DataImportDomainFactory implements DomainFactoryCore {

    static final String ROW_SCHEMA = '''
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "project":  { "type": "string", "otpKeyType": "PROJECT" },
            "file":     { "type": "string", "otpKeyType": "FILE" },
            "md5":      { "type": "string", "otpKeyType": "MD5SUM", "pattern": "^[0-9a-f]{32}$" },
            "sampleId": { "type": "string", "otpKeyType": "DATA" }
          },
          "required": ["project", "file"]
        }
        '''

    DataType createDataType(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DataType, [
                name: "dataType_${nextId}",
        ], properties, saveAndValidate)
    }

    Key createKey(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Key, [
                name    : "key_${nextId}",
                keyType : KeyType.DATA,
                dataType: { createDataType() },
        ], properties, saveAndValidate)
    }

    /**
     * Creates a version with the given or the default schema. If no keys are given, they are created from the schema.
     */
    DataTypeVersion createDataTypeVersion(Map properties = [:], boolean saveAndValidate = true) {
        DataType dataType = properties.dataType as DataType ?: createDataType()
        String schema = properties.containsKey('rawJsonSchema') ? properties.rawJsonSchema : ROW_SCHEMA
        return createDomainObject(DataTypeVersion, [
                dataType       : dataType,
                dataTypeVersion: nextId,
                rawJsonSchema  : schema,
                keys           : {
                    DataImportSchemaValidator.isValidRowSchema(schema) ?
                            DataImportSchemaValidator.extractKeyTypes(schema).collect { String name, KeyType keyType ->
                                createKey(name: name, keyType: keyType, dataType: dataType)
                            } as Set<Key> : [] as Set<Key>
                },
        ], properties, saveAndValidate)
    }

    DataSet createDataSet(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DataSet, [
                ticket         : { createTicket() },
                project        : { createProject() },
                dataTypeVersion: { createDataTypeVersion() },
                rawJsonData    : { '{"project": "project", "file": "/path/to/file.txt", "md5": "0123456789abcdef0123456789abcdef"}' },
        ], properties, saveAndValidate)
    }

    RegisterDataSet createRegisterDataSet(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(RegisterDataSet, [
                ticket         : { createTicket() },
                dataTypeVersion: { createDataTypeVersion() },
                priority       : { createProcessingPriority() },
                projects       : { [createProject()] as Set<Project> },
                rawJsonData    : { '{"rows": [{"project": "project", "file": "/path/to/file.txt"}]}' },
        ], properties, saveAndValidate)
    }
}
