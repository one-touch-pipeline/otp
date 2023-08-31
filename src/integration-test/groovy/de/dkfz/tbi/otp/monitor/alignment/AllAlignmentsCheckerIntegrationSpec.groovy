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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.monitor.MonitorOutputCollector
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow

@Rollback
@Integration
class AllAlignmentsCheckerIntegrationSpec extends Specification implements WorkflowSystemDomainFactory, RoddyRnaFactory {

    void "handle, if SeqTracks given, then return finished RoddyBamFile, call Roddy alignments workflows with correct seqTracks and output SeqTypes not supported by any workflow"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        AllAlignmentsChecker checker = new AllAlignmentsChecker()

        SeqTrack wrongSeqType = DomainFactory.createSeqTrack()

        List<SeqTrack> panCanSeqTracks = DomainFactory.createRoddyAlignableSeqTypes().collect {
            DomainFactory.createSeqTrack(
                    DomainFactory.createMergingWorkPackage(
                            seqType: it,
                            pipeline: DomainFactory.createPanCanPipeline(),
                    )
            )
        }
        DomainFactory.createCellRangerAlignableSeqTypes().collect {
                DomainFactory.createSeqTrack(
                        DomainFactory.proxyCellRanger.createMergingWorkPackage(
                            seqType: it,
                            pipeline: AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.findOrCreatePipeline(),
                    )
            )
        }

        createWorkflow(name: PanCancerWorkflow.WORKFLOW, supportedSeqTypes: [DomainFactory.createWholeGenomeSeqType(), DomainFactory.createExomeSeqType(),
                                                                             DomainFactory.createChipSeqType(),] as Set)
        createWorkflow(name: WgbsWorkflow.WGBS_WORKFLOW, supportedSeqTypes: [DomainFactory.createWholeGenomeBisulfiteSeqType(),
                                                                             DomainFactory.createWholeGenomeBisulfiteTagmentationSeqType(),] as Set)

        RnaRoddyBamFile rnaRoddyBamFile = createBamFile([
                fileOperationStatus: AbstractBamFile.FileOperationStatus.PROCESSED,
        ])

        List<SeqTrack> seqTracks = [
                wrongSeqType,
                panCanSeqTracks,
                rnaRoddyBamFile.containedSeqTracks,
        ].flatten()

        List<RoddyBamFile> finishedRoddyBamFiles = [rnaRoddyBamFile]

        when:
        List<RoddyBamFile> result = checker.handle(seqTracks, output)

        then:
        1 * output.showUniqueList(AllAlignmentsChecker.HEADER_NOT_SUPPORTED_SEQTYPES, [wrongSeqType], _)

        then:
        1 * output.showWorkflowOldSystem('PanCanWorkflow')
        1 * output.showWorkflowOldSystem('WgbsAlignmentWorkflow')
        1 * output.showWorkflowOldSystem('RnaAlignmentWorkflow')
        0 * output.showWorkflowOldSystem(_)

        then:
        finishedRoddyBamFiles == result
    }

    void "handle, if no SeqTracks given, then return empty list and do not create output"() {
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        AllAlignmentsChecker checker = new AllAlignmentsChecker()

        when:
        List<RoddyBamFile> result = checker.handle([], output)

        then:
        0 * output._

        then:
        [] == result
    }
}
