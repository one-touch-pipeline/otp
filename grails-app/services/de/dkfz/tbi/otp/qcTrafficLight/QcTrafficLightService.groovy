package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import org.springframework.security.access.prepost.*

import static de.dkfz.tbi.otp.qcTrafficLight.QcThreshold.ThresholdLevel.ERROR

class QcTrafficLightService {

    CommentService commentService
    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#bamFile?.project, 'OTP_READ_ACCESS')")
    public changeQcTrafficLightStatusWithComment(AbstractMergedBamFile bamFile, AbstractMergedBamFile.QcTrafficLightStatus qcTrafficLightStatus, String comment) {
        assert bamFile: "the bamFile must not be null"
        assert qcTrafficLightStatus: "the qcTrafficLightStatus must not be null"
        assert comment: "the comment must not be null"

        commentService.saveComment(bamFile, comment)
        changeQcTrafficLightStatus(bamFile, qcTrafficLightStatus)
        if (bamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED) {
            linkFilesToFinalDestinationService.linkNewResults(bamFile, ConfigService.getDefaultRealm())
        }
    }

    private changeQcTrafficLightStatus(AbstractMergedBamFile bamFile, AbstractMergedBamFile.QcTrafficLightStatus qcTrafficLightStatus) {
        bamFile.qcTrafficLightStatus = qcTrafficLightStatus
        bamFile.save(flush:true)
    }

    public setQcTrafficLightStatusBasedOnThreshold(BamFilePairAnalysis bamFilePairAnalysis, QcTrafficLightValue qc) {
        [bamFilePairAnalysis.sampleType1BamFile, bamFilePairAnalysis.sampleType2BamFile].each {
            setQcTrafficLightStatusBasedOnThreshold(it, qc)
        }
    }

    public setQcTrafficLightStatusBasedOnThreshold(AbstractMergedBamFile bamFile, QcTrafficLightValue qc) {
        if (bamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED) {
            return
        }
        AbstractMergedBamFile.QcTrafficLightStatus qcStatus
        if (qcValuesExceedErrorThreshold(bamFile, qc)) {
            qcStatus = AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED
            commentService.saveCommentAsOtp(bamFile, "Bam file exceeded threshold")
        } else {
            qcStatus = AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        }
        changeQcTrafficLightStatus(bamFile, qcStatus)
        assert bamFile.save(flush: true)
    }

    private boolean qcValuesExceedErrorThreshold(AbstractMergedBamFile bamFile, QcTrafficLightValue qc) {
        return QcThreshold.getValidQcPropertyForQcClass(qc.class.name).any { String property ->
            QcThreshold.findByQcClassAndSeqTypeAndQcProperty1(qc.class.name, bamFile.seqType, property)?.qcPassed(qc) == ERROR
        }
    }
}
