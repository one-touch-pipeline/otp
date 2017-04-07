package de.dkfz.tbi.otp.dataprocessing

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
        dataToRender.iTotalRecords = processingOptionService.countProcessingOption()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        processingOptions.each { processingOption ->
            dataToRender.aaData << [
                    option: processingOption,
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
                    processingOptionService.createOrUpdate(cmd.name, cmd.type != "" ? cmd.type : null, projectService.getProjectByName(cmd.project), cmd.value, cmd.comment)
                    message = "Saved successfully"
                } catch (Exception e) {
                    message = e.message
                }
            }
        }
        return [
                projects: projects,
                message: message,
                cmd: cmd,
                hasErrors: hasErrors,
        ]
    }
}

class ProcessingOptionCommand {
    def projectService
    String name
    String type
    String value
    String comment
    String project
    String submit

    static constraints = {
        name(blank: false)
        value(blank: false)
        comment(blank: false)
        project(validator: { val, obj ->
            if (val == "no project") {
                return true
            }
            return val && (obj.projectService.getProjectByName(val) != null)
        })
    }
}
