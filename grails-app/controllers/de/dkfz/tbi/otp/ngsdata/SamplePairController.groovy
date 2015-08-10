package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import grails.converters.JSON
import java.text.DecimalFormat
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholdsService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates
import de.dkfz.tbi.otp.utils.DataTableCommand



class SamplePairController {


    ProjectService projectService

    SamplePairService samplePairService

    ProcessingThresholdsService processingThresholdsService
    IndividualService individualService


    def index = {
        Individual individual = individualService.getIndividualByMockPid(params.mockPid)
        if (!individual) {
            response.sendError(404)
            return
        }
        [
            individual: individual
        ]
    }

    def dataTableSNVFinishedSamplePairs(DataTableCommand cmd) {
        Individual individual = individualService.getIndividualByMockPid(params.mockPid)
        Map dataToRender = cmd.dataToRender()
        List data = samplePairService.finishedSamplePairs(individual)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        data.each { SamplePair samplePair ->
            SnvCallingInstance snvCallingInstance = SnvCallingInstance.findBySamplePairAndProcessingState(samplePair, SnvProcessingStates.FINISHED, [sort: 'id', order: 'desc'])
            dataToRender.aaData << [
                sampleType1: samplePair.sampleType1.name,
                sampleType2: samplePair.sampleType2.name,
                seqType: samplePair.seqType.aliasOrName,
                samplePairPath: samplePair.samplePairPath.getAbsoluteDataManagementPath().getAbsolutePath(),
                lastUpdated: snvCallingInstance.lastUpdated?.format("yyyy-MM-dd")
            ]
        }
        render dataToRender as JSON
    }

    def dataTableSNVInprogressSamplePairs(DataTableCommand cmd) {
        Individual individual = individualService.getIndividualByMockPid(params.mockPid)
        Map dataToRender = cmd.dataToRender()
        List data = samplePairService.progressingSamplePairs(individual)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        data.each { SamplePair samplePair ->
            SnvCallingInstance snvCallingInstance = SnvCallingInstance.findBySamplePairAndProcessingState(samplePair, SnvProcessingStates.IN_PROGRESS, [sort: 'id', order: 'desc'])
            dataToRender.aaData << [
                sampleType1: samplePair.sampleType1.name,
                sampleType2: samplePair.sampleType2.name,
                seqType: samplePair.seqType.aliasOrName,
                dateCreated: snvCallingInstance.dateCreated?.format("yyyy-MM-dd")
            ]
        }
        render dataToRender as JSON
    }
    def dataTableSNVNotStartedSamplePairs(DataTableCommand cmd) {
        Individual individual = individualService.getIndividualByMockPid(params.mockPid)
        Map dataToRender = cmd.dataToRender()
        List data = samplePairService.notStartedSamplePairs(individual)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        data.each {SamplePair samplePair ->
            ProcessingThresholds threshold1 = processingThresholdsService.findByProjectAndSampleTypeAndSeqType(samplePair.project, samplePair.sampleType1, samplePair.seqType)
            ProcessingThresholds threshold2 = processingThresholdsService.findByProjectAndSampleTypeAndSeqType(samplePair.project, samplePair.sampleType2, samplePair.seqType)
            AbstractMergedBamFile processedMergedBamFile1 = samplePair.mergingWorkPackage1.processableBamFileInProjectFolder
            AbstractMergedBamFile processedMergedBamFile2 = samplePair.mergingWorkPackage2.processableBamFileInProjectFolder
            def tmp = [
                sampleType1: samplePair?.sampleType1?.name,
                sampleType2: samplePair?.sampleType2?.name,
                seqType: samplePair?.seqType?.aliasOrName,
                laneCount1: valueHelper(processedMergedBamFile1?.numberOfMergedLanes, threshold1?.numberOfLanes),
                laneCount2: valueHelper(processedMergedBamFile2?.numberOfMergedLanes, threshold2?.numberOfLanes),
                coverage1: valueHelper(processedMergedBamFile1?.coverage, threshold1?.coverage),
                coverage2: valueHelper(processedMergedBamFile2?.coverage, threshold2?.coverage),
            ]
            dataToRender.aaData << tmp
        }
        render dataToRender as JSON
    }


    private String valueHelper(Number available, Number requested) {
        String ret = ''
        if (available == null) {
            ret += '0'
        } else {
            ret += formatNumber(available)
        }
        if (requested != null) {
            ret  += ' / ' + formatNumber(requested)
        }
        return ret
    }

    private String formatNumber(Number number) {
        if (number instanceof Double){
            DecimalFormat decimalFormat = new DecimalFormat('0.00')
            return decimalFormat.format(number)
        } else {
            return number as String
        }
    }

    def dataTableSNVProcessingDisabledSamplePairs(DataTableCommand cmd) {
        Individual individual = individualService.getIndividualByMockPid(params.mockPid)
        Map dataToRender = cmd.dataToRender()
        List data = samplePairService.disablesSamplePairs(individual)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        data.each { SamplePair samplePair ->
            dataToRender.aaData << [
                sampleType1: samplePair.sampleType1.name,
                sampleType2: samplePair.sampleType2.name,
                seqType: samplePair.seqType.aliasOrName,
            ]
        }
        render dataToRender as JSON
    }

}
