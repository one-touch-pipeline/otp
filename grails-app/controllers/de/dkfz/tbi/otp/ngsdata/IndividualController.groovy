package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import grails.converters.*
import groovy.json.*
import groovy.transform.*
import org.springframework.validation.*
import org.springframework.web.multipart.*

class IndividualController {

    static final String PID = "PID"
    static final String SAMPLE_TYPE = "SAMPLE_TYPE"
    static final String SAMPLE_IDENTIFIER = "SAMPLE_IDENTIFIER"

    IndividualService individualService
    ProjectService projectService
    CommentService commentService
    SampleIdentifierService sampleIdentifierService

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
            comment: ind.comment,
            typeDropDown: Individual.Type.values(),
            sampleTypeDropDown: individualService.getSampleTypeNames()
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

    def insertMany(InsertManyCommand cmd) {
        String message = ''
        String messageType = ''

        if (cmd.submit) {
            String validate = cmd.validate()
            if (validate.empty) {
                try {
                    String content
                    if (!cmd.sampleFile.empty) {
                        content = new String(cmd.sampleFile.bytes)
                    } else {
                        content = cmd.sampleText
                    }
                    String delimiter = cmd.delimiter == "space" ? ' ' : cmd.delimiter == "tab" ? '\t' : cmd.delimiter
                    content = content.replaceAll(/ *${delimiter} */, delimiter)

                    Spreadsheet spreadsheet = new Spreadsheet(content, delimiter as char)
                    if (!spreadsheet.getColumn(PID)) {
                        message = "Error: The column ${PID} does not exist"
                        messageType = 'error'
                    } else if (!spreadsheet.getColumn(SAMPLE_TYPE)) {
                        message = "Error: The column ${SAMPLE_TYPE} does not exist"
                        messageType = 'error'
                    } else if (!spreadsheet.getColumn(SAMPLE_IDENTIFIER)) {
                        message = "Error: The column ${SAMPLE_IDENTIFIER} does not exist"
                        messageType = 'error'
                    } else {
                        spreadsheet.dataRows.each { Row row ->
                            ParsedSampleIdentifier s = new DefaultParsedSampleIdentifier(
                                    projectName: cmd.project,
                                    pid: row.getCellByColumnTitle(PID).text.trim(),
                                    sampleTypeDbName: row.getCellByColumnTitle(SAMPLE_TYPE).text.trim(),
                                    fullSampleName: row.getCellByColumnTitle(SAMPLE_IDENTIFIER).text.trim(),
                            )
                            sampleIdentifierService.findOrSaveSampleIdentifier(s)
                        }
                        message = 'Creation was successful'
                        messageType = 'info'
                    }
                } catch (Throwable t) {
                    log.error(t.message, t)
                    message = "Error: The creation failed for ${t.message.replaceAll('\n', ' ')}"
                    messageType = 'error'
                }
            } else {
                message = "Error: The input was not valid: ${validate}"
                messageType = 'error'
            }
        }

        return [
                projects: projectService.allProjects*.name,
                delimiters: [',', 'space', ';', 'tab'],
                message: message,
                messageType: messageType,
                oldProject: cmd?.project,
                oldContent: cmd?.sampleText ?: "${PID},${SAMPLE_TYPE},${SAMPLE_IDENTIFIER}\n",
                oldDelimiter: cmd?.delimiter,
        ]
    }

    def insert() {
        List<Project> projects = projectService.getAllProjects()
        List<Individual.Type> individualTypes = Individual.Type.values()
        List<String> sampleTypes = individualService.getSampleTypeNames()
        [projects: projects, individualTypes: individualTypes, sampleTypes: sampleTypes]
    }

    def save(IndividualCommand cmd) {
        if (cmd.hasErrors()) {
            FieldError errors = cmd.errors.getFieldError()
            Map data = [success: false, error: "'${errors.getRejectedValue()}' is not a valid value for '${errors.getField()}'. Error code: '${errors.getField()}' already exists."]
            render data as JSON
            return
        }

        try {
            List<SamplesParser> parsedSamples = new SamplesParser().insertSamplesFromJSON(cmd.samples)
            Individual individual = individualService.createIndividual(projectService.getProject(cmd.project), cmd, parsedSamples)
            Map data = [success: true, id: individual.id]
            render data as JSON
        } catch (Throwable e) {
            Map data = [error: e.message]
            render data as JSON
        }
    }

    def updateField(UpdateFieldCommand cmd) {
        Individual individual = individualService.getIndividual(cmd.id)
        if (!individual) {
            Map data = [error: g.message(code: "individual.update.notFound", args: [cmd.id])]
            render data as JSON
            return
        }
        Map data = [:]
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
            Map data = [error: g.message(code: "individual.update.notFound", args: [cmd.id])]
            render data as JSON
            return
        }
        Map data = [:]
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
            Map data = [error: g.message(code: "individual.update.notFound", args: [cmd.id])]
            render data as JSON
            return
        }
        Map data = [:]
        try {
            individualService.createSample(individual, cmd.value)
            data.put("success", true)
        } catch (Exception e) {
            data = [error: e.message]
        }
        render data as JSON
    }

    def retrieveSampleIdentifiers(RetrieveSampleIdentifiersCommand cmd) {
        render individualService.getSampleIdentifiers(cmd.id, cmd.sampleType) as JSON
    }

    // params.id, params.comment, date
    def saveIndividualComment(CommentCommand cmd) {
        Individual individual = individualService.getIndividual(cmd.id)
        commentService.saveComment(individual, cmd.comment)
        def dataToRender = [date: individual.comment.modificationDate.format('EEE, d MMM yyyy HH:mm'), author: individual.comment.author]
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
    IndividualService individualService
    ProjectService projectService

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
    IndividualService individualService

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
    IndividualService individualService

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
    IndividualService individualService

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
        boolean hasType = value.type
        boolean hasIdentifier = value.identifier.any { !it.empty }

        assert hasType == hasIdentifier : "Please fill in both fields: Sample Type and Sample Identifier"

        return hasType
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

@ToString(includeNames=true)
class InsertManyCommand {
    String submit
    String project
    String delimiter
    String sampleText
    MultipartFile sampleFile

    String validate() {
        if (!submit) {
            return ''
        }
        if (!project) {
            return 'Please select a project'
        }
        if (!delimiter) {
            return 'Please select a delimiter'
        }
        if (!sampleFile.empty) {
            return ''
        }
        List<String> sampleMapping = sampleText.readLines()
        if (sampleMapping && sampleMapping.size()>1 && sampleMapping[1].size() > 0) {
            return ''
        }
        return 'Neither file nor text provided'
    }
}
