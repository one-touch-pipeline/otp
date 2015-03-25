package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON

class MetaDataFieldsController {
    MetaDataFieldsService metaDataFieldsService


    def index() {
        [:]
    }

    JSON dataTableSourceListLibraryPreparationKit(DataTableCommand cmd) {
        List data = metaDataFieldsService.listLibraryPreparationKitWithAliases().collect {
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

    JSON dataTableSourceListSeqCenter(DataTableCommand cmd) {
        List data = metaDataFieldsService.listSeqCenter().collect {
            [it.name, it.dirName]
        }
        renderData(cmd, data)
    }

    JSON dataTableSourceListSeqPlatformAndIdentifier(DataTableCommand cmd) {
        List data = metaDataFieldsService.listPlatformAndIdentifier().collect {
            [it[0].name, it[0]?.model, it[1]*.name.join(', ')]
        }
        renderData(cmd, data)
    }

    JSON dataTableSourceListSeqType(DataTableCommand cmd) {
        List data = metaDataFieldsService.listSeqType().sort{it.name}.collect {
            [it.name]
        }.unique()
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
