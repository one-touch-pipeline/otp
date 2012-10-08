package de.dkfz.tbi.otp.ngsdata

import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import grails.converters.JSON
import groovy.json.JsonSlurper


class SequenceController {
    def seqTrackService
    def projectService
    def servletContext

    def index() {
        List<SeqType> seqTypes = SeqType.list()
        [projects: projectService.getAllProjects(),
            sampleTypes: SampleType.list(),
            seqTypes: new HashSet(seqTypes.collect { it.name }),
            libraryLayouts: new HashSet(seqTypes.collect { it.libraryLayout }),
            seqCenters: SeqCenter.list()
            ]
    }

    def dataTableSource = {
        int start = 0
        int length = 10
        if (params.iDisplayStart) {
            start = params.iDisplayStart as int
        }
        if (params.iDisplayLength) {
            length = Math.min(100, params.iDisplayLength as int)
        }
        int column = 0
        if (params.iSortCol_0) {
            column = params.iSortCol_0 as int
        }
        def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData = []

        SequenceFiltering filtering = SequenceFiltering.fromJSON(params.filtering)

        dataToRender.offset = start
        dataToRender.iSortCol_0 = params.iSortCol_0
        dataToRender.sSortDir_0 = params.sSortDir_0
        dataToRender.iTotalRecords = seqTrackService.countSequences(filtering)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        dataToRender.aaData = seqTrackService.listSequences(start, length, params.sSortDir_0 == "asc", SequenceSortColumn.fromDataTable(column), filtering)
        render dataToRender as JSON
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
    ORIG_ALIGNMENT("hasOriginalBam"),
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
            return SequenceSortColumn.ORIG_ALIGNMENT
        case 11:
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
