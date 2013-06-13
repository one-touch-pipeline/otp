package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SavingException
import org.codehaus.groovy.grails.web.json.JSONObject
import grails.converters.JSON

class QualityAssessmentProcessingService {

    def processedBamFileQaFileService

    ProcessedBamFile bamFileReadyForQa() {
//        def status = AbstractBamFile.QaProcessingStatus.NOT_STARTED
//        return ProcessedBamFile.findByQualityAssessmentStatus(status)
        return ProcessedBamFile.findByQualityAssessmentStatus(AbstractBamFile.QaProcessingStatus.NOT_STARTED)
    }

    void setQaInProcessing(ProcessedBamFile processedBamFile) {
//        def status = AbstractBamFile.QaProcessingStatus.IN_PROGRESS
//        processedBamFile.qualityAssessmentStatus = status
        processedBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.IN_PROGRESS
        safeSave(processedBamFile)
    }

    void setQaFinished(ProcessedBamFile processedBamFile) {
//        def status = AbstractBamFile.QaProcessingStatus.FINISHED
//        processedBamFile.qualityAssessmentStatus = status
        processedBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
        safeSave(processedBamFile)
    }

    void parseQaStatistics(ProcessedBamFile processedBamFile) {
        String qualityAssessmentDataFilePath = processedBamFileQaFileService.qualityAssessmentDataFilePath(processedBamFile)
        // TODO maybe this part should go to a service.. please comment..
        File file = new File(qualityAssessmentDataFilePath)
        JSONObject json = JSON.parse(file.text)
//        def chromosomes = json.keys()
        Iterator chromosomes = json.keys()
        AbstractQualityAssessment qualityAssessmentStatistics
        chromosomes.each { String chromosome ->
            // TODO This "ALL" string falling from the sky deserved a constant a long time ago...
            if (chromosome == "ALL") {
                qualityAssessmentStatistics = new OverallQualityAssessment(json.get(chromosome))
            } else {
                qualityAssessmentStatistics = new ChromosomeQualityAssessment(json.get(chromosome))
            }
            qualityAssessmentStatistics.abstractBamFile = processedBamFile
            safeSave(qualityAssessmentStatistics)
        }
    }

    void safeSave(def obj) {
        if (!obj.save(flush: true)) {
            throw new SavingException(this.class.name)
        }
    }
}
