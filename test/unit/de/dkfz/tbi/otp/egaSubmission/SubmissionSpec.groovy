package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainfactory.SubmissionDomainFactory
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.RunSegment
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqCenter
import de.dkfz.tbi.otp.ngsdata.SeqPlatform
import de.dkfz.tbi.otp.ngsdata.SeqPlatformGroup
import de.dkfz.tbi.otp.ngsdata.SeqPlatformModelLabel
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SoftwareTool
import de.dkfz.tbi.otp.utils.CollectionUtils
import grails.test.mixin.Mock
import spock.lang.Specification

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
class SubmissionSpec extends Specification {

    void "test link Submission and files"() {
        given:
        Submission submission = SubmissionDomainFactory.createSubmission()
        BamFileSubmissionObject bamFileSubmissionObject = SubmissionDomainFactory.createBamFileSubmissionObject()
        DataFileSubmissionObject dataFileSubmissionObject = SubmissionDomainFactory.createDataFileSubmissionObject()
        SampleSubmissionObject sampleSubmissionObject = SubmissionDomainFactory.createSampleSubmissionObject()

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
