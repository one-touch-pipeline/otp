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
package de.dkfz.tbi.otp.workflowTest.alignment.roddy.wgbs

import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.OtpWorkflow
import de.dkfz.tbi.otp.workflowExecution.decider.AbstractWorkflowDecider
import de.dkfz.tbi.otp.workflowExecution.decider.WgbsDecider
import de.dkfz.tbi.otp.workflowTest.alignment.roddy.AbstractRoddyAlignmentWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.referenceGenome.ReferenceGenomeHs37Conv
import de.dkfz.tbi.otp.workflowTest.roddy.RoddyFileAssertHelper

class WgbsAlignmentWorkflowSpec extends AbstractRoddyAlignmentWorkflowSpec implements ReferenceGenomeHs37Conv {

    WgbsDecider wgbsDecider

    Class<? extends OtpWorkflow> workflowComponentClass = WgbsWorkflow

    @Override
    protected AbstractWorkflowDecider getDecider() {
        return wgbsDecider
    }

    @Override
    String getWorkflowName() {
        return WgbsWorkflow.WORKFLOW
    }

    @Override
    protected SeqType findSeqType() {
        return SeqTypeService.wholeGenomeBisulfitePairedSeqType
    }

    @Override
    protected boolean isFastQcRequired() {
        return false
    }

    @Override
    protected void setUpFilesVariables() {
        testFastqFiles = [
                readGroup1: [
                        referenceDataDirectory.resolve('fastqFiles/wgbs/normal/paired/lib1/run1/sequence/gerald_D1VCPACXX_6_R1.fastq.bz2'),
                        referenceDataDirectory.resolve('fastqFiles/wgbs/normal/paired/lib1/run1/sequence/gerald_D1VCPACXX_6_R2.fastq.bz2'),
                ].asImmutable(),
                readGroup2: [
                        referenceDataDirectory.resolve('fastqFiles/wgbs/normal/paired/lib2/run1/sequence/gerald_D1VCPACXX_7_R1.fastq.bz2'),
                        referenceDataDirectory.resolve('fastqFiles/wgbs/normal/paired/lib2/run1/sequence/gerald_D1VCPACXX_7_R2.fastq.bz2'),
                ].asImmutable(),
        ].asImmutable()
    }

    void "test alignLanesOnly, no base bam file exists, one lane, with adapterTrimming, all fine"() {
        given:
        SessionUtils.withTransaction {
            createSeqTrack("readGroup1")
            setupUseAdapterTrimming()

            decide(1, 1)
        }

        when:
        execute(1, 1)

        then:
        verify_AlignLanesOnly_AllFine()
    }

    @Unroll
    void "test alignLanesOnly, no base bam file exists, two libraries #setLibrary, all fine"() {
        given:
        SeqTrack firstSeqTrack
        SeqTrack secondSeqTrack
        SessionUtils.withTransaction {
            if (setLibrary) {
                firstSeqTrack = createSeqTrack("readGroup1", [libraryName: "lib1"])
                secondSeqTrack = createSeqTrack("readGroup2", [libraryName: "lib5"])
            } else {
                firstSeqTrack = createSeqTrack("readGroup1")
                secondSeqTrack = createSeqTrack("readGroup2")
            }

            decide(2, 1)
        }

        when:
        execute(1, 2)

        then:
        verify_alignLanesOnly_NoBaseBamExist_TwoLanes(firstSeqTrack, secondSeqTrack)

        where:
        setLibrary | _
        true       | _
        false      | _
    }

    @Override
    protected void assertWorkflowFileSystemState(RoddyBamFile bamFile) {
        RoddyFileAssertHelper.assertFileSystemState(bamFile, roddyBamFileService)
    }

    @Override
    protected void assertWorkflowWorkDirectoryFileSystemState(RoddyBamFile bamFile, boolean isBaseBamFile) {
        RoddyFileAssertHelper.assertWorkDirectoryFileSystemState(bamFile, isBaseBamFile, roddyBamFileService, roddyConfigService)
    }
}
