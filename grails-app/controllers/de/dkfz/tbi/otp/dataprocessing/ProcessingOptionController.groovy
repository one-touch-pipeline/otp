package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON

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

    def insert() {
        List<String> projects = ["no project"]
        projectService.getAllProjects().each { Project project ->
            projects.add(project.name)
        }
        [projects: projects]
    }

    def save(ProcessingOptionCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }
        try {
            ProcessingOption processingOption = processingOptionService.createOrUpdate(cmd.name, cmd.type, projectService.getProjectByName(cmd.project), cmd.value, cmd.comment)
            def data = [success: true, id: processingOption.id]
            render data as JSON
        } catch (Exception e) {
            def data = [error: e.message]
            render data as JSON
        }
    }
}

class ProcessingOptionCommand {
    def projectService
    String name
    String type
    String value
    String comment
    String project

    static constraints = {
        name(blank: false)
        type(blank: false)
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