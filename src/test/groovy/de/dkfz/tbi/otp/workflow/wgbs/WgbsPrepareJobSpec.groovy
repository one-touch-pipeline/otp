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
package de.dkfz.tbi.otp.workflow.wgbs

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WgbsAlignmentWorkflowDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerPreparationService
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.*

class WgbsPrepareJobSpec extends Specification implements DataTest, WgbsAlignmentWorkflowDomainFactory, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                WorkflowStep,
                MergingWorkPackage,
                FileType,
                FastqImportInstance,
                RoddyWorkflowConfig,
                RoddyBamFile,
                ReferenceGenomeProjectSeqType,
        ]
    }

    static private final String DIRECTORY = "/tmp"

    @TempDir
    Path tempDir

    private WorkflowStep workflowStep
    private WorkflowRun workflowRun
    private WorkflowArtefact workflowArtefact
    private RoddyBamFile roddyBamFile

    void "test buildWorkDirectoryPath should return workflowRun workDirectory"() {
        given:
        setupData()
        WgbsPrepareJob job = new WgbsPrepareJob([
                concreteArtefactService: Mock(ConcreteArtefactService) {
                    1 * getOutputArtefact(workflowStep, WgbsWorkflow.OUTPUT_BAM) >> roddyBamFile
                },
                roddyBamFileService    : Mock(RoddyBamFileService) {
                    1 * getWorkDirectory(roddyBamFile) >> Paths.get(DIRECTORY)
                },
        ])

        expect:
        job.buildWorkDirectoryPath(workflowStep).toString() == DIRECTORY
    }

    void "test generateMapForLinking should return empty list"() {
        given:
        setupData()
        WgbsPrepareJob job = new WgbsPrepareJob()

        when:
        Collection<LinkEntry> result = job.generateMapForLinking(workflowStep)

        then:
        result == []
    }

    void "test doFurtherPreparation should create a notification"() {
        given:
        setupData()
        List<SeqTrack> seqTracks = [createSeqTrack(), createSeqTrack()]

        WgbsPrepareJob job = new WgbsPrepareJob([
                concreteArtefactService: Mock(ConcreteArtefactService) {
                    1 * getInputArtefacts(workflowStep, WgbsWorkflow.INPUT_FASTQ) >> seqTracks
                    1 * getOutputArtefact(workflowStep, WgbsWorkflow.OUTPUT_BAM) >> roddyBamFile
                },
        ])

        job.panCancerPreparationService = new PanCancerPreparationService()
        job.panCancerPreparationService.notificationCreator = Mock(NotificationCreator)
        job.roddyConfigValueService = Mock(RoddyConfigValueService)
        job.fileService = Mock(FileService)
        job.roddyBamFileService = Mock(RoddyBamFileService) {
            getWorkMetadataTableFile(_) >> Paths.get("/tmp/non-existent-dir")
        }

        when:
        job.doFurtherPreparation(workflowStep)

        then:
        1 * job.panCancerPreparationService.notificationCreator.setStartedForSeqTracks(seqTracks, OtrsTicket.ProcessingStep.ALIGNMENT)
    }

    void "test doFurtherPreparation should mark start of workflow in MergingWorkPackage"() {
        given:
        setupData()
        List<SeqTrack> seqTracks = [createSeqTrack(), createSeqTrack()]

        WgbsPrepareJob job = new WgbsPrepareJob([
                concreteArtefactService: Mock(ConcreteArtefactService) {
                    1 * getInputArtefacts(workflowStep, WgbsWorkflow.INPUT_FASTQ) >> seqTracks
                    1 * getOutputArtefact(workflowStep, WgbsWorkflow.OUTPUT_BAM) >> roddyBamFile
                },
        ])

        job.panCancerPreparationService = new PanCancerPreparationService()
        job.panCancerPreparationService.notificationCreator = Mock(NotificationCreator)
        job.roddyConfigValueService = Mock(RoddyConfigValueService)
        job.fileService = Mock(FileService)
        job.roddyBamFileService = Mock(RoddyBamFileService) {
            getWorkMetadataTableFile(_) >> Paths.get("/tmp/non-existent-dir")
        }

        when:
        job.doFurtherPreparation(workflowStep)

        then:
        !MergingWorkPackage.findAll(seqTracks: seqTracks).first().needsProcessing
    }

    void "test doFurtherPreparation should create metadata table file"() {
        given:
        Path metadataFile = tempDir.resolve("file.tsv")

        setupData()
        List<SeqTrack> seqTracks = [createSeqTrackWithTwoFastqFile(), createSeqTrackWithTwoFastqFile()]

        WgbsPrepareJob job = new WgbsPrepareJob([
                concreteArtefactService: Mock(ConcreteArtefactService) {
                    1 * getInputArtefacts(workflowStep, WgbsWorkflow.INPUT_FASTQ) >> seqTracks
                    1 * getOutputArtefact(workflowStep, WgbsWorkflow.OUTPUT_BAM) >> roddyBamFile
                },
        ])

        job.panCancerPreparationService = Mock(PanCancerPreparationService)
        job.roddyConfigValueService = new RoddyConfigValueService()
        job.roddyConfigValueService.lsdfFilesService = Mock(LsdfFilesService)
        job.fileService = new FileService()
        job.roddyBamFileService = Mock(RoddyBamFileService) {
            getWorkMetadataTableFile(_) >> metadataFile
        }

        when:
        job.doFurtherPreparation(workflowStep)

        then:
        Files.exists(metadataFile)
        metadataFile.readLines().size() == 5
    }

    void setupData() {
        workflowRun = createWorkflowRun([
                workflowVersion: createWgbsAlignmenWorkflowVersion(),
                workDirectory  : DIRECTORY,
        ])
        workflowStep = createWorkflowStep([
                workflowRun: workflowRun,
        ])
        workflowArtefact = createWorkflowArtefact([
                producedBy  : workflowRun,
                artefactType: ArtefactType.BAM,
                outputRole  : PanCancerWorkflow.OUTPUT_BAM,
        ])
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage([
                needsProcessing: true,
        ])
        roddyBamFile = createBamFile([
                workflowArtefact: workflowArtefact,
                workPackage     : mergingWorkPackage,
        ])
    }
}
