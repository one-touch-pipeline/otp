/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationException
import groovy.transform.Canonical
import org.springframework.http.HttpStatus

import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.TableCellValue
import de.dkfz.tbi.util.TimeFormats

@Secured("hasRole('ROLE_ADMIN')")
class ProcessingOptionController {

    static allowedMethods = [
            index : "GET",
            update: "POST",
            obsolete: "POST",
    ]

    ProcessingOptionService processingOptionService
    PropertiesValidationService propertiesValidationService

    private static final int MAX_LENGTH = 100

    def index() {
        List<ProcessingOption> existingOptions = processingOptionService.listProcessingOptions()

        List<OptionRow> options = OptionName.values().sort().collectMany { OptionName name ->
            List<String> types = name.validatorForType?.allowedValues != null ? name.validatorForType.allowedValues : [null]

            types.sort().collect { String type ->
                ProcessingOption existingOption = existingOptions.find { it.name == name && it.type == type }

                if (existingOption) {
                    generateOptionRow(existingOption)
                } else {
                    ProcessingOption unsavedOption = new ProcessingOption(name: name, type: type)
                    generateOptionRow(unsavedOption)
                }
            }
        }

        return [
                options: options,
        ]
    }

    JSON update(ProcessingOptionCommand cmd) {
        try {
            ProcessingOption processingOption = processingOptionService.createOrUpdate(cmd.optionName, cmd.value, cmd.type, cmd.specificProject)
            render generateOptionRow(processingOption) as JSON
        } catch (ValidationException ignored) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), g.message(code: "processingOption.store.failure") as String)
        }
        return [] as JSON
    }

    def obsolete(ProcessingOptionCommand cmd) {
        try {
            processingOptionService.obsoleteOptionByName(cmd.optionName, cmd.type, cmd.specificProject)
        } catch (ValidationException | ProcessingException ignored) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), g.message(code: "processingOption.obsolete.failure") as String)
        }
        Map data = [obsoleted: true]
        render data as JSON
    }

    /**
     * Generate an optionRow of a processingOption and fill it with meta data.
     * An option row is a presentable format used in the UI.
     * @param processingOption for the row which should be created
     * @return OptionRow: wrapped processing option with row data
     */
    private OptionRow generateOptionRow(ProcessingOption processingOption) {
        OptionProblem.ProblemType problemType = propertiesValidationService.validateProcessingOptionName(processingOption.name, processingOption.type)?.type

        return new OptionRow(
                name: new TableCellValue(
                        value: processingOption.name.toString(),
                        warnColor: null,
                        link: null,
                        tooltip: processingOption.name.description,
                ),
                type: new TableCellValue(
                        value: processingOption.type ?: "",
                        warnColor: (problemType == OptionProblem.ProblemType.TYPE_INVALID) ?
                                TableCellValue.WarnColor.ERROR : TableCellValue.WarnColor.OKAY,
                        link: null,
                        tooltip: processingOption.type,
                ),
                value: new TableCellValue(
                        value: processingOption?.value ?
                                processingOption.value.length() > MAX_LENGTH ?
                                        "...${processingOption.value[-MAX_LENGTH..-1]}" :
                                        processingOption.value :
                                "",
                        warnColor: (problemType in [OptionProblem.ProblemType.VALUE_INVALID, OptionProblem.ProblemType.MISSING]) ?
                                TableCellValue.WarnColor.ERROR : TableCellValue.WarnColor.OKAY,
                        link: null,
                        tooltip: processingOption?.value,
                ),
                allowedValues: processingOption.name.validatorForValue.allowedValues?.sort(),
                project: processingOption?.project,
                dateCreated: TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(processingOption?.dateCreated),
                multiline: processingOption.name.validatorForValue == TypeValidators.MULTI_LINE_TEXT,
                defaultValue: processingOption.name?.defaultValue
        )
    }
}

class ProcessingOptionCommand {
    OptionName optionName
    String value
    String type
    Project specificProject

    void setType(String type) {
        this.type = type ?: null
    }

    static constraints = {
        specificProject nullable: true
        value nullable: true
    }
}

@Canonical
class OptionRow {
    TableCellValue name
    TableCellValue type
    TableCellValue value
    List<String> allowedValues
    Project project
    String dateCreated
    boolean multiline
    String defaultValue
}
