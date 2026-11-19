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

/**
 * Rules for the data of a {@link DataSet}, used by its constraints.
 */
class DataSetValidator {

    static final String NO_ABSOLUTE_PATH = "${DataImportSchemaValidator.CODE_PREFIX}no.absolute.path"
    static final String INVALID_MD5SUM = "${DataImportSchemaValidator.CODE_PREFIX}invalid.md5sum"

    /**
     * Checks the data against the row schema, verifying that the values of the keys of type FILE and DIRECTORY are absolute paths
     * without the path components "." and "..", and that the values of the keys of type MD5SUM are valid md5sums.
     * If the data type version or its schema are missing or invalid, only the check for a JSON object will be done.
     *
     * @return one error for each location with an error, or an empty list if the data are valid
     */
    static List<JsonValidationError> validateRawJsonData(String rawJsonData, DataTypeVersion dataTypeVersion) {
        if (!dataTypeVersion || !DataImportSchemaValidator.isValidRowSchema(dataTypeVersion.rawJsonSchema)) {
            return DataImportSchemaValidator.validateJsonObject(rawJsonData)
        }
        List<JsonValidationError> errors = DataImportSchemaValidator.validateRow(rawJsonData, dataTypeVersion.rawJsonSchema)
        if (errors) {
            return errors
        }
        Collection<Key> keys = dataTypeVersion.keys ?: []
        List<String> pathKeys = keys.findAll { it.keyType in [KeyType.FILE, KeyType.DIRECTORY] }*.name
        List<String> md5sumKeys = keys.findAll { it.keyType == KeyType.MD5SUM }*.name
        return DataImportSchemaValidator.findKeysWithoutAbsolutePath(rawJsonData, pathKeys).collect { String keyName ->
            new JsonValidationError(NO_ABSOLUTE_PATH, DataImportSchemaValidator.location(keyName), null)
        } + DataImportSchemaValidator.findKeysWithInvalidMd5sum(rawJsonData, md5sumKeys).collect { String keyName ->
            new JsonValidationError(INVALID_MD5SUM, DataImportSchemaValidator.location(keyName), null)
        }
    }
}
