package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.security.access.prepost.PreAuthorize

class EgaSubmissionService {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(params.project, 'OTP_READ_ACCESS')")
    Submission createSubmission(Map params) {
        Submission submission = new Submission( params + [
                state: Submission.State.SELECTION,
        ])
        assert submission.save(flush: true, failOnError: true)

        return submission
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateSubmissionState (Submission submission, Submission.State state) {
        submission.state = state
        submission.save(flush: true)
    }

    List<SeqType> seqTypeByProject(Project project) {
        List<Long> seqTypeIds = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqTypeId")
            }
        }
        List<SeqType> seqTypes = []
        if (seqTypeIds) {
            seqTypes = SeqType.withCriteria {
                'in'("id", seqTypeIds)
                order("name")
                order("libraryLayout")
            }
        }
        return seqTypes
    }

    void saveSampleSubmissionObject(Submission submission, Sample sample, SeqType seqType) {
        SampleSubmissionObject sampleSubmissionObject = new SampleSubmissionObject(
                sample: sample,
                seqType: seqType
        ).save(flush: true)
        submission.addToSamplesToSubmit(sampleSubmissionObject)
    }
}
