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
import groovy.transform.CompileDynamic
import groovy.xml.MarkupBuilder

import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion

import java.nio.file.Path

@CompileDynamic
class RoddyConfigService {

    final static String ANALYSIS_ID = "analysis"
    final static String CONFIGURATION_DIRECTORY = "config"
    final static String CONFIGURATION_NAME = "config"
    final static String CONFIGURATION_FILE = "config.xml"

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
                ".*": {
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
                ".*": {
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
    private static final int DROP_CHAR_COUNT = 2

    @SuppressWarnings("DuplicateNumberLiteral")
    static Set<String> validateRoddyConfig(String value) {
        JsonNode node = MAPPER.readTree(value)
        Set<ValidationMessage> errors = SCHEMA_VALIDATOR.validate(node)
        Set<String> errorsString = errors.collect { error ->
            return "RODDY." + error.message.drop(DROP_CHAR_COUNT)
        }
        return errorsString
    }

    @SuppressWarnings("DuplicateNumberLiteral")
    static Set<String> validateRoddyFilenamesConfig(String value) {
        JsonNode node = MAPPER.readTree(value)
        Set<ValidationMessage> errors = FILENAMES_SCHEMA_VALIDATOR.validate(node)
        Set<String> errorsString = errors.collect { error ->
            return "RODDY_FILENAMES." + error.message.drop(DROP_CHAR_COUNT)
        }
        return errorsString
    }

    @SuppressWarnings(["Indentation"])
    String createRoddyXmlConfig(
            String combinedConfig,
            Map<String, String> specificConfig,
            String workflowName,
            WorkflowVersion workflowVersion,
            String analysisConfiguration,
            Path inputDir,
            Path outputDir,
            String queue,
            boolean filenameSectionKillSwitch
    ) {
        assert combinedConfig
        assert specificConfig != null
        assert workflowName
        assert workflowVersion
        assert analysisConfiguration
        assert inputDir
        assert outputDir
        assert queue

        JsonNode combinedConfigJson = MAPPER.readTree(combinedConfig)
        JsonNode roddy = combinedConfigJson.get("RODDY")

        Map<String, String> cValues = [:]
        roddy?.get("cvalues")?.fields()?.each {
            cValues.put(it.key, it.value.get('value').asText())
        }
        cValues.putAll(specificConfig)
        cValues.put("inputBaseDirectory", inputDir)
        cValues.put("outputBaseDirectory", outputDir)

        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)
        xml.omitNullAttributes = true
        xml.configuration(name: CONFIGURATION_NAME, configurationType: "project", usedresourcessize: "l") {
            availableAnalyses {
                analysis(
                        id: ANALYSIS_ID,
                        configuration: analysisConfiguration,
                        useplugin: "${workflowName}:${workflowVersion.workflowVersion}",
                        killswitches: filenameSectionKillSwitch ? "FilenameSection" : null,
                )
            }
            configurationvalues {
                cValues.sort { a, b ->
                    String.CASE_INSENSITIVE_ORDER.compare(a.key, b.key)
                }.each { conf ->
                    cvalue(name: conf.key, value: conf.value)
                }
            }
            processingTools {
                combinedConfigJson.get("RODDY")?.get("resources")?.fields()?.sort { a, b ->
                    String.CASE_INSENSITIVE_ORDER.compare(a.key, b.key)
                }?.each { resource ->
                    tool(
                            name: resource.key,
                            value: resource.value.get("value").textValue(),
                            basepath: resource.value.get("basepath").textValue(),
                            overrideresourcesets: "true",
                    ) {
                        resourcesets {
                            rset(
                                    size: "l",
                                    queue: queue,
                                    memory: resource.value.has("memory") ? resource.value.get("memory").textValue() : null,
                                    cores: resource.value.has("cores") ? resource.value.get("cores").intValue() : null,
                                    nodes: resource.value.has("nodes") ? resource.value.get("nodes").intValue() : null,
                                    walltime: resource.value.has("walltime") ? resource.value.get("walltime").textValue() : null,
                            )
                        }
                    }
                }
            }
            filenames(package: "de.dkfz.b080.co.files", filestagesbase: "de.dkfz.b080.co.files.COFileStage") {
                combinedConfigJson?.get("RODDY_FILENAMES")?.get("filenames")?.elements()?.sort { a, b ->
                    String.CASE_INSENSITIVE_ORDER.compare(a.findValue("class").textValue(), b.findValue("class").textValue())
                }?.each { fileName ->
                    filename(
                            "class": fileName.findValue("class").textValue(),
                            derivedFrom: fileName.has("derivedFrom") ? fileName.get("derivedFrom").textValue() : null,
                            fileStage: fileName.has("fileStage") ? fileName.get("fileStage").textValue() : null,
                            onMethod: fileName.has("onMethod") ? fileName.get("onMethod").textValue() : null,
                            onScriptParameter: fileName.has("onScriptParameter") ? fileName.get("onScriptParameter").textValue() : null,
                            onTool: fileName.has("onTool") ? fileName.get("onTool").textValue() : null,
                            pattern: fileName.get("pattern").textValue(),
                            selectiontag: fileName.has("selectiontag") ? fileName.get("selectiontag").textValue() : null,
                    )
                }
            }
        }
        return writer.toString()
    }

    Path getConfigDirectory(Path basPath) {
        return basPath.resolve(CONFIGURATION_DIRECTORY)
    }

    Path getConfigFile(Path basPath) {
        return getConfigDirectory(basPath).resolve(CONFIGURATION_FILE)
    }
}
