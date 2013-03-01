package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SavingException

class QualityAssessmentProcessingService {

    ProcessedBamFile bamFileReadyForQa() {
        def status = AbstractBamFile.QaProcessingStatus.NOT_STARTED
        return ProcessedBamFile.findByQualityAssessmentStatus(status)
    }

    void setQaInProcessing(ProcessedBamFile bamFile) {
        def status = AbstractBamFile.QaProcessingStatus.IN_PROGRESS
        bamFile.qualityAssessmentStatus = status
        safeSave(bamFile)
    }

    void safeSave(def obj) {
        if (obj.save(flush: true)) {
            throw new SavingException(this.class.name)
        }
    }
}
