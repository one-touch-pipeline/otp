package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON
import groovy.json.JsonSlurper

class IndividualController {

    def individualService
    def igvSessionFileService
    def projectService

    def display = {
        redirect(action: "show", id: params.id)
    }

    def show = {
        if (!params.id) {
            params.id = "0"
        }
        Individual ind = individualService.getIndividual(params.id)
        if (!ind) {
            response.sendError(404)
            return
        }

        [
            ind: ind,
            previous: individualService.previousIndividual(ind),
            next: individualService.nextIndividual(ind),
            igvMap: igvSessionFileService.createMapOfIgvEnabledScans(ind.seqScans)
        ]
    }

    def list = {
    }

    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        dataToRender.iTotalRecords = individualService.countIndividual(cmd.sSearch)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        individualService.listIndividuals(cmd.iDisplayStart, cmd.iDisplayLength, cmd.sortOrder, cmd.iSortCol_0, cmd.sSearch).each { individual ->
            dataToRender.aaData << [
                [id: individual.id, text: individual.pid],
                individual.mockFullName,
                individual.mockPid,
                individual.project.toString(),
                individual.type.toString()
            ]
        }
        render dataToRender as JSON
    }

    def igvStart = {
        println params
        println flash

        // TODO: this code is crashy and cannot be protected
        List<SeqScan> scans = new ArrayList<SeqScan>()
        params.each {
            if (it.value == "on") {
                SeqScan scan = SeqScan.get(it.key as int)
                scans << scan
            }
        }
        String url = igvSessionFileService.buildSessionFile(scans)
        println "Redirecting: ${url}"
        redirect(url: url)
    }

    def igvDownload = {
        println "Download"
        render "downloding ..."
    }

    def insert = {
        List<Project> projects = projectService.getAllProjects()
        List<Individual.Type> individualTypes = Individual.Type.values()
        List<String> sampleTypes = individualService.getSampleTypeNames()
        List<SampleIdentifier> sampleIdentifiers = individualService.getSampleIdentifiers()
        [projects: projects, individualTypes: individualTypes, sampleTypes: sampleTypes, sampleIdentifiers: sampleIdentifiers]
    }

    def save = { IndividualCommand cmd ->
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }
        try {
            individualService.createIndividual(projectService.getProject(cmd.project), cmd)
            def data = [success: true]
            render data as JSON
        } catch (IndividualCreationException e) {
            def data = [error: e.message]
            render data as JSON
        }
    }
}

class IndividualCommand {
    def individualService
    def projectService
    String pid
    Long project
    String mockPid
    String mockFullName
    Individual.Type individualType
    String samples

    Map<String, List<String>> parseSamples() {
        def jSonObj = new JsonSlurper().parseText(this.samples)
        Map<String, List<String>> samplesMap = [:]
        jSonObj.each {
            samplesMap.put(it.type, it.id)
        }
        return samplesMap
    }

    static constraints = {
        pid(blank: false, validator: { val, obj ->
            return val && !obj.individualService.individualExists(val)
        })
        mockPid(blank: false)
        mockFullName(blank: false)
        project(min: 0L, validator: { val, obj ->
            return val && (obj.projectService.getProject(val) != null)
        })
    }
}
