package de.dkfz.tbi.otp.qcTrafficLight

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.tracking.TrackingService

import static de.dkfz.tbi.otp.qcTrafficLight.QcThreshold.ThresholdLevel.ERROR

class QcTrafficLightService {

    CommentService commentService
    ConfigService configService
    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService

    TrackingService trackingService


    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#bamFile?.project, 'OTP_READ_ACCESS')")
    void setQcTrafficLightStatusWithComment(AbstractMergedBamFile bamFile, AbstractMergedBamFile.QcTrafficLightStatus qcTrafficLightStatus, String comment) {
        assert bamFile: "the bamFile must not be null"
        assert qcTrafficLightStatus: "the qcTrafficLightStatus must not be null"
        assert comment: "the comment must not be null"

        commentService.saveComment(bamFile, comment)
        setQcTrafficLightStatus(bamFile, qcTrafficLightStatus)
        if (bamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED) {
            trackingService.findAllOtrsTickets(bamFile.containedSeqTracks).each {
                trackingService.resetAnalysisNotification(it)
            }
            if (bamFile.seqType.isRna()) {
                linkFilesToFinalDestinationService.linkNewRnaResults((RnaRoddyBamFile) bamFile, configService.getDefaultRealm())
            } else {
                linkFilesToFinalDestinationService.linkNewResults(bamFile, configService.getDefaultRealm())
            }
        }
    }

    private void setQcTrafficLightStatus(AbstractMergedBamFile bamFile, AbstractMergedBamFile.QcTrafficLightStatus qcTrafficLightStatus) {
        bamFile.qcTrafficLightStatus = qcTrafficLightStatus
        assert bamFile.save(flush: true)
    }

    void setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(BamFilePairAnalysis bamFilePairAnalysis, QcTrafficLightValue qc) {
        [bamFilePairAnalysis.sampleType1BamFile, bamFilePairAnalysis.sampleType2BamFile].each {
            setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(it, qc)
        }
    }

    void setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(AbstractMergedBamFile bamFile, QcTrafficLightValue qc) {
        if (bamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED) {
            return
        }

        Project project = bamFile.getProject()

        if (!project.qcThresholdHandling.checksThreshold) {
            setQcTrafficLightStatus(bamFile, AbstractMergedBamFile.QcTrafficLightStatus.UNCHECKED)
            return
        }

        AbstractMergedBamFile.QcTrafficLightStatus qcStatus
        if (qcValuesExceedErrorThreshold(bamFile, qc)) {
            if (project.qcThresholdHandling.blocksBamFile) {
                qcStatus = AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED
                commentService.saveCommentAsOtp(bamFile, "Bam file exceeded threshold")
            } else {
                qcStatus = AbstractMergedBamFile.QcTrafficLightStatus.AUTO_ACCEPTED
            }
        } else {
            qcStatus = AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        }

        setQcTrafficLightStatus(bamFile, qcStatus)
    }

    private boolean qcValuesExceedErrorThreshold(AbstractMergedBamFile bamFile, QcTrafficLightValue qc) {
        return QcThreshold.getValidQcPropertyForQcClass(qc.class.name).any { String property ->
            QcThreshold qcThreshold =
                    QcThreshold.findByQcClassAndSeqTypeAndQcProperty1AndProject(qc.class.name, bamFile.seqType, property, bamFile.project) ?:
                            QcThreshold.findByQcClassAndSeqTypeAndQcProperty1AndProjectIsNull(qc.class.name, bamFile.seqType, property)
            return qcThreshold?.qcPassed(qc) == ERROR
        }
    }
}
