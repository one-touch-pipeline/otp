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

import spock.lang.Specification
import spock.lang.Unroll

import static de.dkfz.tbi.otp.dataimport.DataImportSchemaValidator.ROOT

class DataImportSchemaValidatorSpec extends Specification {

    static final String VALID_SCHEMA = '''
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

    /** location of a syntax error, e.g. "line 2, column 5" */
    static final String TEXT_POSITION = /line \d+, column \d+/

    private static JsonValidationError error(String code, String location, String detail = null) {
        return new JsonValidationError(code, location, detail)
    }

    void "validateRowSchema, when schema is valid, then return no errors"() {
        expect:
        DataImportSchemaValidator.validateRowSchema(VALID_SCHEMA) == []
        DataImportSchemaValidator.isValidRowSchema(VALID_SCHEMA)
    }

    void "validateRowSchema, when schema declares no dialect, then it is validated as draft 2020-12 and returns no errors"() {
        expect:
        DataImportSchemaValidator.validateRowSchema('{"type": "object", "properties": {"p": {"otpKeyType": "PROJECT"}}, "required": ["p"]}') == []
    }

    void "validateRowSchema, when schema uses an internal reference, then return no errors"() {
        given:
        String schema = '''
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$defs": { "path": { "type": "string", "pattern": "^/" } },
              "type": "object",
              "properties": {
                "project": { "type": "string", "otpKeyType": "PROJECT" },
                "file":    { "$ref": "#/$defs/path", "otpKeyType": "FILE" }
              },
              "required": ["project"]
            }
            '''

        expect:
        DataImportSchemaValidator.validateRowSchema(schema) == []
    }

    @Unroll
    void "validateRowSchema, when schema is '#schema', then return one error invalid json with the position in the text"() {
        when:
        List<JsonValidationError> errors = DataImportSchemaValidator.validateRowSchema(schema)

        then:
        errors.size() == 1
        errors[0].code == DataImportSchemaValidator.INVALID_JSON
        errors[0].location ==~ TEXT_POSITION
        errors[0].detail
        !DataImportSchemaValidator.isValidRowSchema(schema)

        where:
        schema << [null, '', '{', '{"type": "object",}', 'no json']
    }

    void "validateRowSchema, when the JSON is invalid in a later line, then the location is the line and column of the error"() {
        given:
        String schema = '{\n  "type": "object",\n  "properties": {}}\n}'

        expect:
        DataImportSchemaValidator.validateRowSchema(schema)*.location == ["line 4, column 1"]
    }

    @Unroll
    void "validateRowSchema, when schema violates the meta-schema (#name), then return code invalid schema at #location"() {
        when:
        List<JsonValidationError> errors = DataImportSchemaValidator.validateRowSchema(schema)

        then:
        errors
        errors*.code.unique() == [DataImportSchemaValidator.INVALID_SCHEMA]
        errors*.location.unique() == [location]
        errors.every { it.detail }

        where:
        name                   | schema                                                                               || location
        'not an object'        | '5'                                                                                  || ROOT
        'invalid type'         | '{"type": "notAType", "properties": {"p": {"otpKeyType": "PROJECT"}}}'               || '#/type'
        'required not a list'  | '{"type": "object", "required": "p", "properties": {"p": {"otpKeyType": "PROJECT"}}}' || '#/required'
        'properties not a map' | '{"type": "object", "properties": []}'                                               || '#/properties'
    }

    void "validateRowSchema, when schema violates the meta-schema at several locations, then return one error for each, without duplicates"() {
        when:
        List<JsonValidationError> errors = DataImportSchemaValidator.validateRowSchema(
                '{"type": "notAType", "required": "p", "properties": {"p": {"otpKeyType": "PROJECT"}}}')

        then:
        errors*.location.toSet() == ['#/type', '#/required'] as Set
        errors.unique(false) == errors
    }

    void "validateRowSchema, when schema references a remote resource, then return code invalid schema without loading it"() {
        given:
        // The TLD 'invalid' is reserved and never resolves (RFC 2606). A network access would fail with an UnknownHostException,
        // while the FileNotFoundException shows that no loader for remote resources is configured.
        String schema = '''
            {
              "type": "object",
              "properties": {
                "project": { "$ref": "https://otp.invalid/schema.json", "otpKeyType": "PROJECT" }
              },
              "required": ["project"]
            }
            '''

        expect:
        // Compared exactly on purpose: the message shows that no remote loader was used. If a library update changes the behavior,
        // this test fails and the offline validation has to be checked again.
        DataImportSchemaValidator.validateRowSchema(schema) == [
                error(DataImportSchemaValidator.INVALID_SCHEMA, ROOT, "java.io.FileNotFoundException: https://otp.invalid/schema.json"),
        ]
    }

    @Unroll
    void "validateRowSchema, when schema declares the unsupported dialect #dialect, then return code invalid schema instead of throwing an error"() {
        given:
        String schema = """{"\$schema": "${dialect}", "type": "object", "properties": {"p": {"otpKeyType": "PROJECT"}}, "required": ["p"]}"""

        when:
        List<JsonValidationError> errors = DataImportSchemaValidator.validateRowSchema(schema)

        then:
        errors*.code == [DataImportSchemaValidator.INVALID_SCHEMA]
        errors[0].location == ROOT
        errors[0].detail

        where:
        dialect << [
                'http://json-schema.org/draft-07/schema#',
                'https://json-schema.org/draft/2019-09/schema',
                'https://otp.invalid/unknown-dialect',
        ]
    }

    @Unroll
    void "validateRowSchema, when root is not an object schema (#name), then return code no object schema"() {
        expect:
        DataImportSchemaValidator.validateRowSchema(schema) == [error(DataImportSchemaValidator.NO_OBJECT_SCHEMA, ROOT)]

        where:
        name          | schema
        'boolean'     | 'true'
        'no type'     | '{"properties": {"p": {"otpKeyType": "PROJECT"}}}'
        'type string' | '{"type": "string"}'
        'type list'   | '{"type": ["object", "null"], "properties": {"p": {"otpKeyType": "PROJECT"}}}'
    }

    @Unroll
    void "validateRowSchema, when properties are #name, then return code no properties"() {
        expect:
        DataImportSchemaValidator.validateRowSchema(schema) == [error(DataImportSchemaValidator.NO_PROPERTIES, '#/properties')]

        where:
        name      | schema
        'missing' | '{"type": "object"}'
        'empty'   | '{"type": "object", "properties": {}}'
    }

    @Unroll
    void "validateRowSchema, when otpKeyType is #name, then return code invalid key type at the property"() {
        given:
        String schema = """{"type": "object", "properties": {"ok": {"otpKeyType": "PROJECT"}, "bad": ${property}}, "required": ["ok"]}"""

        expect:
        DataImportSchemaValidator.validateRowSchema(schema) == [
                error(DataImportSchemaValidator.INVALID_KEY_TYPE, '#/properties/bad/otpKeyType', KeyType.values()*.name().join(", ")),
        ]

        where:
        name          | property
        'missing'     | '{"type": "string"}'
        'unknown'     | '{"otpKeyType": "UNKNOWN"}'
        'lower case'  | '{"otpKeyType": "file"}'
        'not a text'  | '{"otpKeyType": 1}'
        'null'        | '{"otpKeyType": null}'
        'bool schema' | 'true'
    }

    @Unroll
    void "validateRowSchema, when a property of type PROJECT is not required (#name), then return code project not required for each property"() {
        given:
        String schema = """{"type": "object", "properties": {
            "p1": {"otpKeyType": "PROJECT"}, "p2": {"otpKeyType": "PROJECT"}, "f": {"otpKeyType": "FILE"}}${required}}"""

        expect:
        DataImportSchemaValidator.validateRowSchema(schema) == locations.collect { error(DataImportSchemaValidator.PROJECT_NOT_REQUIRED, it) }

        where:
        name               | required                    || locations
        'no required'      | ''                          || ['#/properties/p1', '#/properties/p2']
        'empty required'   | ', "required": []'          || ['#/properties/p1', '#/properties/p2']
        'only other keys'  | ', "required": ["f"]'       || ['#/properties/p1', '#/properties/p2']
        'only one project' | ', "required": ["p1", "f"]' || ['#/properties/p2']
    }

    void "validateRowSchema, when the properties of type PROJECT are required, then return no errors"() {
        given:
        String schema = '''{"type": "object", "properties": {
            "p1": {"otpKeyType": "PROJECT"}, "p2": {"otpKeyType": "PROJECT"}, "f": {"otpKeyType": "FILE"}}, "required": ["p2", "p1"]}'''

        expect:
        // more than one key of type PROJECT is rejected by DataTypeVersionValidator.validateKeys, not by the schema validation
        DataImportSchemaValidator.validateRowSchema(schema) == []
    }

    void "validateRowSchema, when several properties are invalid, then return one error for each property"() {
        given:
        String schema = '''{"type": "object", "properties": {
            "p": {"otpKeyType": "PROJECT"}, "a/b": {"otpKeyType": "UNKNOWN"}, "f": {"otpKeyType": "FILE"}, "c": {}}}'''

        expect:
        DataImportSchemaValidator.validateRowSchema(schema) == [
                error(DataImportSchemaValidator.PROJECT_NOT_REQUIRED, '#/properties/p'),
                error(DataImportSchemaValidator.INVALID_KEY_TYPE, '#/properties/a~1b/otpKeyType', KeyType.values()*.name().join(", ")),
                error(DataImportSchemaValidator.INVALID_KEY_TYPE, '#/properties/c/otpKeyType', KeyType.values()*.name().join(", ")),
        ]
    }

    void "validateRowSchema, when schema contains a duplicate key, then return code invalid json at the duplicate"() {
        given:
        String schema = '''{"type": "object", "properties": {"p": {"otpKeyType": "PROJECT"}}, "required": ["p"],
            "properties": {"p": {"otpKeyType": "DATA"}}}'''

        when:
        List<JsonValidationError> errors = DataImportSchemaValidator.validateRowSchema(schema)

        then:
        errors*.code == [DataImportSchemaValidator.INVALID_JSON]
        errors[0].location.startsWith("line 2, ")
        errors[0].detail.contains("properties")
    }

    void "extractKeyTypes, when schema is valid, then return the key type of each property"() {
        expect:
        DataImportSchemaValidator.extractKeyTypes(VALID_SCHEMA) == [
                project : KeyType.PROJECT,
                file    : KeyType.FILE,
                md5     : KeyType.MD5SUM,
                sampleId: KeyType.DATA,
        ]
    }

    @Unroll
    void "location, when property names are #names, then return #expected"() {
        expect:
        DataImportSchemaValidator.location(names as String[]) == expected

        where:
        names              || expected
        []                 || '#'
        ['a']              || '#/a'
        ['a', 'b']         || '#/a/b'
        ['a/b', 'c~d', ''] || '#/a~1b/c~0d/'
    }

    @Unroll
    void "validateRow, when row is '#row', then return #expected"() {
        when:
        List<JsonValidationError> errors = DataImportSchemaValidator.validateRow(row, VALID_SCHEMA)

        then:
        errors*.code == expected

        where:
        row                                              || expected
        '{"project": "p", "file": "/f"}'                 || []
        '{"project": "p", "file": "/f", "other": 1}'     || []
        null                                             || [DataImportSchemaValidator.INVALID_JSON]
        '{'                                              || [DataImportSchemaValidator.INVALID_JSON]
        '"text"'                                         || [DataImportSchemaValidator.NO_OBJECT]
        '[]'                                             || [DataImportSchemaValidator.NO_OBJECT]
        '{"project": "p", "file": "../x", "file": "/f"}' || [DataImportSchemaValidator.INVALID_JSON]
    }

    void "validateRow, when row does not match the schema at several locations, then return one error not matching schema for each"() {
        when:
        List<JsonValidationError> errors = DataImportSchemaValidator.validateRow('{"project": 1, "md5": "x"}', VALID_SCHEMA)

        then:
        errors*.code.unique() == [DataImportSchemaValidator.NOT_MATCHING_SCHEMA]
        errors*.location.toSet() == [ROOT, '#/project', '#/md5'] as Set
        errors.find { it.location == ROOT }.detail.contains("file")
        errors.every { it.detail }
    }

    void "findKeysWithoutAbsolutePath, when values are not absolute paths, then return the sorted key names"() {
        given:
        String row = '''{"b": "relative", "a": 5, "ok": "/abs/path", "missing": null, "c": "/with/../dots", "d": "/with/./dot",
            "e": "/ends/with/..", "f": "/ends/with/.", "dots": "/.hidden/..dir/a..b/file.txt."}'''

        expect:
        DataImportSchemaValidator.findKeysWithoutAbsolutePath(row, ['b', 'a', 'ok', 'missing', 'notInRow', 'c', 'd', 'e', 'f', 'dots']) ==
                ['a', 'b', 'c', 'd', 'e', 'f']
    }

    void "findKeysWithInvalidMd5sum, when values are not valid md5sums, then return the sorted key names"() {
        given:
        String row = '''{"b": "0123456789abcdef", "a": 5, "ok": "0123456789abcdef0123456789abcdef", "missing": null,
            "c": "0123456789ABCDEF0123456789ABCDEF", "d": "0123456789abcdef0123456789abcdeg", "e": "0123456789abcdef0123456789abcdef0",
            "f": "", "g": ["0123456789abcdef0123456789abcdef"]}'''

        expect:
        DataImportSchemaValidator.findKeysWithInvalidMd5sum(row, ['b', 'a', 'ok', 'missing', 'notInRow', 'c', 'd', 'e', 'f', 'g']) ==
                ['a', 'b', 'c', 'd', 'e', 'f', 'g']
    }

    @Unroll
    void "validateJsonObject, when value is '#json', then return #expected"() {
        when:
        List<JsonValidationError> errors = DataImportSchemaValidator.validateJsonObject(json)

        then:
        errors*.code == expected

        where:
        json                      || expected
        '{"a": 1}'                || []
        '{}'                      || []
        '[]'                      || [DataImportSchemaValidator.NO_OBJECT]
        '5'                       || [DataImportSchemaValidator.NO_OBJECT]
        '{"a": 1,}'               || [DataImportSchemaValidator.INVALID_JSON]
        '{"a": 1} x'              || [DataImportSchemaValidator.INVALID_JSON]
        '   '                     || [DataImportSchemaValidator.INVALID_JSON]
        null                      || [DataImportSchemaValidator.INVALID_JSON]
        '{"a": 1, "a": 2}'        || [DataImportSchemaValidator.INVALID_JSON]
        '{"a": {"b": 1, "b": 1}}' || [DataImportSchemaValidator.INVALID_JSON]
    }

    void "validateJsonObject, when value is not an object, then the location is the root"() {
        expect:
        DataImportSchemaValidator.validateJsonObject('[1]') == [error(DataImportSchemaValidator.NO_OBJECT, ROOT)]
    }

    void "validateRowSchema, when called repeatedly, then the result stays the same"() {
        given:
        String invalidSchema = '{"type": "object", "properties": {"p": {"otpKeyType": "UNKNOWN"}}}'

        expect:
        DataImportSchemaValidator.validateRowSchema(VALID_SCHEMA) == []
        DataImportSchemaValidator.validateRowSchema(VALID_SCHEMA) == []
        DataImportSchemaValidator.validateRowSchema(invalidSchema)*.code == [DataImportSchemaValidator.INVALID_KEY_TYPE]
        DataImportSchemaValidator.validateRowSchema(invalidSchema)*.code == [DataImportSchemaValidator.INVALID_KEY_TYPE]
    }

    void "validateRow, when the schema was not validated before, then it is validated against the schema"() {
        given:
        String schema = '{"type": "object", "properties": {"other": {"type": "integer", "otpKeyType": "DATA"}}, "required": ["other"]}'

        expect:
        DataImportSchemaValidator.validateRow('{"other": 1}', schema) == []
        DataImportSchemaValidator.validateRow('{"other": "x"}', schema)*.code == [DataImportSchemaValidator.NOT_MATCHING_SCHEMA]
    }

    void "validateRowSchema, when an invalid schema was used by validateRow before, then it is still invalid"() {
        given:
        String invalidSchema = '{"type": "object", "properties": {"q": {"type": "string"}}}'
        DataImportSchemaValidator.validateRow('{"q": "x"}', invalidSchema)

        expect:
        DataImportSchemaValidator.validateRowSchema(invalidSchema)*.code == [DataImportSchemaValidator.INVALID_KEY_TYPE]
    }

    @Unroll
    void "validateJson, when value is '#json', then return #expected"() {
        when:
        List<JsonValidationError> errors = DataImportSchemaValidator.validateJson(json)

        then:
        errors*.code == expected

        where:
        json                 || expected
        '{"a": 1}'           || []
        '[]'                 || []
        '[1, 2]'             || []
        '["a", "b"]'         || []
        '5'                  || []
        '{"a": 1,}'          || [DataImportSchemaValidator.INVALID_JSON]
        'no json'            || [DataImportSchemaValidator.INVALID_JSON]
        ''                   || [DataImportSchemaValidator.INVALID_JSON]
        '   '                || [DataImportSchemaValidator.INVALID_JSON]
        '{"a": 1} x'         || [DataImportSchemaValidator.INVALID_JSON]
        '{"a": 1}}'          || [DataImportSchemaValidator.INVALID_JSON]
        '[1] [2]'            || [DataImportSchemaValidator.INVALID_JSON]
        null                 || [DataImportSchemaValidator.INVALID_JSON]
        '{"a": 1, "a": 2}'   || [DataImportSchemaValidator.INVALID_JSON]
        '[{"a": 1, "a": 1}]' || [DataImportSchemaValidator.INVALID_JSON]
    }
}
