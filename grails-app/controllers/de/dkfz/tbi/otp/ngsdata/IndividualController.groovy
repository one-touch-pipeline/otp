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

        // TODO: this code is crashy and cannot be protected
        List<SeqScan> scans = new ArrayList<SeqScan>()
        params.each {
            if (it.value == "on") {
                SeqScan scan = SeqScan.get(it.key as int)
                scans << scan
            }
        }
        String url = igvSessionFileService.buildSessionFile(scans)
        redirect(url: url)
    }

    def igvDownload = {
        render "downloding ..."
    }

    def insert() {
        List<Project> projects = projectService.getAllProjects()
        List<Individual.Type> individualTypes = Individual.Type.values()
        List<String> sampleTypes = individualService.getSampleTypeNames()
        List<SampleIdentifier> sampleIdentifiers = individualService.getSampleIdentifiers()
        [projects: projects, individualTypes: individualTypes, sampleTypes: sampleTypes, sampleIdentifiers: sampleIdentifiers]
    }

    def save(IndividualCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }
        try {
            List<SamplesParser> parsedSamples = new SamplesParser().insertSamplesFromJSON(cmd.samples)
            Individual individual = individualService.createIndividual(projectService.getProject(cmd.project), cmd, parsedSamples)
            def data = [success: true, id: individual.id]
            render data as JSON
        } catch (Exception e) {
            def data = [error: e.message]
            render data as JSON
        }
    }

    def updateField(UpdateFieldCommand cmd) {
        Individual individual = individualService.getIndividual(cmd.id)
        if (!individual) {
            def data = [error: g.message(code: "individual.update.notFound", args: [cmd.id])]
            render data as JSON
            return
        }
        def data = [:]
        try {
            individualService.updateField(individual, cmd.key, cmd.value)
            data.put("success", true)
        } catch (IndividualUpdateException e) {
            data.put("error", g.message(code: "individual.update.error"))
        } catch (ChangelogException e) {
            data.put("error", g.message(code: "individual.update.changelog.error"))
        }
        render data as JSON
    }

    def updateSamples(UpdateSamplesCommand cmd) {
        Individual individual = individualService.getIndividual(cmd.id)
        if (!individual) {
            def data = [error: g.message(code: "individual.update.notFound", args: [cmd.id])]
            render data as JSON
            return
        }
        def data = [:]
        try {
            SamplesParser parsedSamples = new SamplesParser().modifySamplesFromJSON(cmd.samples)
            individualService.createOrUpdateSamples(individual, [parsedSamples])
            data.put("success", true)
        } catch (IndividualUpdateException e) {
            data.put("error", g.message(code: "individual.update.error"))
        } catch (ChangelogException e) {
            data.put("error", g.message(code: "individual.update.changelog.error"))
        }
        render data as JSON
    }

    def newSampleType(UpdateFieldCommand cmd) {
        Individual individual = individualService.getIndividual(cmd.id)
        if (!individual) {
            def data = [error: g.message(code: "individual.update.notFound", args: [cmd.id])]
            render data as JSON
            return
        }
        def data = [:]
        try {
            individualService.createSample(individual, cmd.value)
            data.put("success", true)
        } catch (Exception e) {
            data = [error: e.message]
        }
        render data as JSON
    }

    def typeDropDown() {
        render Individual.Type.values() as JSON
    }

    def sampleTypeDropDown() {
        render individualService.getSampleTypeNames() as JSON
    }

    def retrieveSampleIdentifiers(RetrieveSampleIdentifiersCommand cmd) {
        render individualService.getSampleIdentifiers(cmd.id, cmd.sampleType) as JSON
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

class UpdateFieldCommand {
    def individualService
    Long id
    String key
    String value

    static constraints = {
        id(min: 0L, validator: { val, obj ->
            return val && (obj.individualService.getIndividual(val) != null)
        })
        key(blank: false)
        value(blank: false)
    }
}

class RetrieveSampleIdentifiersCommand {
    def individualService
    Long id
    String sampleType

    static constraints = {
        id(min: 0L, validator: { val, obj ->
            return val && (obj.individualService.getIndividual(val) != null)
        })
        sampleType(blank: false)
    }
}

class UpdateSamplesCommand {
    def individualService
    Long id
    String samples

    static constraints = {
        id(min: 0L, validator: { val, obj ->
            return val && (obj.individualService.getIndividual(val) != null)
        })
        samples(blank: false)
    }
}

class SamplesParser {
    Map<String, String> updateEntries = [:]
    List<String> newEntries = []
    String type

    private SamplesParser modifySamplesFromJSON(String json) {
        if (!json) {
            return null
        }
        def slurper = new JsonSlurper()
        SamplesParser samplesParser = new SamplesParser()
        slurper.parseText(json).eachWithIndex { value, index ->
            if (index == 0) {
                samplesParser.type = value.type
            }
            if (!isValues(value)) {
                return // next Sample
            }
            if (value.id) {
                samplesParser.updateEntries.put(value.id.get(0), value.identifier.get(0))
            } else {
                samplesParser.newEntries << value.identifier.get(0)
            }
        }
        return samplesParser
    }

    private List<SamplesParser> insertSamplesFromJSON(String json) {
        if (!json) {
            return null
        }
        def slurper = new JsonSlurper()
        List<SamplesParser> parsedSamples = []
        SamplesParser samplesParser
        slurper.parseText(json).eachWithIndex { value, index ->
            if (!isValues(value)) {
                return // next Sample
            }
            if (index == 0 || !(parsedSamples.get(index - 1).type.equals(value.type))) {
                samplesParser = new SamplesParser()
                samplesParser.type = value.type
                parsedSamples.add(samplesParser)
            }
            value.identifier.each { String v ->
                    samplesParser.newEntries << v
            }
        }
        return parsedSamples
    }

    private boolean isValues(Map value) {
        if (!value.type) {
            return false
        }
        if (!value.identifier) {
            return false
        }
        return true
    }
}
