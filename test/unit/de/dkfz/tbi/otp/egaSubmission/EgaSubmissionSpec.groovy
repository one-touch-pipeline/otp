package de.dkfz.tbi.otp.egaSubmission

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.submissions.ega.EgaSubmissionFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

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
        EgaSubmission,
])
class EgaSubmissionSpec extends Specification implements EgaSubmissionFactory {

    void "test link Submission and files"() {
        given:
        EgaSubmission submission = createSubmission()
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
