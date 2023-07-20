/*
 * Copyright 2011-2023 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.domainFactory.submissions.ega

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.IsPipeline
import de.dkfz.tbi.otp.egaSubmission.*

trait EgaSubmissionFactory implements IsPipeline, DomainFactoryCore {

    EgaSubmission createEgaSubmission(Map properties = [:]) {
        return createDomainObject(EgaSubmission, [
                project       : { createProject() },
                egaBox        : "egaBox_${nextId}",
                submissionName: "submissionName_${nextId}",
                studyName     : "studyName_${nextId}",
                studyType     : EgaSubmission.StudyType.CANCER_GENOMICS,
                studyAbstract : "studyAbstract_${nextId}",
                pubMedId      : "pubMedId_${nextId}",
                state         : EgaSubmission.State.SELECTION,
                selectionState: EgaSubmission.SelectionState.SELECT_SAMPLES,
        ], properties)
    }

    BamFileSubmissionObject createBamFileSubmissionObject(Map properties = [:]) {
        return createDomainObject(BamFileSubmissionObject, [
                egaAliasName          : "bam_file_alias_${nextId}",
                bamFile               : { createBamFile() },
                sampleSubmissionObject: { createSampleSubmissionObject() },
        ], properties)
    }

    RawSequenceFileSubmissionObject createRawSequenceFileSubmissionObject(Map properties = [:]) {
        return createDomainObject(RawSequenceFileSubmissionObject, [
                egaAliasName          : "data_file_alias_${nextId}",
                sequenceFile          : { createFastqFile() },
                sampleSubmissionObject: { createSampleSubmissionObject() },
        ], properties)
    }

    SampleSubmissionObject createSampleSubmissionObject(Map properties = [:]) {
        return createDomainObject(SampleSubmissionObject, [
                egaAliasName: "sample_alias_${nextId}",
                sample      : { createSample() },
                seqType     : { createSeqType() },
        ], properties)
    }
}
