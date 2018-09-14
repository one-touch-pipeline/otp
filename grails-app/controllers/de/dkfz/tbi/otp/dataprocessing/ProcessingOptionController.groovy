package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import grails.validation.*
import groovy.transform.*

class ProcessingOptionController {

    static allowedMethods = [
            index : "GET",
            update: "POST",
    ]

    ProcessingOptionService processingOptionService
    ProjectService projectService
    PropertiesValidationService propertiesValidationService

    private static final int MAX_LENGTH = 20

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
                )
            }
        }

        return [
                options: options,
        ]
    }

    def update(ProcessingOptionCommand cmd) {
        try {
            processingOptionService.createOrUpdate(cmd.optionName, cmd.value, cmd.type != "" ? cmd.type : null)
            flash.message = g.message(code: "processingOption.store.success")
        } catch (ValidationException e) {
            flash.message = g.message(code: "processingOption.store.failure")
            flash.errors = e.errors
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
}
