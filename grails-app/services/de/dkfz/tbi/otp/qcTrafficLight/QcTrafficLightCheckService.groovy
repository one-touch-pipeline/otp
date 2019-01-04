package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*

class QcTrafficLightCheckService {

    QcTrafficLightNotificationService qcTrafficLightNotificationService

    void handleQcCheck(AbstractMergedBamFile bamFile, Closure callbackIfAllFine) {
        switch (bamFile.qcTrafficLightStatus?.jobLinkCase) {
            case null:
            case AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.CREATE_LINKS:
                callbackIfAllFine()
                break
            case AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.CREATE_NO_LINK:
                if (bamFile.project.qcThresholdHandling.notifiesUser) {
                    qcTrafficLightNotificationService.informResultsAreBlocked(bamFile)
                }
                break
            case AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.SHOULD_NOT_OCCUR:
                throw new OtpRuntimeException("${bamFile.qcTrafficLightStatus} is not a valid qcTrafficLightStatus " +
                        "during workflow processing, it should only occur after the workflow has finished")
            default:
                assert false: "Unknown value: ${bamFile.qcTrafficLightStatus?.jobLinkCase}"
        }
    }
}

