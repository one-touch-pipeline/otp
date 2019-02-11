package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile

class QcTrafficLightCheckService {

    QcTrafficLightNotificationService qcTrafficLightNotificationService

    void handleQcCheck(AbstractMergedBamFile bamFile, Closure callbackIfAllFine) {
        switch (bamFile.qcTrafficLightStatus?.jobLinkCase) {
            case null:
            case AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.CREATE_LINKS:
                callbackIfAllFine()
                break
            case AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.CREATE_NO_LINK:
                //no links creating, so nothing to do
                break
            case AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.SHOULD_NOT_OCCUR:
                throw new OtpRuntimeException("${bamFile.qcTrafficLightStatus} is not a valid qcTrafficLightStatus " +
                        "during workflow processing, it should only occur after the workflow has finished")
            default:
                throw new AssertionError("Unknown value: ${bamFile.qcTrafficLightStatus?.jobLinkCase}")
        }

        switch (bamFile.qcTrafficLightStatus?.jobNotifyCase) {
            case null:
            case AbstractMergedBamFile.QcTrafficLightStatus.JobNotifyCase.NO_NOTIFY:
                //no email sending, so nothing to do
                break
            case AbstractMergedBamFile.QcTrafficLightStatus.JobNotifyCase.NOTIFY:
                qcTrafficLightNotificationService.informResultsAreBlocked(bamFile)
                break
            case AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.SHOULD_NOT_OCCUR:
                throw new OtpRuntimeException("${bamFile.qcTrafficLightStatus} is not a valid qcTrafficLightStatus " +
                        "during workflow processing, it should only occur after the workflow has finished")
            default:
                throw new AssertionError("Unknown value: ${bamFile.qcTrafficLightStatus?.jobNotifyCase}")
        }
    }
}

