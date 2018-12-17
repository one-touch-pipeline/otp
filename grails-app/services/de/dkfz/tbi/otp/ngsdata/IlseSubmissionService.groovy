package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CommentService

class IlseSubmissionService {

    CommentService commentService

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<IlseSubmission> getSortedBlacklistedIlseSubmissions() {
        return IlseSubmission.findAllByWarning(true, [sort: 'ilseNumber', order: 'desc'])
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    boolean checkIfIlseNumberDoesNotExist(int ilseNumber) {
        return !IlseSubmission.findByIlseNumber(ilseNumber)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    IlseSubmission createNewIlseSubmission(int ilse, String comment) {
        IlseSubmission ilseSubmission = new IlseSubmission(
                ilseNumber: ilse,
                warning: true,
        )

        commentService.saveComment(ilseSubmission, comment)
        return  ilseSubmission
    }
}
