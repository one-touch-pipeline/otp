package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON

class MetaDataFieldsController {
    MetaDataFieldsService metaDataFieldsService


    def index() {
        [:]
    }

    JSON dataTableSourceListExomeEnrichmentKit(DataTableCommand cmd) {
        List data = metaDataFieldsService.listExomeEnrichmentKitWithAliases().collect {
            [it[0].name, it[1]*.name.join(', ')]
        }
        renderData(cmd, data)
    }

    JSON dataTableSourceListAntibodyTarget(DataTableCommand cmd) {
        List data = metaDataFieldsService.listAntibodyTarget().collect {
            [it.name]
        }
        renderData(cmd, data)
    }

    private JSON renderData(DataTableCommand cmd, List data) {
        Map dataToRender = cmd.dataToRender()
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }
}
