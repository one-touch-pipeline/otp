package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON

class ProcessingOptionController {

    def processingOptionService

    def datatable(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        List<ProcessingOption> processingOptions = processingOptionService.listProcessingOptions(cmd.iDisplayStart, cmd.iDisplayLength)
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
}
