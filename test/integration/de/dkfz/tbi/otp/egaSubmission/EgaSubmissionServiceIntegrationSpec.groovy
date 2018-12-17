package de.dkfz.tbi.otp.egaSubmission

import grails.test.spock.IntegrationSpec
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.domainFactory.submissions.ega.EgaSubmissionFactory

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

    void "test create bam file submission objects"() {
        given:
        EgaSubmission submission = createSubmission()
        RoddyBamFile bamFile = createBamFile()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject(
                sample: bamFile.sample,
                seqType: bamFile.seqType,
                useBamFile: true,
        )
        submission.addToSamplesToSubmit(sampleSubmissionObject)

        when:
        egaSubmissionService.createBamFileSubmissionObjects(submission)

        then:
        submission.bamFilesToSubmit.size() == BamFileSubmissionObject.findAll().size()
    }
}
