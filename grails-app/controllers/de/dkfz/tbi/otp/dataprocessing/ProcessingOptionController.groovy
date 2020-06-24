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

import grails.validation.ValidationException
import groovy.transform.Canonical

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.qcTrafficLight.TableCellValue

class ProcessingOptionController {

    static allowedMethods = [
            index : "GET",
            update: "POST",
    ]

    ProcessingOptionService processingOptionService
    ProjectService projectService
    PropertiesValidationService propertiesValidationService

    private static int MAX_LENGTH = 100

    def index() {
        List<ProcessingOption> existingOptions = processingOptionService.listProcessingOptions()

        List<OptionRow> options = OptionName.values().sort().collectMany { OptionName name ->
            List<String> types = name.validatorForType?.allowedValues != null ? name.validatorForType.allowedValues : [null]

            types.sort().collect { String type ->
                ProcessingOption existingOption = existingOptions.find { it.name == name && it.type == type }

                OptionProblem.ProblemType problemType = propertiesValidationService.validateProcessingOptionName(name, type)?.type

                new OptionRow(
                        new TableCellValue(
                                name.name(),
                                null,
                                null,
                                name.description,
                        ),
                        new TableCellValue(
                                type ?: "",
                                (problemType == OptionProblem.ProblemType.MISSING) ?
                                        TableCellValue.WarnColor.ERROR : TableCellValue.WarnColor.OKAY,
                                null,
                                type,
                        ),
                        new TableCellValue(
                                existingOption?.value ?
                                        existingOption.value.length() > MAX_LENGTH ?
                                                "...${existingOption.value[-MAX_LENGTH..-1]}" :
                                                existingOption.value :
                                        "",
                                (problemType in [OptionProblem.ProblemType.VALUE_INVALID, OptionProblem.ProblemType.TYPE_INVALID]) ?
                                        TableCellValue.WarnColor.ERROR : TableCellValue.WarnColor.OKAY,
                                null,
                                existingOption?.value,
                        ),
                        name.validatorForValue.allowedValues?.sort(),
                        existingOption?.project?.name,
                        existingOption?.dateCreated?.format("yyyy-MM-dd HH:mm:ss"),
                        name.validatorForValue == TypeValidators.MULTI_LINE_TEXT,
                )
            }
        }

        return [
                options: options,
        ]
    }

    def update(ProcessingOptionCommand cmd) {
        try {
            processingOptionService.createOrUpdate(cmd.optionName, cmd.value, cmd.type == "" ? null : cmd.type)
            flash.message = new FlashMessage(g.message(code: "processingOption.store.success") as String)
        } catch (ValidationException e) {
            flash.message = new FlashMessage(g.message(code: "processingOption.store.failure") as String, e.errors)
       }
        redirect(action: "index")
    }
}

class ProcessingOptionCommand {
    OptionName optionName
    String type
    String value

    static constraints = {
        value nullable: true
    }
}

@Canonical
class OptionRow {
    TableCellValue name
    TableCellValue type
    TableCellValue value
    List<String> allowedValues
    String project
    String dateCreated
    boolean multiline
}
