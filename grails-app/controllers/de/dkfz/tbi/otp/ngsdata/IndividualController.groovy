package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.CommentCommand
import grails.converters.JSON
import groovy.json.JsonSlurper
import de.dkfz.tbi.otp.utils.DataTableCommand

class IndividualController {

    def individualService
    def igvSessionFileService
    def projectService

    def display = {
        redirect(action: "show", id: params.id)
    }

    def show = {
        Individual ind
        if (params.id) {
            ind = individualService.getIndividual(params.id)
        } else if (params.mockPid) {
            ind = individualService.getIndividualByMockPid(params.mockPid)
        } else {
            //neither id not mockPid given, give up
            response.sendError(404)
            return
        }
        if (!ind) {
            response.sendError(404)
            return
        }

        [
            ind: ind,
            previous: individualService.previousIndividual(ind),
            next: individualService.nextIndividual(ind),
            igvMap: igvSessionFileService.createMapOfIgvEnabledScans(ind.seqScans),
            comment: ind.comment,
            commentDate: ind.commentDate?.format('EEE, d MMM yyyy HH:mm'),
            commentAuthor: ind.commentAuthor
        ]
    }

    def list = {
        Map retValue  = [
            projects: projectService.getAllProjects(),
            individualTypes: Individual.Type.values()
        ]
        return retValue
    }

    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        IndividualFiltering filtering = IndividualFiltering.fromJSON(params.filtering)

        dataToRender.iTotalRecords = individualService.countIndividual(filtering, cmd.sSearch)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        individualService.listIndividuals(cmd.sortOrder, IndividualSortColumn.fromDataTable(cmd.iSortCol_0), filtering, cmd.sSearch).each { individual ->
            dataToRender.aaData << [
                id: individual.id,
                pid: individual.pid,
                mockFullName: individual.mockFullName,
                mockPid: individual.mockPid,
                project: individual.project.toString(),
                type: individual.type.toString()
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

    // params.id, params.comment, date
    def saveIndividualComment(CommentCommand cmd) {
        def date = new Date()
        Individual individual = individualService.getIndividual(cmd.id)
        individualService.saveComment(individual, cmd.comment, date)
        def dataToRender = [date: date?.format('EEE, d MMM yyyy HH:mm'), author: individual.commentAuthor]
        render dataToRender as JSON
    }
}

enum IndividualSortColumn {
    PID("pid"),
    MOCKFULLNAME("mockFullName"),
    MOCKPID("mockPid"),
    PROJECT("project"),
    TYPE("type"),

    private final String columnName

    IndividualSortColumn(String column) {
        this.columnName = column
    }

    static IndividualSortColumn fromDataTable(int column) {
        switch (column) {
            case 0:
                return IndividualSortColumn.PID
            case 1:
                return IndividualSortColumn.MOCKFULLNAME
            case 2:
                return IndividualSortColumn.MOCKPID
            case 3:
                return IndividualSortColumn.PROJECT
            default:
                return IndividualSortColumn.TYPE
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
    String internIdentifier
    Individual.Type individualType
    String samples

    static constraints = {
        pid(blank: false, validator: { val, obj ->
            return val && !obj.individualService.individualExists(val)
        })
        mockPid(blank: false)
        mockFullName(blank: false)
        internIdentifier(nullable: true, blank: true)
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
/**
 * Class describing the various filtering to do on Individual page.
 *
 */
class IndividualFiltering {
    List<Long> project = []
    List<String> pid = []
    List<String> mockPid = []
    List<String> mockFullName = []
    List<Individual.Type> type= []

    boolean enabled = false

    static IndividualFiltering fromJSON(String json) {
        IndividualFiltering filtering = new IndividualFiltering()
        if (!json) {
            return filtering
        }
        def slurper = new JsonSlurper()
        slurper.parseText(json).each {
            switch (it.type) {
                case "projectSelection":
                    if (it.value.isLong()) {
                        filtering.project << (it.value as Long)
                        filtering.enabled = true
                    }
                    break
                case "pidSearch":
                    if (it.value && it.value.length() >= 3) {
                        filtering.pid << it.value
                        filtering.enabled = true
                    }
                    break
                case "mockFullNameSearch":
                    if (it.value && it.value.length() >= 3) {
                        filtering.mockFullName << it.value
                        filtering.enabled = true
                    }
                    break
                case "mockPidSearch":
                    if (it.value && it.value.length() >= 3) {
                        filtering.mockPid << it.value
                        filtering.enabled = true
                    }
                    break
                case "typeSelection":
                    if (it.value) {
                        filtering.type << (it.value as Individual.Type)
                        filtering.enabled = true
                    }
                    break
            }
        }
        return filtering
    }
}
