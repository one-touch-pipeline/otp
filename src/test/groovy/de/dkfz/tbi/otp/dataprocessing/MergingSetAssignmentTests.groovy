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

package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.*

import de.dkfz.tbi.otp.ngsdata.*

import static org.junit.Assert.assertTrue

@TestFor(MergingSetAssignment)
@Build([
        MergingCriteria,
        MergingSet,
        ProcessedBamFile,
])
class MergingSetAssignmentTests {

    MergingSet mergingSet = null
    ProcessedBamFile processedBamFile = null

    @Before
    void setUp() {
        Individual individual = DomainFactory.createIndividual()

        SampleType sampleType = DomainFactory.createSampleType( [
                name: "TUMOR",
        ])

        Sample sample = DomainFactory.createSample(
                individual: individual,
                sampleType: sampleType,
        )

        SeqType seqType = DomainFactory.createWholeGenomeSeqType(LibraryLayout.SINGLE)

        Run run = DomainFactory.createRun()

        SoftwareTool softwareTool = DomainFactory.createSoftwareTool([
                type: SoftwareTool.Type.ALIGNMENT,
        ])

        SeqTrack seqTrack = DomainFactory.createSeqTrack([
                run: run,
                sample: sample,
                seqType: seqType,
                pipelineVersion: softwareTool,
        ])

        AlignmentPass alignmentPass = DomainFactory.createAlignmentPass(
            identifier: 2,
            seqTrack: seqTrack,
        )

        processedBamFile = new ProcessedBamFile()
        processedBamFile.type = AbstractBamFile.BamType.SORTED
        processedBamFile.fileExists = true
        processedBamFile.dateFromFileSystem = new Date()
        processedBamFile.alignmentPass = alignmentPass
        processedBamFile.save(flush: true)
        assertTrue(processedBamFile.validate())

        MergingWorkPackage workPackage = processedBamFile.mergingWorkPackage
        workPackage.save(flush: true)

        this.mergingSet = new MergingSet(
            mergingWorkPackage: workPackage)
        this.mergingSet.save(flush: true)
    }

    @After
    void tearDown() {
        mergingSet = null
        processedBamFile = null
    }

    @Test
    void testSave() {
        MergingSetAssignment mtm = new MergingSetAssignment(
            mergingSet: mergingSet,
            bamFile: processedBamFile)
        Assert.assertTrue(mtm.validate())
        mtm.save(flush: true)
    }

    @Test
    void testConstraints() {
        // mergingSet must not be null
        MergingSetAssignment mtm = new MergingSetAssignment(
            bamFile: processedBamFile)
        Assert.assertFalse(mtm.validate())
        // bamFile must not be null
        mtm.mergingSet = mergingSet
        Assert.assertTrue(mtm.validate())
        mtm.bamFile = null
        Assert.assertFalse(mtm.validate())
    }
}
