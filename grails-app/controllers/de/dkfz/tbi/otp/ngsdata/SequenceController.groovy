package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ngsqc.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*
import groovy.json.*


class SequenceController {
    SeqTrackService seqTrackService
    ProjectService projectService
    FastqcResultsService fastqcResultsService

    def index() {
        List<SeqType> seqTypes = SeqType.list(sort: "name", order: "asc")
        [
            projects: projectService.getAllProjects(),
            sampleTypes: SampleType.list(sort: "name", order: "asc"),
            seqTypes: new TreeSet(seqTypes.collect { it.displayName }),
            libraryLayouts: new TreeSet(seqTypes.collect { it.libraryLayout }),
            seqCenters: SeqCenter.list(sort: "name", order: "asc"),
            libaryPreparationKits: LibraryPreparationKit.list(sort: "shortDisplayName", order: "asc").shortDisplayName,
        ]
    }

    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        SequenceFiltering filtering = SequenceFiltering.fromJSON(params.filtering)

        dataToRender.iTotalRecords = seqTrackService.countSequences(filtering)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        List<Sequence> sequences = seqTrackService.listSequences(cmd.iDisplayStart, cmd.iDisplayLength, cmd.sortOrder, SequenceSortColumn.fromDataTable(cmd.iSortCol_0), filtering)
        List<DataFile> dataFiles = sequences ? fastqcResultsService.fastQCFiles(sequences) : []
        // need to add an additional field to the sequences
        // if added as dynamic property, it is not included during the JSON conversion
        // because of that, we just copy all properties into a map
        sequences.each { Sequence seq ->
            Map data = [:]
            seq.domainClass.persistentProperties.each {
                data.put(it.name, seq[it.name])
            }
            // format date
            data.dateCreated = data.dateCreated.format("yyyy-MM-dd")

            data.withdrawn = SeqTrack.get(seq.seqTrackId).isWithdrawn()

            data.problemDescription = seq.problem?.description

            dataToRender.aaData << data
        }
        // add the DataFiles to the sequences
        dataFiles.each { DataFile df ->
            for (def sequence in dataToRender.aaData) {
                if (df.seqTrack.id == sequence.seqTrackId) {
                    if (sequence.containsKey("fastQCFiles")) {
                        df.mateNumber == 1 ? sequence.fastQCFiles = [df, sequence.fastQCFiles].flatten() : sequence.fastQCFiles << df
                    } else {
                        sequence.fastQCFiles = [df]
                    }
                }
            }
        }
        render dataToRender as JSON
    }

    def exportAll(DataTableCommand cmd) {
        SequenceFiltering filtering = SequenceFiltering.fromJSON(params.filtering)

        List<Sequence> sequences = seqTrackService.listSequences(0, -1, cmd.sortOrder, SequenceSortColumn.fromDataTable(cmd.iSortCol_0), filtering)

        def contentBody = sequences.collect { def row ->
            [
                    row.projectName,
                    row.mockPid,
                    row.sampleTypeName,
                    row.seqTypeDisplayName,
                    row.libraryLayout,
                    row.seqCenterName,
                    row.libraryPreparationKit,
                    row.name,
                    row.laneId,
                    row.libraryName,
                    row.ilseId,
                    row.problem?.name() ?: "",
                    row.dateCreated?.format("yyyy-MM-dd"),
            ].join(",")
        }.join("\n")
        def contentHeader = [
            'Project',
            'Individual',
            'Sample Type',
            'Sequence Type',
            'Library Layout',
            'Sequence Center',
            'Library Preparation Kit',
            'Run',
            'Lane',
            'Library',
            'ILSe',
            'Known issues',
            'Run Date'
        ].join(',')
        def content = "${contentHeader}\n${contentBody}\n"
        response.setContentType("application/octet-stream")
        response.setHeader("Content-disposition", "filename=sequence_export.csv")
        response.outputStream << content.toString().bytes
    }
}

enum SequenceSortColumn {
    PROJECT("projectName"),
    INDIVIDUAL("mockPid"),
    SAMPLE_TYPE("sampleTypeName"),
    SEQ_TYPE("seqTypeName"),
    LIBRARY_LAYOUT("libraryLayout"),
    SEQ_CENTER("seqCenterName"),
    LIBRARY_PREPARATION_KIT("libraryPreparationKit"),
    RUN("name"),
    LANE("laneId"),
    LIBRARY("libraryName"),
    FASTQC("fastqcState"),
    ILSEID("ilseId"),
    KNOWN_ISSUES("problem"),
    DATE("dateCreated")

    private final String columnName

    SequenceSortColumn(String column) {
        this.columnName = column
    }

    static SequenceSortColumn fromDataTable(int column) {
        switch (column) {
            case 0:
                return SequenceSortColumn.PROJECT
            case 1:
                return SequenceSortColumn.INDIVIDUAL
            case 2:
                return SequenceSortColumn.SAMPLE_TYPE
            case 3:
                return SequenceSortColumn.SEQ_TYPE
            case 4:
                return SequenceSortColumn.LIBRARY_LAYOUT
            case 5:
                return SequenceSortColumn.SEQ_CENTER
            case 6:
                return SequenceSortColumn.LIBRARY_PREPARATION_KIT
            case 7:
                return SequenceSortColumn.RUN
            case 8:
                return SequenceSortColumn.LANE
            case 9:
                return SequenceSortColumn.LIBRARY
            case 10:
                return SequenceSortColumn.FASTQC
            case 11:
                return SequenceSortColumn.ILSEID
            case 12:
                return SequenceSortColumn.KNOWN_ISSUES
            case 13:
                return SequenceSortColumn.DATE
            default:
                return SequenceSortColumn.PROJECT
        }
    }
}

/**
 * Class describing the various filtering to do on WorkPackage.
 *
 */
class SequenceFiltering {
    List<String> project = []
    List<String> individual = []
    List<Long> sampleType = []
    List<String> seqType = []
    List<String> libraryLayout = []
    List<Integer> ilseId = []
    List<Integer> seqCenter = []
    List<String> run = []
    List<String> libraryPreparationKit = []

    boolean enabled = false

    static SequenceFiltering fromJSON(String json) {
        SequenceFiltering filtering = new SequenceFiltering()
        if (!json) {
            return filtering
        }
        def slurper = new JsonSlurper()
        slurper.parseText(json).each {
            switch (it.type) {
                case "projectSelection":
                    if (it.value) {
                        filtering.project << it.value
                        filtering.enabled = true
                    }
                    break
                case "individualSearch":
                    if (it.value && it.value.length() >= 3) {
                        filtering.individual << it.value
                        filtering.enabled = true
                    }
                    break
                case "sampleTypeSelection":
                    if (it.value.isLong()) {
                        filtering.sampleType << (it.value as Long)
                        filtering.enabled = true
                    }
                    break
                case "seqTypeSelection":
                    if (it.value) {
                        filtering.seqType << it.value
                        filtering.enabled = true
                    }
                    break
                case "ilseIdSearch":
                    if (it.value) {
                        filtering.ilseId << (it.value as Integer)
                        filtering.enabled = true
                    }
                    break
                case "libraryLayoutSelection":
                    if (it.value) {
                        filtering.libraryLayout << it.value
                        filtering.enabled = true
                    }
                    break
                case "seqCenterSelection":
                    if (it.value.isLong()) {
                        filtering.seqCenter << (it.value as Long)
                        filtering.enabled = true
                    }
                    break
                case "runSearch":
                    if (it.value && it.value.length() >= 3) {
                        filtering.run << it.value
                        filtering.enabled = true
                    }
                    break
                case "libraryPreparationKitSelection":
                    if (it.value) {
                        filtering.libraryPreparationKit << it.value
                        filtering.enabled = true
                    }
                    break
            }
        }
        return filtering
    }
}
