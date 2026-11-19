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
 * Rules for the keys of a {@link DataTypeVersion}, used by its constraints.
 */
class DataTypeVersionValidator {

    static final String CODE_PREFIX = DataImportSchemaValidator.CODE_PREFIX
    static final String WRONG_DATA_TYPE = "${CODE_PREFIX}wrong.data.type"
    static final String KEYS_NOT_MATCHING_SCHEMA = "${CODE_PREFIX}keys.not.matching.schema"
    static final String NOT_EXACTLY_ONE_PROJECT = "${CODE_PREFIX}not.exactly.one.project"
    static final String NO_FILE_OR_DIRECTORY = "${CODE_PREFIX}no.file.or.directory"
    static final String MULTIPLE_MD5SUM = "${CODE_PREFIX}multiple.md5sum"
    static final String MD5SUM_WITHOUT_SINGLE_FILE = "${CODE_PREFIX}md5sum.without.single.file"
    static final String NULL_KEY = "${CODE_PREFIX}null.key"
    static final String DUPLICATE_KEY_NAMES = "${CODE_PREFIX}duplicate.key.names"

    /** orders the names of keys, which are not yet validated and therefore can be null */
    private static final Comparator<String> NULLS_FIRST = Comparator.nullsFirst(Comparator.<String> naturalOrder())

    /**
     * Checks the keys against the schema: they must not be null, have to belong to the data type, must have unique names
     * and have to match the properties of the schema with their key types. Further, exactly one key of type PROJECT, at least one of type FILE or DIRECTORY and at most one
     * of type MD5SUM, which requires exactly one of type FILE, are required.
     *
     * The checks are skipped for an invalid schema and true is returned: the invalid schema is already reported on the field
     * rawJsonSchema ({@link DataImportSchemaValidator#validateRowSchema}), and keys can not be checked against it, so a further
     * error on the keys would only be a misleading consequence of the invalid schema.
     *
     * @return true if valid or not checkable, otherwise the error code, or a list of the error code and its arguments,
     * as for a custom validator
     */
    static Object validateKeys(Set<Key> keys, DataType dataType, String rawJsonSchema) {
        if (!DataImportSchemaValidator.isValidRowSchema(rawJsonSchema)) {
            return true
        }
        Set<Key> nonNullKeys = keys ?: [] as Set<Key>
        if (nonNullKeys.contains(null)) {
            return NULL_KEY
        }
        List<String> wrongDataType = nonNullKeys.findAll { it.dataType != dataType }.collect {
            "${it.name} (${it.dataType?.name})".toString()
        }
        if (wrongDataType) {
            return [WRONG_DATA_TYPE, wrongDataType.sort().join(", ")]
        }
        List<String> duplicateNames = nonNullKeys.countBy { it.name }.findAll { it.value > 1 }.keySet().toList()
        if (duplicateNames) {
            return [DUPLICATE_KEY_NAMES, duplicateNames.toSorted(NULLS_FIRST).join(", ")]
        }
        Map<String, KeyType> keyTypesOfKeys = nonNullKeys.collectEntries { [(it.name): it.keyType] }
        Map<String, KeyType> keyTypesOfSchema = DataImportSchemaValidator.extractKeyTypes(rawJsonSchema)
        if (keyTypesOfKeys != keyTypesOfSchema) {
            return [KEYS_NOT_MATCHING_SCHEMA, keyTypesOfKeys.sort(NULLS_FIRST).toString(), keyTypesOfSchema.sort().toString()]
        }
        Map<KeyType, Integer> count = nonNullKeys.countBy { it.keyType }
        if (count[KeyType.PROJECT] != 1) {
            return NOT_EXACTLY_ONE_PROJECT
        }
        if (!count[KeyType.FILE] && !count[KeyType.DIRECTORY]) {
            return NO_FILE_OR_DIRECTORY
        }
        if ((count[KeyType.MD5SUM] ?: 0) > 1) {
            return MULTIPLE_MD5SUM
        }
        if (count[KeyType.MD5SUM] && count[KeyType.FILE] != 1) {
            return MD5SUM_WITHOUT_SINGLE_FILE
        }
        return true
    }
}
