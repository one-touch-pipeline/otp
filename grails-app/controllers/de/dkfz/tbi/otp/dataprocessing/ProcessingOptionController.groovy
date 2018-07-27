package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*
import org.springframework.validation.*

class ProcessingOptionController {

    def processingOptionService
    def projectService

    def datatable(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        List<ProcessingOption> processingOptions = processingOptionService.listProcessingOptions()
        dataToRender.iTotalRecords = processingOptions.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        processingOptions.each { processingOption ->
            dataToRender.aaData << [
                    option : processingOption,
                    project: processingOption.project.toString()
            ]
        }
        render dataToRender as JSON
    }

    def index() {
    }

    def insert(ProcessingOptionCommand cmd) {
        String message
        boolean hasErrors
        List<String> projects = ["no project"]
        projectService.getAllProjects().each { Project project ->
            projects.add(project.name)
        }
        if (cmd.submit == "Save") {
            hasErrors = cmd.hasErrors()
            if (hasErrors) {
                FieldError errors = cmd.errors.getFieldError()
                message = "'" + errors.getRejectedValue() + "' is not a valid value for '" + errors.getField() + "'. Error code: '" + errors.code + "'"
            } else {
                try {
                    processingOptionService.createOrUpdate(cmd.optionName, cmd.type != "" ? cmd.type : null, projectService.getProjectByName(cmd.project), cmd.value)
                    message = "Saved successfully"
                } catch (Exception e) {
                    message = e.message
                }
            }
        }
        return [
                projects: projects,
                optionNames: OptionName.findAll().sort{ it.toString() },
                message: message,
                cmd: cmd,
                hasErrors: hasErrors,
        ]
    }
}

class ProcessingOptionCommand {
    def projectService
    OptionName optionName
    String type
    String value
    String project
    String submit

    static constraints = {
        value(validator: { val, obj ->
            val != null && obj.optionName && obj.optionName.type.validate(val)
        })
        type(nullable: true, blank: true)
        project(validator: { val, obj ->
            if (val == "no project") {
                return true
            }
            return val && (obj.projectService.getProjectByName(val) != null)
        })
    }
}
