package de.dkfz.tbi.otp.domainfactory

import de.dkfz.tbi.otp.egaSubmission.BamFileSubmissionObject
import de.dkfz.tbi.otp.egaSubmission.DataFileSubmissionObject
import de.dkfz.tbi.otp.egaSubmission.SampleSubmissionObject
import de.dkfz.tbi.otp.egaSubmission.Submission
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class SubmissionDomainFactory extends AbstractDomainFactory {

    private SubmissionDomainFactory() {}

    static Submission createSubmission(Map properties = [:]) {
        return createDomainObject(Submission, [
                project       : { DomainFactory.createProject() },
                egaBox        : "egaBox",
                submissionName: "submissionName",
                studyName     : "studyName",
                studyType     : Submission.StudyType.CANCER_GENOMICS,
                studyAbstract : "studyAbstract",
                pubMedId      : "pubMedId",
                state         : Submission.State.SELECTION,
        ], properties)
    }

    static BamFileSubmissionObject createBamFileSubmissionObject(Map properties = [:]) {
        return createDomainObject(BamFileSubmissionObject, [
                egaAliasName: "bam_file_alias",
                bamFile: { DomainFactory.createRoddyBamFile() },
        ], properties)
    }

    static DataFileSubmissionObject createDataFileSubmissionObject (Map properties = [:]) {
        return createDomainObject(DataFileSubmissionObject , [
                egaAliasName: "data_file_alias",
                dataFile: { DomainFactory.createDataFile() },
        ], properties)
    }

    static SampleSubmissionObject createSampleSubmissionObject(Map properties = [:]) {
        return createDomainObject(SampleSubmissionObject, [
                egaAliasName: "sample_alias${DomainFactory.counter++}",
                sample: { DomainFactory.createSample() },
                seqType: { DomainFactory.createSeqType() },
        ], properties)
    }
}
