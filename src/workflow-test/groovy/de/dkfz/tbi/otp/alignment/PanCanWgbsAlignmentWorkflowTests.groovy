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

package de.dkfz.tbi.otp.alignment

import org.junit.Ignore
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Ignore
class PanCanWgbsAlignmentWorkflowTests extends AbstractRoddyAlignmentWorkflowTests {

    String getRefGenFileNamePrefix() {
        return 'hs37d5_PhiX_Lambda.conv'
    }

    protected String getChromosomeStatFileName() {
        return 'hs37d5_PhiX_Lambda.fa.chrLenOnlyACGT.tab'
    }

    protected String getCytosinePositionsIndex() {
        return 'hs37d5_PhiX_Lambda.pos.gz'
    }

    @Override
    SeqType findSeqType() {
        return exactlyOneElement(SeqType.findAllWhere(
                name: SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName,
                libraryLayout: LibraryLayout.PAIRED,
        ))
    }

    @Override
    void setUpFilesVariables() {
        File baseTestDataDirWGBS = new File(inputRootDirectory, 'fastqFiles/wgbs')
        testFastqFiles = [
                readGroup1: [
                        new File(baseTestDataDirWGBS, 'normal/paired/lib1/run1/sequence/gerald_D1VCPACXX_6_R1.fastq.bz2'),
                        new File(baseTestDataDirWGBS, 'normal/paired/lib1/run1/sequence/gerald_D1VCPACXX_6_R2.fastq.bz2'),
                ].asImmutable(),
                readGroup2: [
                        new File(baseTestDataDirWGBS, 'normal/paired/lib2/run1/sequence/gerald_D1VCPACXX_7_R1.fastq.bz2'),
                        new File(baseTestDataDirWGBS, 'normal/paired/lib2/run1/sequence/gerald_D1VCPACXX_7_R2.fastq.bz2'),
                ].asImmutable(),
        ].asImmutable()
        refGenDir = new File(referenceGenomeDirectory, 'bwa06_methylCtools_hs37d5_PhiX_Lambda')
        chromosomeNamesFile = new File(referenceGenomeDirectory, 'chromosome-names.txt')
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/WgbsAlignmentWorkflow.groovy",
        ]
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_allFine() {

        // prepare
        createSeqTrack("readGroup1")

        executeAndVerify_AlignLanesOnly_AllFine()
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_TwoLibraries_allFine() {
        alignLanesOnly_NoBaseBamExist_TwoLanes(true)
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_TwoLanes_allFine() {
        alignLanesOnly_NoBaseBamExist_TwoLanes(false)
    }
}
