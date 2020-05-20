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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.After
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type
import de.dkfz.tbi.otp.project.Project

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

@Rollback
@Integration
class MergingSetServiceIntegrationTests {

    Sample sample
    SeqType seqType
    SeqPlatform seqPlatform
    SeqTrack seqTrack
    SeqTrack seqTrack2

    void setupData() {
        Project project = DomainFactory.createProject(
                        name: "name_1",
                        dirName: "dirName",
                        )
        assertNotNull(project.save([flush: true]))

        Individual individual = new Individual(
                        pid: "pid_1",
                        mockPid: "mockPid_1",
                        mockFullName: "mockFullName_1",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        SampleType sampleType = new SampleType(
                        name: "name-1"
                        )
        assertNotNull(sampleType.save([flush: true]))

        sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        seqType = DomainFactory.createSeqType()

        seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup()

        SeqCenter seqCenter = new SeqCenter(
                        name: "name",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        Run run = new Run(
                        name: "name",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        )
        assertNotNull(run.save([flush: true]))

        SoftwareTool softwareTool = new SoftwareTool(
                        programName: "name",
                        programVersion: "version",
                        type: Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true]))

        seqTrack = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true]))
        seqTrack2 = new SeqTrack(
                        laneId: "laneId2",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
        )
        assertNotNull(seqTrack2.save([flush: true]))
    }

    @After
    void tearDown() {
        sample = null
        seqType = null
        seqTrack = null
    }

    @Test(expected = AssertionError)
    void testNextIdentifierIdentifierNull() {
        setupData()
        MergingSet.nextIdentifier(null)
    }

    @Test
    void testNextIdentifier() {
        setupData()
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()
        assertEquals(0, MergingSet.nextIdentifier(mergingWorkPackage))
        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet.save([flush: true]))
        assertEquals(1, MergingSet.nextIdentifier(mergingWorkPackage))
        MergingSet mergingSet2 = new MergingSet(
                        identifier: 1,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet2.save([flush: true]))
        assertEquals(2, MergingSet.nextIdentifier(mergingWorkPackage))
    }

    private MergingWorkPackage createMergingWorkPackage() {
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqType)
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: DomainFactory.createReferenceGenome(),
                statSizeFileName: null,
                pipeline: DomainFactory.createDefaultOtpPipeline(),
        ] + MergingWorkPackage.getMergingProperties(seqTrack))
        assertNotNull(mergingWorkPackage.save([flush: true]))
        return mergingWorkPackage
    }
}
