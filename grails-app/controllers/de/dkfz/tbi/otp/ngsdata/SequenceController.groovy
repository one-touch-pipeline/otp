package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import groovy.json.JsonSlurper
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import de.dkfz.tbi.otp.utils.DataTableCommand


class SequenceController {
    def seqTrackService
    def projectService
    def servletContext
    def fastqcResultsService

    def index() {
        List<SeqType> seqTypes = SeqType.list(sort: "name", order: "asc")
        [projects: projectService.getAllProjects(),
            sampleTypes: SampleType.list(sort: "name", order: "asc"),
            seqTypes: new TreeSet(seqTypes.collect { it.aliasOrName }),
            libraryLayouts: new TreeSet(seqTypes.collect { it.libraryLayout }),
            seqCenters: SeqCenter.list(sort: "name", order: "asc")
        ]
    }

    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        SequenceFiltering filtering = SequenceFiltering.fromJSON(params.filtering)

        dataToRender.iTotalRecords = seqTrackService.countSequences(filtering)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        List<Sequence> sequences = seqTrackService.listSequences(cmd.iDisplayStart, cmd.iDisplayLength, cmd.sortOrder, SequenceSortColumn.fromDataTable(cmd.iSortCol_0), filtering)
        List<DataFile> dataFiles = fastqcResultsService.fastQCFiles(sequences)
        // need to add an additional field to the sequences
        // if added as dynamic property, it is not included during the JSON conversion
        // because of that, we just copy all properties into a map
        sequences.each { Sequence seq ->
            Map data = [:]
            seq.domainClass.persistentProperties.each {
                data.put(it.name, seq[it.name])
            }
            dataToRender.aaData << data
        }
        // add the DataFiles to the sequences
        dataFiles.each { DataFile df ->
            for (def sequence in dataToRender.aaData) {
                if (df.seqTrack.id == sequence.seqTrackId) {
                    if (sequence.containsKey("fastQCFiles")) {
                        sequence.fastQCFiles << df
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
                row.seqTypeAliasOrName,
                row.libraryLayout,
                row.seqCenterName,
                row.name,
                row.laneId,
                row.alignmentState.toString(),
                row.dateCreated?.format('EEE MMM dd yyyy')
            ].join(",")
        }.join("\n")
        def contentHeader = [
                'Project',
                'Individual',
                'Sample Type',
                'Sequence Type',
                'Library Layout',
                'Sequence Center',
                'Run',
                'Lane',
                'OTP Alignment',
                'Run Date'
                ].join(',')
        def content = "${contentHeader}\n${contentBody}\n"
        response.setContentType("application/octet-stream")
        response.setHeader("Content-disposition", "filename=sequence_export.csv")
        response.outputStream << content.toString().bytes
    }

    def exportCsv = {
        SequenceFiltering filtering = SequenceFiltering.fromJSON(params.filtering)
        String xml = seqTrackService.performXMLExport(filtering)

        // transform XML into CSV
        TransformerFactory factory = TransformerFactory.newInstance()
        Transformer transformer = factory.newTransformer(new StreamSource(new File(servletContext.getRealPath("xslt/sequencetocsv.xslt"))))
        StringWriter plainText = new StringWriter()
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(plainText))

        // make response a file for download
        response.setContentType("application/octet-stream")
        response.setHeader("Content-disposition", "filename=sequence_export.csv")
        response.outputStream << plainText.toString().bytes
    }
}

enum SequenceSortColumn {
    PROJECT("projectId"),
    INDIVIDUAL("mockPid"),
    SAMPLE_TYPE("sampleTypeName"),
    SEQ_TYPE("seqTypeName"),
    LIBRARY_LAYOUT("libraryLayout"),
    SEQ_CENTER("seqCenterName"),
    RUN("name"),
    LANE("laneId"),
    FASTQC("fastqcState"),
    ALIGNMENT("alignmentState"),
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
                return SequenceSortColumn.RUN
            case 7:
                return SequenceSortColumn.LANE
            case 8:
                return SequenceSortColumn.FASTQC
            case 9:
                return SequenceSortColumn.ALIGNMENT
            case 10:
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
    List<Long> project = []
    List<String> individual = []
    List<Long> sampleType = []
    List<String> seqType = []
    List<String> libraryLayout = []
    List<Long> seqCenter = []
    List<String> run = []

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
                    if (it.value.isLong()) {
                        filtering.project << (it.value as Long)
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
            }
        }
        return filtering
    }
}
