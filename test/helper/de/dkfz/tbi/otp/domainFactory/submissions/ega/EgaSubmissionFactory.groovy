package de.dkfz.tbi.otp.domainFactory.submissions.ega

import de.dkfz.tbi.otp.domainFactory.*
import de.dkfz.tbi.otp.domainFactory.pipelines.*
import de.dkfz.tbi.otp.egaSubmission.*
import de.dkfz.tbi.otp.ngsdata.*

trait EgaSubmissionFactory implements IsRoddy, DomainFactoryCore {

    EgaSubmission createSubmission(Map properties = [:]) {
        return createDomainObject(EgaSubmission, [
                project       : { createProject() },
                egaBox        : "egaBox",
                submissionName: "submissionName",
                studyName     : "studyName",
                studyType     : EgaSubmission.StudyType.CANCER_GENOMICS,
                studyAbstract : "studyAbstract",
                pubMedId      : "pubMedId",
                state         : EgaSubmission.State.SELECTION,
                selectionState: EgaSubmission.SelectionState.SELECT_SAMPLES,
        ], properties)
    }

    BamFileSubmissionObject createBamFileSubmissionObject(Map properties = [:]) {
        return createDomainObject(BamFileSubmissionObject, [
                egaAliasName: "bam_file_alias",
                bamFile: { createBamFile() },
                sampleSubmissionObject: { createSampleSubmissionObject() },
        ], properties)
    }

    DataFileSubmissionObject createDataFileSubmissionObject (Map properties = [:]) {
        return createDomainObject(DataFileSubmissionObject , [
                egaAliasName: "data_file_alias",
                dataFile: { DomainFactory.createDataFile() },
                sampleSubmissionObject: { createSampleSubmissionObject() },
        ], properties)
    }

    SampleSubmissionObject createSampleSubmissionObject(Map properties = [:]) {
        return createDomainObject(SampleSubmissionObject, [
                egaAliasName: "sample_alias${nextId}",
                sample: { createSample() },
                seqType: { createSeqType() },
        ], properties)
    }
}
