package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import java.text.DecimalFormat
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholdsService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates
import de.dkfz.tbi.otp.utils.DataTableCommand



class SampleTypeCombinationPerIndividualController {


    ProjectService projectService

    SampleTypeCombinationPerIndividualService sampleTypeCombinationPerIndividualService

    ProcessingThresholdsService processingThresholdsService
    IndividualService individualService


    def index = {
        Individual ind = individualService.getIndividualByMockPid(params.mockPid)
        if (!ind) {
            response.sendError(404)
            return
        }
        [
            individual: ind
        ]
    }

    def dataTableSNVFinishedSamplePairs(DataTableCommand cmd) {

        Individual individual = individualService.getIndividual(params.id)
        Map dataToRender = cmd.dataToRender()
        List data = sampleTypeCombinationPerIndividualService.finishedSamplePairs(individual)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        data.each { SampleTypeCombinationPerIndividual sampleTypeCombination ->
            SnvCallingInstance snvCallingInstance = SnvCallingInstance.findBySampleTypeCombinationAndProcessingState(sampleTypeCombination, SnvProcessingStates.FINISHED, [sort: 'id', order: 'desc'])
            dataToRender.aaData << [
                sampleType1: sampleTypeCombination.sampleType1.name,
                sampleType2: sampleTypeCombination.sampleType2.name,
                seqType: sampleTypeCombination.seqType.aliasOrName,
                sampleTypeCombinationPath: sampleTypeCombination.sampleTypeCombinationPath.getAbsoluteDataManagementPath().getAbsolutePath(),
                lastUpdated: snvCallingInstance.lastUpdated?.format("yyyy-MM-dd")
            ]
        }
        render dataToRender as JSON
    }

    def dataTableSNVInprogressSamplePairs(DataTableCommand cmd) {
        params.id = 186 //TODO only test, I will remove it before pushing the code.

        Individual individual = individualService.getIndividual(params.id)
        Map dataToRender = cmd.dataToRender()
        List data = sampleTypeCombinationPerIndividualService.progressingSamplePairs(individual)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        data.each { SampleTypeCombinationPerIndividual sampleTypeCombination ->
            SnvCallingInstance snvCallingInstance = SnvCallingInstance.findBySampleTypeCombinationAndProcessingState(sampleTypeCombination, SnvProcessingStates.IN_PROGRESS, [sort: 'id', order: 'desc'])
            dataToRender.aaData << [
                sampleType1: sampleTypeCombination.sampleType1.name,
                sampleType2: sampleTypeCombination.sampleType2.name,
                seqType: sampleTypeCombination.seqType.aliasOrName,
                dateCreated: snvCallingInstance.dateCreated?.format("yyyy-MM-dd")
            ]
        }
        render dataToRender as JSON
    }
    def dataTableSNVNotStartedSamplePairs(DataTableCommand cmd) {
        params.id = 186 //TODO only test, I will remove it before pushing the code.
        Individual individual = individualService.getIndividual(params.id)
        Map dataToRender = cmd.dataToRender()
        List data = sampleTypeCombinationPerIndividualService.notStartedSamplePairs(individual)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        data.each {SampleTypeCombinationPerIndividual sampleTypeCombination ->
            ProcessingThresholds threshold1 = processingThresholdsService.findByProjectAndSampleTypeAndSeqType(sampleTypeCombination.project, sampleTypeCombination.sampleType1, sampleTypeCombination.seqType)
            ProcessingThresholds threshold2 = processingThresholdsService.findByProjectAndSampleTypeAndSeqType(sampleTypeCombination.project, sampleTypeCombination.sampleType2, sampleTypeCombination.seqType)
            ProcessedMergedBamFile processedMergedBamFile1 = sampleTypeCombination.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeCombination.sampleType1)
            ProcessedMergedBamFile processedMergedBamFile2 = sampleTypeCombination.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeCombination.sampleType2)
            def tmp = [
                sampleType1: sampleTypeCombination?.sampleType1?.name,
                sampleType2: sampleTypeCombination?.sampleType2?.name,
                seqType: sampleTypeCombination?.seqType?.aliasOrName,
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
        params.id = 186 //TODO only test, I will remove it before pushing the code.

        Individual individual = individualService.getIndividual(params.id)
        Map dataToRender = cmd.dataToRender()
        List data = sampleTypeCombinationPerIndividualService.disablesSamplePairs(individual)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        data.each { SampleTypeCombinationPerIndividual sampleTypeCombination ->
            dataToRender.aaData << [
                sampleType1: sampleTypeCombination.sampleType1.name,
                sampleType2: sampleTypeCombination.sampleType2.name,
                seqType: sampleTypeCombination.seqType.aliasOrName,
            ]
        }
        render dataToRender as JSON
    }

}
