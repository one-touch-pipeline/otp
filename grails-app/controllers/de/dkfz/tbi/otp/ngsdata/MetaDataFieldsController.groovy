package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON

class MetaDataFieldsController {
    MetaDataFieldsService metaDataFieldsService


    def index() {
        [:]
    }

    JSON dataTableSourceListExomeEnrichmentKit(DataTableCommand cmd) {

        Map dataToRender = cmd.dataToRender()
        List data = metaDataFieldsService.listExomeEnrichmentKitWithAliases().collect {
            [it[0].name, it[1]*.name.join(', ')]
        }
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

}
