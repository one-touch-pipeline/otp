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

package de.dkfz.tbi.otp.monitor.alignment

import grails.transaction.Rollback

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType

@Rollback
class CellRangerAlignmentCheckerIntegrationSpec extends AbstractAlignmentCheckerIntegrationSpec implements CellRangerFactory {

    @Override
    AbstractAlignmentChecker createAlignmentChecker() {
        return new CellRangerAlignmentChecker()
    }

    @Override
    Pipeline createPipeLine() {
        return CellRangerFactory.super.findOrCreatePipeline()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createRoddyRnaPipeline()
    }

    @Override
    AbstractMergedBamFile createBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        createBamFile([
                workPackage: mergingWorkPackage,
        ] + properties)
    }

    @Override
    MergingWorkPackage createMWP(Map properties = [:]) {
        return createMergingWorkPackage([
                pipeline: createPipeLine(),
                seqType : seqTypes.first(),
        ] + properties)
    }

    @Override
    List<SeqTrack> createSeqTracksWithConfig(Map configProperties = [:], Map seqTrackProperties = [:]) {
        createSeqTracks(seqTrackProperties).each {
            createCellRangerConfig([
                    seqType: it.seqType,
                    project: it.project,
                    pipeline: createPipeLine(),
            ] +  configProperties)
        }
    }


    void "workflowName, should return CellRangerWorkflow"() {
        expect:
        'CellRangerWorkflow' == createAlignmentChecker().getWorkflowName()
    }

    void "pipeLineName, should return CELL_RANGER"() {
        expect:
        Pipeline.Name.CELL_RANGER == createAlignmentChecker().getPipeLineName()
    }

    void "seqTypes, should return single cell"() {
        given:
        List<SeqType> seqTypes = DomainFactory.createCellRangerAlignableSeqTypes()

        expect:
        TestCase.assertContainSame(seqTypes, createAlignmentChecker().getSeqTypes())
    }
}