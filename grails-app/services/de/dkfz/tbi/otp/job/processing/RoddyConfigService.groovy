/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.job.processing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.*

class RoddyConfigService {

    private static final String SCHEMA = '''
{
  "$schema": "https://json-schema.org/draft/2019-09/schema",
  "type": "object",
  "title": "Roddy configuration schema",
  "required": [],
  "properties": {
    "cvalues": {
      "type": "object",
      "title": "Regular configuration (cvalues)",
      "description": "",
      "additionalProperties": false,
      "patternProperties": {
        "": {
          "anyOf": [
            {
              "type": "object",
              "title": "Configuration",
              "required": [
                "value"
              ],
              "properties": {
                "value": {
                  "type": "string"
                },
                "type": {
                  "type": "string",
                  "enum": [
                    "bashArray",
                    "double",
                    "filename",
                    "filenamePattern",
                    "float",
                    "path",
                    "string"
                  ]
                }
              },
              "additionalProperties": false
            },
            {
              "type": "object",
              "title": "Configuration with type integer",
              "required": [
                "value",
                "type"
              ],
              "properties": {
                "value": {
                  "type": "integer"
                },
                "type": {
                  "type": "string",
                  "const": "integer"
                }
              },
              "additionalProperties": false
            },
            {
              "type": "object",
              "title": "Configuration with type boolean",
              "required": [
                "value",
                "type"
              ],
              "properties": {
                "value": {
                  "type": "string",
                  "enum": [
                    "true",
                    "false"
                  ]
                },
                "type": {
                  "type": "string",
                  "const": "boolean"
                }
              },
              "additionalProperties": false
            }
          ]
        }
      }
    },
    "resources": {
      "type": "object",
      "title": "Configuration of resources",
      "additionalProperties": false,
      "patternProperties": {
        "": {
          "type": "object",
          "required": [
            "value",
            "basepath"
          ],
          "properties": {
            "value": {
              "type": "string"
            },
            "basepath": {
              "type": "string"
            },
            "memory": {
              "type": "string"
            },
            "cores": {
              "type": "integer"
            },
            "nodes": {
              "type": "integer"
            },
            "walltime": {
              "type": "string"
            }
          },
          "additionalProperties": false
        }
      }
    }
  },
  "additionalProperties": false
}
'''

    private static final String FILENAMES_SCHEMA = '''
{
  "$schema": "https://json-schema.org/draft/2019-09/schema",
  "type": "object",
  "title": "Roddy configuration schema",
  "required": [
  ],
  "properties": {
    "filenames": {
      "type": "array",
      "title": "Configuration of file names",
      "additionalItems": false,
      "items": {
        "type": "object",
        "required": [
          "class",
          "pattern"
        ],
        "properties": {
          "class": {
            "type": "string"
          },
          "pattern": {
            "type": "string"
          },
          "selectiontag": {
            "type": "string"
          },
          "derivedFrom": {
            "type": "string"
          },
          "fileStage": {
            "type": "string"
          },
          "onMethod": {
            "type": "string"
          },
          "onScriptParameter": {
            "type": "string"
          },
          "onTool": {
            "type": "string"
          }
        },
        "additionalProperties": false
      }
    }
  },
  "additionalProperties": false
}
'''

    private static final JsonSchema SCHEMA_VALIDATOR = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema(SCHEMA)
    private static final JsonSchema FILENAMES_SCHEMA_VALIDATOR = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema(FILENAMES_SCHEMA)
    private static final ObjectMapper MAPPER = new ObjectMapper()

    static boolean validateRoddyConfig(String value) {
        JsonNode node = MAPPER.readTree(value)
        Set<ValidationMessage> errors = SCHEMA_VALIDATOR.validate(node)
        return errors.empty
    }

    static boolean validateRoddyFilenamesConfig(String value) {
        JsonNode node = MAPPER.readTree(value)
        Set<ValidationMessage> errors = FILENAMES_SCHEMA_VALIDATOR.validate(node)
        return errors.empty
    }
}
