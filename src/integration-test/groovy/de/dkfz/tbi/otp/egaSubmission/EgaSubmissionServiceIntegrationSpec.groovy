/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.egaSubmission

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.submissions.ega.EgaSubmissionFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType

@Rollback
@Integration
class EgaSubmissionServiceIntegrationSpec extends Specification implements EgaSubmissionFactory, IsRoddy {

    private final EgaSubmissionService egaSubmissionService = new EgaSubmissionService()

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

        EgaSubmission submission = createEgaSubmission(
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
        true         | true      | true   | AbstractMergedBamFile.FileOperationStatus.PROCESSED
        true         | false     | false  | AbstractMergedBamFile.FileOperationStatus.INPROGRESS
    }

    void "test create bam file submission objects"() {
        given:
        EgaSubmission submission = createEgaSubmission()
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

    void "test get experimental metadata"() {
        given:
        SampleSubmissionObject sampleSubmissionObject = createSampleSubmissionObject()
        SeqType seqType = createSeqType()
        DomainFactory.createSeqTrack(
                sample: sampleSubmissionObject.sample,
                seqType: seqType
        )
        sampleSubmissionObject.sample.refresh()
        EgaSubmission submission = createEgaSubmission(
                samplesToSubmit: [sampleSubmissionObject]
        )

        when:
        Map experimentalMetadata = egaSubmissionService.getExperimentalMetadata(submission)[0] as Map

        then:
        experimentalMetadata.libraryPreparationKit == sampleSubmissionObject.sample.seqTracks[0].libraryPreparationKit
        experimentalMetadata.libraryLayout == seqType.libraryLayout
        experimentalMetadata.displayName == seqType.displayName
    }
}
