package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.security.access.prepost.*

class QcTrafficLightService {

    CommentService commentService
    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#bamFile?.project, read)")
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

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#bamFile?.project, read)")
    private changeQcTrafficLightStatus(AbstractMergedBamFile bamFile, AbstractMergedBamFile.QcTrafficLightStatus qcTrafficLightStatus) {
        bamFile.qcTrafficLightStatus = qcTrafficLightStatus
        bamFile.save(flush:true)
    }
}
