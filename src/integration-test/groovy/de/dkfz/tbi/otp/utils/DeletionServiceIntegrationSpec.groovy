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
package de.dkfz.tbi.otp.utils

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AnalysisDeletionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Rollback
@Integration
class DeletionServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    DeletionService deletionService

    void setupData() {
        deletionService = new DeletionService()
        deletionService.lsdfFilesService = new LsdfFilesService()
        deletionService.dataSwapService = Mock(DataSwapService)
        deletionService.commentService = Mock(CommentService)
        deletionService.fastqcDataFilesService = Mock(FastqcDataFilesService)
        deletionService.dataProcessingFilesService = Mock(DataProcessingFilesService)
        deletionService.seqTrackService = Mock(SeqTrackService)
        deletionService.analysisDeletionService = new AnalysisDeletionService()
        deletionService.fileService = new FileService()
        deletionService.runService = new RunService()
    }

    void "test delete project content"() {
        given:
        setupData()
        Project project = createProject()
        SeqTrack seqTrack = createSeqTrack([
                sample: createSample([
                        individual: createIndividual([
                                project: project
                        ])
                ])
        ])
        createDataFile([seqTrack: seqTrack])
        createDataFile([seqTrack: seqTrack])

        when:
        deletionService.deleteProjectContent(project)

        then:
        DataFile.count() == 0
        Individual.count() == 0
        Project.count() == 1
    }

    void "deleteSeqTrack, delete an empty run"() {
        given:
        setupData()
        Run run = createRun()
        SeqTrack seqTrack = createSeqTrack(run: run)

        assert Run.count() == 1

        when:
        deletionService.deleteSeqTrack(seqTrack)

        then:
        Run.count() == 0
    }

    void "deleteSeqTrack, keep an non empty run"() {
        given:
        setupData()
        Run run = createRun()
        List<SeqTrack> seqTrackList = []
        (0..1).collect {
            seqTrackList <<  createSeqTrack(run: run)
        }

        assert Run.count() == 1

        when:
        deletionService.deleteSeqTrack(seqTrackList.first())

        then:
        Run.count() == 1
    }

    void "test delete project content without any content"() {
        given:
        setupData()
        Project project = createProject()

        when:
        deletionService.deleteProjectContent(project)

        then:
        Project.count() == 1
    }

    void "test delete project"() {
        given:
        setupData()
        Project project = createProject()
        SeqTrack seqTrack = createSeqTrack([
                sample: createSample([
                        individual: createIndividual([
                                project: project
                        ])
                ])
        ])
        createDataFile([seqTrack: seqTrack])
        createDataFile([seqTrack: seqTrack])

        when:
        deletionService.deleteProject(project)

        then:
        DataFile.count() == 0
        Individual.count() == 0
        Project.count() == 0
    }

    void "test delete project without any content"() {
        given:
        setupData()
        Project project = createProject()

        when:
        deletionService.deleteProject(project)

        then:
        Project.count() == 0
    }

    void "test delete project with config per project and seq type"() {
        given:
        setupData()
        Project project = createProject()
        SeqTrack seqTrack = createSeqTrack([
                sample: createSample([
                        individual: createIndividual([
                                project: project
                        ])
                ])
        ])
        RoddyWorkflowConfig config1 = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqTrack.seqType,
                obsoleteDate: new Date(),
        ])

        DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqTrack.seqType,
                previousConfig: config1,
        ])

        when:
        deletionService.deleteProject(project)

        then:
        ConfigPerProjectAndSeqType.count() == 0
        Project.count() == 0
    }

    void "test delete project with config per project and seq type reverse"() {
        given:
        setupData()
        Project project = createProject()
        SeqTrack seqTrack = createSeqTrack([
                sample: createSample([
                        individual: createIndividual([
                                project: project
                        ])
                ])
        ])
        RoddyWorkflowConfig config1 = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqTrack.seqType,
        ])

        RoddyWorkflowConfig config2 = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqTrack.seqType,
                obsoleteDate: new Date(),
        ])

        config1.previousConfig = config2
        config1.save(flush: true)

        when:
        deletionService.deleteProject(project)

        then:
        ConfigPerProjectAndSeqType.count() == 0
        Project.count() == 0
    }

    void "test delete process with dependencies"() {
        given:
        setupData()
        JobExecutionPlan jobExecutionPlan = DomainFactory.createJobExecutionPlan()
        JobDefinition jobDefinition1 = DomainFactory.createJobDefinition([
                plan: jobExecutionPlan,
        ])

        JobDefinition jobDefinition2 = DomainFactory.createJobDefinition([
                plan: jobExecutionPlan,
        ])

        JobDefinition jobDefinition3 = DomainFactory.createJobDefinition([
                plan: jobExecutionPlan,
        ])

        Process process = DomainFactory.createProcess([
                jobExecutionPlan:  jobExecutionPlan,
                finished: true,
        ])

        ProcessingStep processingStep1 = DomainFactory.createProcessingStep([
                process: process,
                jobDefinition: jobDefinition1,
        ])

        ProcessingStep processingStep2 = DomainFactory.createProcessingStep([
                process: process,
                jobDefinition: jobDefinition2,
                previous: processingStep1,
        ])

        processingStep1.next = processingStep2
        processingStep1.save(flush: true)

        ProcessingStep processingStep3 = DomainFactory.createProcessingStep([
                process: process,
                jobDefinition: jobDefinition3,
                previous: processingStep2,
        ])

        processingStep2.next = processingStep3
        processingStep2.save(flush: true)

        DomainFactory.createRestartedProcessingStep([
                process: process,
                jobDefinition: jobDefinition3,
                original: processingStep3,
                previous: processingStep3.previous,
        ])

        ProcessingStepUpdate processingStepUpdate11 = DomainFactory.createProcessingStepUpdate([
                processingStep: processingStep1,
        ])

        ProcessingError processingError =  DomainFactory.createProcessingError()

        ProcessingStepUpdate processingStepUpdate12 = DomainFactory.createProcessingStepUpdate([
                processingStep: processingStep1,
                previous: processingStepUpdate11,
                state: ExecutionState.FAILURE,
                error: processingError,
        ])

        DomainFactory.createProcessingStepUpdate([
                processingStep: processingStep1,
                previous: processingStepUpdate12,
        ])

        ClusterJob clusterJob21 = DomainFactory.createClusterJob([
                processingStep: processingStep2,
        ])

        DomainFactory.createClusterJob([
                processingStep: processingStep2,
                dependencies: [clusterJob21] as Set,
        ])

        when:
        deletionService.deleteProcess(process)

        then:
        ProcessingError.count() == 0
        ProcessingStepUpdate.count() == 0
        RestartedProcessingStep.count() == 0
        ProcessingStep.count() == 0
        Process.count() == 0
        ClusterJob.count() == 0
    }

    void "test deleteProcessParameters"() {
        given:
        ProcessParameter processParameter = DomainFactory.createProcessParameter(className: SeqTrack.name)
        processParameter.process.finished = true
        processParameter.process.save(flush: true)

        when:
        deletionService.deleteProcessParameters([processParameter])

        then:
        ProcessParameter.count() == 0
        Process.count() == 0
    }
}
