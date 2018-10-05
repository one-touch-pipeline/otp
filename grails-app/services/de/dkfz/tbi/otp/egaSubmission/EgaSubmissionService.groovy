package de.dkfz.tbi.otp.egaSubmission

import org.springframework.security.access.prepost.PreAuthorize

class EgaSubmissionService {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(params.project, 'OTP_READ_ACCESS')")
    Submission createSubmission(Map params) {
        Submission submission = new Submission( params + [
                state: Submission.State.SELECTION
        ])
        assert submission.save(flush: true, failOnError: true)

        return submission
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateSubmissionState (Submission submission, Submission.State state) {
        submission.state = state
        submission.save(flush: true)
    }

}
