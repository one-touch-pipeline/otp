package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.submissions.ega.*
import grails.test.spock.*
import spock.lang.*

class EgaSubmissionServiceIntegrationSpec extends IntegrationSpec implements EgaSubmissionFactory {

    EgaSubmissionService egaSubmissionService = new EgaSubmissionService()

    @Unroll
    void "test check file types with bam file"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile(
                withdrawn: withdrawn,
                fileOperationStatus: status,
        )
        SampleSubmissionObject sampleSubmissionObject
        if (withFile) {
            sampleSubmissionObject = createSampleSubmissionObject(
                    sample: roddyBamFile.sample,
                    seqType: roddyBamFile.seqType,
            )
        } else {
            sampleSubmissionObject = createSampleSubmissionObject()
        }

        EgaSubmission submission = createSubmission(
                project: roddyBamFile.project
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        submission.save(flush: true)

        when:
        Map map = egaSubmissionService.checkBamFiles(submission)

        then:
        map.get(sampleSubmissionObject) == result

        where:
        withFile     | withdrawn | result | status
        true         | false     | true   | AbstractMergedBamFile.FileOperationStatus.PROCESSED
        false        | false     | false  | AbstractMergedBamFile.FileOperationStatus.PROCESSED
        true         | true      | false  | AbstractMergedBamFile.FileOperationStatus.PROCESSED
        true         | false     | false  | AbstractMergedBamFile.FileOperationStatus.INPROGRESS
    }
}
