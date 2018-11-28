package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.domainFactory.submissions.ega.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        AbstractMergedBamFile,
        BamFileSubmissionObject,
        DataFile,
        DataFileSubmissionObject,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        Project,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SampleSubmissionObject,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
        Submission,
])
class SubmissionSpec extends Specification implements EgaSubmissionFactory {

    void "test link Submission and files"() {
        given:
        Submission submission = createSubmission()
        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject()
        DataFileSubmissionObject dataFileSubmissionObject = createDataFileSubmissionObject()
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject()

        when:
        submission.addToBamFilesToSubmit(bamFileSubmissionObject)
        submission.addToDataFilesToSubmit(dataFileSubmissionObject)
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        submission.save(flush: true)

        then:
        CollectionUtils.exactlyOneElement(submission.bamFilesToSubmit) == bamFileSubmissionObject
        CollectionUtils.exactlyOneElement(submission.dataFilesToSubmit) == dataFileSubmissionObject
        CollectionUtils.exactlyOneElement(submission.samplesToSubmit) == sampleSubmissionObject
    }
}
