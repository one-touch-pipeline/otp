package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SavingException

class QualityAssessmentProcessingService {

    AbstractBamFile bamFileReadyForQa() {
        def status = AbstractBamFile.QaProcessingStatus.NOT_STARTED
        return AbstractBamFile.findByQualityAssessmentStatus(status)
    }

    void setQaInProcessing(AbstractBamFile bamFile) {
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
