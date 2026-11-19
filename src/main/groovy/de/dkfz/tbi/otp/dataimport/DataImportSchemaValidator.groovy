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

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.core.JsonLocation
import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.Error as SchemaError
import com.networknt.schema.Schema
import com.networknt.schema.SchemaException
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.dialect.BasicDialectRegistry
import com.networknt.schema.dialect.Dialect
import com.networknt.schema.dialect.Dialects
import com.networknt.schema.keyword.NonValidationKeyword
import com.networknt.schema.path.NodePath

import de.dkfz.tbi.otp.utils.validation.OtpPathValidator

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Validates the JSON schema of one row of a data import ({@link DataTypeVersion#rawJsonSchema}) and checks the rows against it.
 *
 * The validation works without network access: the meta-schema of draft 2020-12 is shipped with the library,
 * and remote resources are never fetched, so a schema referencing a remote resource is invalid.
 *
 * The validation methods return one {@link JsonValidationError} for each location with an error, or an empty list for a valid
 * value. The location is a JSON pointer with the prefix "#", e.g. "#/properties/project", or for an invalid JSON the position in the
 * text, e.g. "line 2, column 5".
 *
 * Valid row schemas are cached compiled, since each row of an import is validated against the same schema.
 * The number of row schemas is small (one per {@link DataTypeVersion}), so the cache is not limited.
 */
class DataImportSchemaValidator {

    /** keyword in each property of the row schema defining the {@link KeyType} of the key */
    static final String KEY_TYPE_KEYWORD = "otpKeyType"

    static final String CODE_PREFIX = "json.validate."
    static final String INVALID_JSON = "${CODE_PREFIX}invalid.json"
    static final String INVALID_SCHEMA = "${CODE_PREFIX}invalid.schema"
    static final String NO_OBJECT_SCHEMA = "${CODE_PREFIX}no.object.schema"
    static final String NO_PROPERTIES = "${CODE_PREFIX}no.properties"
    static final String INVALID_KEY_TYPE = "${CODE_PREFIX}invalid.key.type"
    static final String PROJECT_NOT_REQUIRED = "${CODE_PREFIX}project.not.required"
    static final String NO_OBJECT = "${CODE_PREFIX}no.object"
    static final String NOT_MATCHING_SCHEMA = "${CODE_PREFIX}not.matching.schema"

    /** location of the root of a JSON value */
    static final String ROOT = "#"

    private static final String META_SCHEMA = "https://json-schema.org/draft/2020-12/schema"
    private static final String PROPERTIES = "properties"
    private static final String REQUIRED = "required"
    private static final Pattern MD5SUM_PATTERN = ~/^[0-9a-f]{32}$/

    /**
     * Fails on content after the JSON value, e.g. '{"a": 1} x', and on duplicate keys, e.g. '{"a": 1, "a": 2}',
     * which Jackson both accepts by default. With duplicate keys, Jackson keeps only the last value, so a check of the parsed
     * value, e.g. for an absolute path, would not see the other values of the stored JSON.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(
            DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
            DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY,
    )

    private static final Dialect DIALECT = Dialect.builder(Dialects.draft202012)
            .keyword(new NonValidationKeyword(KEY_TYPE_KEYWORD))
            .build()

    private static final SchemaRegistry REGISTRY = SchemaRegistry.builder()
            .defaultDialectId(DIALECT.id)
            .dialectRegistry(new BasicDialectRegistry(DIALECT))
            .build()

    private static final Schema META_SCHEMA_VALIDATOR = REGISTRY.getSchema(SchemaLocation.of(META_SCHEMA))

    /** compiled valid row schemas by their JSON */
    private static final Map<String, Schema> VALID_ROW_SCHEMAS = new ConcurrentHashMap<>()

    /**
     * Checks that the row schema is a valid JSON schema (draft 2020-12) of an object with properties,
     * each having a valid {@link #KEY_TYPE_KEYWORD}. Properties of {@link KeyType#PROJECT} have to be listed in "required",
     * since each row has to reference its project.
     */
    static List<JsonValidationError> validateRowSchema(String schema) {
        if (schema == null) {
            return [noContent()]
        }
        if (VALID_ROW_SCHEMAS.containsKey(schema)) {
            return []
        }
        List<JsonValidationError> errors = []
        JsonNode node = parse(schema, errors)
        if (node == null) {
            return errors
        }
        errors = convert(INVALID_SCHEMA, META_SCHEMA_VALIDATOR.validate(node))
        if (errors) {
            return errors
        }
        Schema compiledSchema
        try {
            // fails for a dialect other than draft 2020-12, e.g. "$schema" of draft 7, which passes the meta-schema check
            compiledSchema = REGISTRY.getSchema(node)
            compiledSchema.initializeValidators()
        } catch (SchemaException e) {
            String location = pointerLocation(e.error?.instanceLocation)
            return [new JsonValidationError(INVALID_SCHEMA, location, e.message)]
        }
        if (node.path("type").asText() != "object") {
            return [new JsonValidationError(NO_OBJECT_SCHEMA, ROOT, null)]
        }
        JsonNode properties = node.path(PROPERTIES)
        if (!properties.isObject() || properties.isEmpty()) {
            return [new JsonValidationError(NO_PROPERTIES, location(PROPERTIES), null)]
        }
        errors = validateKeyTypes(node)
        if (errors) {
            return errors
        }
        VALID_ROW_SCHEMAS.put(schema, compiledSchema)
        return []
    }

    /**
     * @return whether the row schema is valid, see {@link #validateRowSchema}
     */
    static boolean isValidRowSchema(String schema) {
        return validateRowSchema(schema).isEmpty()
    }

    /**
     * Checks that the value is valid JSON.
     */
    static List<JsonValidationError> validateJson(String json) {
        List<JsonValidationError> errors = []
        parse(json, errors)
        return errors
    }

    /**
     * Checks that the value is a JSON object.
     */
    static List<JsonValidationError> validateJsonObject(String json) {
        List<JsonValidationError> errors = []
        parseObject(json, errors)
        return errors
    }

    /**
     * Checks that the row is a JSON object matching the row schema, which has to be valid ({@link #validateRowSchema}).
     */
    static List<JsonValidationError> validateRow(String row, String rowSchema) {
        List<JsonValidationError> errors = []
        JsonNode node = parseObject(row, errors)
        if (node == null) {
            return errors
        }
        // only validateRowSchema fills the cache, so that it contains only valid schemas
        Schema compiledSchema = VALID_ROW_SCHEMAS[rowSchema] ?: REGISTRY.getSchema(MAPPER.readTree(rowSchema))
        return convert(NOT_MATCHING_SCHEMA, compiledSchema.validate(node))
    }

    /**
     * Returns the names of the given keys whose value in the row is not a valid absolute path.
     * A valid absolute path contains no path components like "." or ".." ({@link OtpPathValidator#isValidAbsolutePath}).
     * Keys missing in the row or with the value null are ignored. The row has to be a JSON object.
     */
    static List<String> findKeysWithoutAbsolutePath(String row, Collection<String> keyNames) {
        return findKeysWithInvalidText(row, keyNames) { String text -> OtpPathValidator.isValidAbsolutePath(text) }
    }

    /**
     * Returns the names of the given keys whose value in the row is not a valid md5sum (32 lowercase hexadecimal characters).
     * Keys missing in the row or with the value null are ignored. The row has to be a JSON object.
     */
    static List<String> findKeysWithInvalidMd5sum(String row, Collection<String> keyNames) {
        return findKeysWithInvalidText(row, keyNames) { String text -> text ==~ MD5SUM_PATTERN }
    }

    /**
     * Returns the sorted names of the given keys whose value in the row is not text or a text not accepted by the check.
     * Keys missing in the row or with the value null are ignored.
     */
    private static List<String> findKeysWithInvalidText(String row, Collection<String> keyNames, Closure<Boolean> isValidText) {
        JsonNode node = MAPPER.readTree(row)
        return keyNames.findAll { String keyName ->
            JsonNode value = node.path(keyName)
            return !(value.isMissingNode() || value.isNull()) && !(value.isTextual() && isValidText(value.asText()))
        }.sort()
    }

    /**
     * Returns the {@link KeyType} of each property of a row schema, which has to be valid ({@link #validateRowSchema}).
     */
    static Map<String, KeyType> extractKeyTypes(String schema) {
        return MAPPER.readTree(schema).path(PROPERTIES).properties().collectEntries { Map.Entry<String, JsonNode> property ->
            [(property.key): KeyType.valueOf(property.value.path(KEY_TYPE_KEYWORD).asText())]
        } as Map<String, KeyType>
    }

    /**
     * @return the location of the value reached by the given property names from the root, e.g. "#/properties/project"
     */
    static String location(String... propertyNames) {
        JsonPointer pointer = propertyNames.inject(JsonPointer.empty()) { JsonPointer p, String name -> p.appendProperty(name) }
        return ROOT + pointer
    }

    /**
     * Checks the {@link #KEY_TYPE_KEYWORD} of each property and that the properties of {@link KeyType#PROJECT} are required.
     */
    private static List<JsonValidationError> validateKeyTypes(JsonNode schema) {
        List<String> keyTypes = KeyType.values()*.name()
        // validateRowSchema checks the schema against the meta-schema first, so "required" is either missing or a list of strings
        List<String> required = schema.path(REQUIRED).toList()*.asText()
        return schema.path(PROPERTIES).properties().collect { Map.Entry<String, JsonNode> property ->
            String keyType = property.value.path(KEY_TYPE_KEYWORD).asText()
            if (!(keyType in keyTypes)) {
                return new JsonValidationError(INVALID_KEY_TYPE, location(PROPERTIES, property.key, KEY_TYPE_KEYWORD), keyTypes.join(", "))
            }
            if (keyType == KeyType.PROJECT.name() && !(property.key in required)) {
                return new JsonValidationError(PROJECT_NOT_REQUIRED, location(PROPERTIES, property.key), null)
            }
            return null
        }.findAll()
    }

    /**
     * @return one error for each location and message; the meta-schema validation reports some errors more than once
     */
    private static List<JsonValidationError> convert(String code, List<SchemaError> schemaErrors) {
        return schemaErrors.collect { SchemaError error ->
            new JsonValidationError(code, pointerLocation(error.instanceLocation), error.message)
        }.unique()
    }

    /**
     * @return the location of the path, or the root if the path is unknown, e.g. for an exception without location
     */
    private static String pointerLocation(NodePath path) {
        return path == null ? ROOT : ROOT + path
    }

    private static String textLocation(JsonLocation location) {
        return location ? "line ${location.lineNr}, column ${location.columnNr}" : ROOT
    }

    private static JsonValidationError noContent() {
        return new JsonValidationError(INVALID_JSON, "line 1, column 1", "no content")
    }

    /**
     * @return the parsed JSON object, or null if it is not a valid JSON or not an object; then the error is added to the given list
     */
    private static JsonNode parseObject(String json, List<JsonValidationError> errors) {
        JsonNode node = parse(json, errors)
        if (node != null && !node.isObject()) {
            errors << new JsonValidationError(NO_OBJECT, ROOT, null)
            return null
        }
        return node
    }

    /**
     * @return the parsed JSON, or null if it is not a valid JSON, including an empty or blank input; then the error is added to the given list
     */
    private static JsonNode parse(String json, List<JsonValidationError> errors) {
        if (json == null) {
            errors << noContent()
            return null
        }
        JsonNode node = null
        try {
            node = MAPPER.readTree(json)
            // for blank input, Jackson returns a MissingNode instead of failing
            if (node.isMissingNode()) {
                errors << noContent()
                node = null
            }
        } catch (JacksonException e) {
            errors << new JsonValidationError(INVALID_JSON, textLocation(e.location), e.originalMessage)
        }
        return node
    }
}
