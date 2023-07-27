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
package de.dkfz.tbi.otp.utils

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AnalysisDeletionService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.administration.DocumentFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.submissions.ega.EgaSubmissionFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.egaSubmission.EgaSubmission
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.project.dta.*
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Path
import java.nio.file.Paths

@Rollback
@Integration
class DeletionServiceIntegrationSpec extends Specification implements EgaSubmissionFactory, IsRoddy, DocumentFactory, WorkflowSystemDomainFactory {

    DeletionService deletionService
    TestConfigService configService

    String seqDir = "/seq-dir"

    @TempDir
    Path tempDir

    void setupData() {
        IndividualService individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> { Individual individual, SeqType seqType ->
                Paths.get(seqDir).resolve(seqType.dirName).resolve(individual.pid)
            }
        }

        deletionService = new DeletionService()
        deletionService.individualService = individualService
        deletionService.lsdfFilesService = new LsdfFilesService()
        deletionService.lsdfFilesService.projectService = Mock(ProjectService) {
            getSequencingDirectory(_) >> Paths.get(seqDir)
        }
        deletionService.lsdfFilesService.individualService = individualService
        deletionService.commentService = Mock(CommentService)
        deletionService.fastqcDataFilesService = Mock(FastqcDataFilesService)
        deletionService.dataProcessingFilesService = Mock(DataProcessingFilesService)
        deletionService.seqTrackService = Mock(SeqTrackService)
        deletionService.analysisDeletionService = new AnalysisDeletionService()
        deletionService.fileService = new FileService()
        deletionService.runService = new RunService()
        deletionService.workflowDeletionService = new WorkflowDeletionService()
    }

    void cleanup() {
        configService.clean()
    }

    void "assertNoEgaSubmissionsForProject, throws assertion error when there are connected EgaSubmissions"() {
        given:
        setupData()

        Project project = createProject()
        createEgaSubmission(project: project)

        when:
        deletionService.deleteProjectContent(project)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("There are Ega Submissions connected to this Project, thus it can not be deleted")
    }

    void "assertNoEgaSubmissionsForProject, does not throw an assertion error without connected EgaSubmissions"() {
        given:
        setupData()

        Project project = createProject()

        expect:
        deletionService.deleteProjectContent(project)
    }

    void "deleteProjectContent"() {
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
        createFastqFile([seqTrack: seqTrack])
        createFastqFile([seqTrack: seqTrack])
        createReferenceGenomeSelector(project: project)
        createWorkflowVersionSelector(project: project)
        createWorkflowVersionSelector(project: project)
        createWorkflowVersionSelector(project: project)
        createExternalWorkflowConfigSelector(projects: [project])
        (1..5).each {
            createWorkflowRun(project: project, restartedFrom: createWorkflowRun(project: project))
        }

        when:
        deletionService.deleteProjectContent(project)

        then:
        RawSequenceFile.count() == 0
        Individual.count() == 0
        WorkflowVersionSelector.count == 0
        ReferenceGenomeSelector.count == 0
        ExternalWorkflowConfigSelector.count == 0
        WorkflowRun.count == 0
        Project.count() == 1
    }

    void "deleteProjectContent, for project with EgaSubmission"() {
        given:
        setupData()

        EgaSubmission egaSubmission = createEgaSubmission()

        when:
        deletionService.deleteProjectContent(egaSubmission.project)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("There are Ega Submissions connected to this Project, thus it can not be deleted")
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
        List<SeqTrack> seqTrackList = (0..1).collect {
            createSeqTrack(run: run)
        }

        assert Run.count() == 1

        when:
        deletionService.deleteSeqTrack(seqTrackList.first())

        then:
        Run.count() == 1
    }

    void "deleteSeqTrack, delete non-blacklisted ilse that is not used by other seqTracks"() {
        given:
        setupData()
        IlseSubmission ilseSubmission = createIlseSubmission(ilseNumber: 1234)
        SeqTrack seqTrack = createSeqTrack(ilseSubmission: ilseSubmission)

        when:
        deletionService.deleteSeqTrack(seqTrack)

        then:
        IlseSubmission.count() == 0
    }

    void "deleteSeqTrack, don't delete non-blacklisted ilse that is used by other seqTracks"() {
        given:
        setupData()
        IlseSubmission ilseSubmission = createIlseSubmission(ilseNumber: 1234)
        SeqTrack seqTrack = createSeqTrack(ilseSubmission: ilseSubmission)
        createSeqTrack(ilseSubmission: ilseSubmission)

        when:
        deletionService.deleteSeqTrack(seqTrack)

        then:
        IlseSubmission.count() == 1
    }

    void "deleteSeqTrack, don't delete ilse that is blacklisted that is not used by other seqTracks"() {
        given:
        setupData()
        IlseSubmission ilseSubmission = createIlseSubmission(ilseNumber: 1234)
        ilseSubmission.warning = true
        ilseSubmission.comment = new Comment(author: "user", comment: "test", modificationDate: new Date())
        ilseSubmission.save(flush: true)

        SeqTrack seqTrack = createSeqTrack(ilseSubmission: ilseSubmission)

        when:
        deletionService.deleteSeqTrack(seqTrack)

        then:
        IlseSubmission.count() == 1
    }

    void "deleteSeqTrack, don't delete ilse that is blacklisted that is used by other seqTracks"() {
        given:
        setupData()
        IlseSubmission ilseSubmission = createIlseSubmission(ilseNumber: 1234)
        ilseSubmission.warning = true
        ilseSubmission.comment = new Comment(author: "user", comment: "test", modificationDate: new Date())
        ilseSubmission.save(flush: true)

        SeqTrack seqTrack = createSeqTrack(ilseSubmission: ilseSubmission)
        createSeqTrack(ilseSubmission: ilseSubmission)

        when:
        deletionService.deleteSeqTrack(seqTrack)

        then:
        IlseSubmission.count() == 1
    }

    void "deleteSeqTrack, should delete subdirectories correctly"() {
        given:
        setupData()
        Run run = createRun(name: "0", seqCenter: createSeqCenter(dirName: "sc0"))

        Individual individual = createIndividual(pid: "pid0")
        Sample sample1 = createSample(individual: individual, sampleType: createSampleType(name: "sampletype1"))
        Sample sample2 = createSample(individual: individual, sampleType: createSampleType(name: "sampletype2"))
        SeqType seqType1 = createSeqType(dirName: "seqType1")
        SeqType seqType2 = createSeqType(dirName: "seqType2")
        SeqType seqType3 = createSeqType(dirName: "seqType3", hasAntibodyTarget: true)
        AntibodyTarget antibodyTarget1 = createAntibodyTarget(name: "abt1")
        AntibodyTarget antibodyTarget2 = createAntibodyTarget(name: "abt2")

        SeqTrack seqTrack1 = createSeqTrackWithOneFastqFile([laneId: "lane1", sample: sample1, seqType: seqType1, run: run], [fileName: "df1.gz", vbpFileName: "df1.gz"])
        SeqTrack seqTrack2 = createSeqTrackWithOneFastqFile([laneId: "lane2", sample: sample1, seqType: seqType1, run: run], [fileName: "df2.gz", vbpFileName: "df2.gz"])
        SeqTrack seqTrack3 = createSeqTrackWithOneFastqFile([laneId: "lane3", sample: sample2, seqType: seqType1, run: run], [fileName: "df3.gz", vbpFileName: "df3.gz"])
        SeqTrack seqTrack4 = createSeqTrackWithOneFastqFile([laneId: "lane4", sample: sample2, seqType: seqType1, run: run], [fileName: "df4.gz", vbpFileName: "df4.gz"])

        SeqTrack seqTrack1a = createSeqTrackWithOneFastqFile([laneId: "lane1a", sample: sample1, seqType: seqType3, run: run, antibodyTarget: antibodyTarget1], [fileName: "df1.gz", vbpFileName: "df1.gz"])
        SeqTrack seqTrack2a = createSeqTrackWithOneFastqFile([laneId: "lane2a", sample: sample1, seqType: seqType3, run: run, antibodyTarget: antibodyTarget1], [fileName: "df2.gz", vbpFileName: "df2.gz"])
        SeqTrack seqTrack3a = createSeqTrackWithOneFastqFile([laneId: "lane3a", sample: sample1, seqType: seqType3, run: run, antibodyTarget: antibodyTarget2], [fileName: "df3.gz", vbpFileName: "df3.gz"])
        SeqTrack seqTrack4a = createSeqTrackWithOneFastqFile([laneId: "lane4a", sample: sample1, seqType: seqType3, run: run, antibodyTarget: antibodyTarget2], [fileName: "df4.gz", vbpFileName: "df4.gz"])

        SeqTrack seqTrack5 = createSeqTrackWithOneFastqFile([laneId: "lane5", sample: sample1, seqType: seqType2, run: run], [fileName: "df5.gz", vbpFileName: "df5.gz"])
        SeqTrack seqTrack6 = createSeqTrackWithOneFastqFile([laneId: "lane6", sample: sample1, seqType: seqType2, run: run], [fileName: "df6.gz", vbpFileName: "df6.gz"])
        SeqTrack seqTrack7 = createSeqTrackWithOneFastqFile([laneId: "lane7", sample: sample2, seqType: seqType2, run: run], [fileName: "df7.gz", vbpFileName: "df7.gz"])
        SeqTrack seqTrack8 = createSeqTrackWithOneFastqFile([laneId: "lane8", sample: sample2, seqType: seqType2, run: run], [fileName: "df8.gz", vbpFileName: "df8.gz"])

        expect:
        deletionService.deleteSeqTrack(seqTrack1) == [
                new File("/seq-dir/seqType1/sc0/run0/df1.gz"),
                new File("/seq-dir/seqType1/sc0/run0/df1.gz.md5sum"),
                new File("/seq-dir/seqType1/pid0/sampletype1/single/run0/sequence/df1.gz"),
        ]
        deletionService.deleteSeqTrack(seqTrack2) == [
                new File("/seq-dir/seqType1/sc0/run0/df2.gz"),
                new File("/seq-dir/seqType1/sc0/run0/df2.gz.md5sum"),
                new File("/seq-dir/seqType1/pid0/sampletype1/single/run0/sequence/df2.gz"),
                new File("/seq-dir/seqType1/pid0/sampletype1"),
        ]
        deletionService.deleteSeqTrack(seqTrack3) == [
                new File("/seq-dir/seqType1/sc0/run0/df3.gz"),
                new File("/seq-dir/seqType1/sc0/run0/df3.gz.md5sum"),
                new File("/seq-dir/seqType1/pid0/sampletype2/single/run0/sequence/df3.gz"),
        ]
        deletionService.deleteSeqTrack(seqTrack4) == [
                new File("/seq-dir/seqType1/sc0/run0/df4.gz"),
                new File("/seq-dir/seqType1/sc0/run0/df4.gz.md5sum"),
                new File("/seq-dir/seqType1/pid0/sampletype2/single/run0/sequence/df4.gz"),
                new File("/seq-dir/seqType1/pid0/sampletype2"),
                new File("/seq-dir/seqType1/pid0"),
        ]
        deletionService.deleteSeqTrack(seqTrack1a) == [
                new File("/seq-dir/seqType3/sc0/run0/df1.gz"),
                new File("/seq-dir/seqType3/sc0/run0/df1.gz.md5sum"),
                new File("/seq-dir/seqType3/pid0/sampletype1-abt1/single/run0/sequence/df1.gz"),
        ]
        deletionService.deleteSeqTrack(seqTrack2a) == [
                new File("/seq-dir/seqType3/sc0/run0/df2.gz"),
                new File("/seq-dir/seqType3/sc0/run0/df2.gz.md5sum"),
                new File("/seq-dir/seqType3/pid0/sampletype1-abt1/single/run0/sequence/df2.gz"),
                new File("/seq-dir/seqType3/pid0/sampletype1-abt1"),
        ]
        deletionService.deleteSeqTrack(seqTrack3a) == [
                new File("/seq-dir/seqType3/sc0/run0/df3.gz"),
                new File("/seq-dir/seqType3/sc0/run0/df3.gz.md5sum"),
                new File("/seq-dir/seqType3/pid0/sampletype1-abt2/single/run0/sequence/df3.gz"),
        ]
        deletionService.deleteSeqTrack(seqTrack4a) == [
                new File("/seq-dir/seqType3/sc0/run0/df4.gz"),
                new File("/seq-dir/seqType3/sc0/run0/df4.gz.md5sum"),
                new File("/seq-dir/seqType3/pid0/sampletype1-abt2/single/run0/sequence/df4.gz"),
                new File("/seq-dir/seqType3/pid0/sampletype1-abt2"),
                new File("/seq-dir/seqType3/pid0"),
        ]
        deletionService.deleteSeqTrack(seqTrack5) == [
                new File("/seq-dir/seqType2/sc0/run0/df5.gz"),
                new File("/seq-dir/seqType2/sc0/run0/df5.gz.md5sum"),
                new File("/seq-dir/seqType2/pid0/sampletype1/single/run0/sequence/df5.gz"),
        ]
        deletionService.deleteSeqTrack(seqTrack6) == [
                new File("/seq-dir/seqType2/sc0/run0/df6.gz"),
                new File("/seq-dir/seqType2/sc0/run0/df6.gz.md5sum"),
                new File("/seq-dir/seqType2/pid0/sampletype1/single/run0/sequence/df6.gz"),
                new File("/seq-dir/seqType2/pid0/sampletype1"),
        ]
        deletionService.deleteSeqTrack(seqTrack7) == [
                new File("/seq-dir/seqType2/sc0/run0/df7.gz"),
                new File("/seq-dir/seqType2/sc0/run0/df7.gz.md5sum"),
                new File("/seq-dir/seqType2/pid0/sampletype2/single/run0/sequence/df7.gz"),
        ]
        deletionService.deleteSeqTrack(seqTrack8) == [
                new File("/seq-dir/seqType2/sc0/run0/df8.gz"),
                new File("/seq-dir/seqType2/sc0/run0/df8.gz.md5sum"),
                new File("/seq-dir/seqType2/pid0/sampletype2/single/run0/sequence/df8.gz"),
                new File("/seq-dir/seqType2/pid0/sampletype2"),
                new File("/seq-dir/seqType2/pid0"),
        ]
    }

    void "deleteSeqTrack, should delete individual"() {
        given:
        setupData()
        SeqTrack seqTrack = createSeqTrack()

        when:
        deletionService.deleteSeqTrack(seqTrack)

        then:
        Individual.all.empty
    }

    void "deleteSeqTrack, should not delete individual"() {
        given:
        setupData()
        SeqTrack seqTrack = createSeqTrack()
        createSeqTrack(sample: seqTrack.sample)

        when:
        deletionService.deleteSeqTrack(seqTrack)

        then:
        !Individual.all.empty
    }

    void "deleteSeqTrack, should delete sample"() {
        given:
        setupData()
        SeqTrack seqTrack = createSeqTrack()

        when:
        deletionService.deleteSeqTrack(seqTrack)

        then:
        Sample.all.empty
    }

    void "deleteSeqTrack, should not delete sample"() {
        given:
        setupData()
        SeqTrack seqTrack = createSeqTrack()
        createSeqTrack(sample: seqTrack.sample)

        when:
        deletionService.deleteSeqTrack(seqTrack)

        then:
        !Sample.all.empty
    }

    void "deleteProjectContent without any content"() {
        given:
        setupData()
        Project project = createProject()

        when:
        deletionService.deleteProjectContent(project)

        then:
        Project.count() == 1
    }

    void "deleteProject"() {
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
        createFastqFile([seqTrack: seqTrack])
        createFastqFile([seqTrack: seqTrack])

        when:
        deletionService.deleteProject(project)

        then:
        RawSequenceFile.count() == 0
        Individual.count() == 0
        Project.count() == 0
    }

    void "deleteProject without any content"() {
        given:
        setupData()
        Project project = createProject()

        when:
        deletionService.deleteProject(project)

        then:
        Project.count() == 0
    }

    void "deleteProject with config per project and seq type"() {
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
                project     : project,
                seqType     : seqTrack.seqType,
                obsoleteDate: new Date(),
        ])

        DomainFactory.createRoddyWorkflowConfig([
                project       : project,
                seqType       : seqTrack.seqType,
                previousConfig: config1,
        ])

        when:
        deletionService.deleteProject(project)

        then:
        ConfigPerProjectAndSeqType.count() == 0
        Project.count() == 0
    }

    void "deleteProject with config per project and seq type reverse"() {
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
                project     : project,
                seqType     : seqTrack.seqType,
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

    void "deleteProject with dataTransferAgreement and dataTransfer"() {
        given:
        setupData()
        Project project = createProject()

        DataTransferAgreement dataTransferAgreement = createDataTransferAgreement([
                project: project,
        ])

        dataTransferAgreement.addToDataTransferAgreementDocuments(createDataTransferAgreementDocument([
                dataTransferAgreement: dataTransferAgreement,
        ]))

        DataTransfer dataTransfer = createDataTransfer([
                dataTransferAgreement: dataTransferAgreement,
        ])

        dataTransferAgreement.addToTransfers(dataTransfer)

        dataTransfer.addToDataTransferDocuments(createDataTransferDocument([
                dataTransfer: dataTransfer,
        ]))

        when:
        deletionService.deleteProject(project)

        then:
        DataTransferDocument.count() == 0
        DataTransfer.count() == 0
        DataTransferAgreementDocument.count() == 0
        DataTransferAgreement.count() == 0
        Project.count() == 0
    }

    void "deleteProcess with dependencies"() {
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
                jobExecutionPlan: jobExecutionPlan,
                finished        : true,
        ])

        ProcessingStep processingStep1 = DomainFactory.createProcessingStep([
                process      : process,
                jobDefinition: jobDefinition1,
        ])

        ProcessingStep processingStep2 = DomainFactory.createProcessingStep([
                process      : process,
                jobDefinition: jobDefinition2,
                previous     : processingStep1,
        ])

        processingStep1.next = processingStep2
        processingStep1.save(flush: true)

        ProcessingStep processingStep3 = DomainFactory.createProcessingStep([
                process      : process,
                jobDefinition: jobDefinition3,
                previous     : processingStep2,
        ])

        processingStep2.next = processingStep3
        processingStep2.save(flush: true)

        DomainFactory.createRestartedProcessingStep([
                process      : process,
                jobDefinition: jobDefinition3,
                original     : processingStep3,
                previous     : processingStep3.previous,
        ])

        ProcessingStepUpdate processingStepUpdate11 = DomainFactory.createProcessingStepUpdate([
                processingStep: processingStep1,
        ])

        ProcessingError processingError = DomainFactory.createProcessingError()

        ProcessingStepUpdate processingStepUpdate12 = DomainFactory.createProcessingStepUpdate([
                processingStep: processingStep1,
                previous      : processingStepUpdate11,
                state         : ExecutionState.FAILURE,
                error         : processingError,
        ])

        DomainFactory.createProcessingStepUpdate([
                processingStep: processingStep1,
                previous      : processingStepUpdate12,
        ])

        ClusterJob clusterJob21 = DomainFactory.createClusterJob([
                processingStep: processingStep2,
        ])

        DomainFactory.createClusterJob([
                processingStep: processingStep2,
                dependencies  : [clusterJob21] as Set,
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

    void "deleteProcessParameters"() {
        given:
        ProcessParameter processParameter1 = DomainFactory.createProcessParameter(className: SeqTrack.name)
        ProcessParameter processParameter2 = DomainFactory.createProcessParameter(className: SeqTrack.name)
        ProcessParameter processParameter3 = DomainFactory.createProcessParameter(className: SeqTrack.name)
        DomainFactory.createProcessParameter(className: SeqTrack.name)

        processParameter1.process.finished = true
        processParameter1.process.save(flush: true)

        processParameter2.process.restarted = processParameter1.process
        processParameter2.process.finished = true
        processParameter2.process.save(flush: true)

        processParameter3.process.restarted = processParameter2.process
        processParameter3.process.finished = true
        processParameter3.process.save(flush: true)

        when:
        deletionService.deleteProcessParameters([processParameter1])

        then:
        ProcessParameter.count() == 1
        Process.count() == 1
    }

    void "deleteIndividual with Sample, SampleIdentifier, SeqTrack, ClusterJob and WorkflowArtefact and create script for data file deletion"() {
        given:
        setupData()

        configService.addOtpProperties(tempDir)
        final String projectPath = "projectPath"
        final Individual individual = createIndividual(
                project: createProject(
                        dirName: projectPath
                )
        )
        final SeqTrack seqTrack = createSeqTrack(
                sample: createSample(
                        individual: individual
                )
        )
        createClusterJob(individual: individual)

        final RawSequenceFile rawSequenceFile = createFastqFile(seqTrack: seqTrack)
        createSampleIdentifier(sample: seqTrack.sample)

        final String seqCenterName = rawSequenceFile.run.seqCenter.dirName
        final String runDirName = rawSequenceFile.run.dirName
        final String seqTypeDirName = seqTrack.seqType.dirName
        final String sampleTypeDirName = seqTrack.sample.sampleType.dirName
        final String vbpPath = rawSequenceFile.fileType.vbpPath
        final String vbpFileName = rawSequenceFile.vbpFileName
        final String expectedScriptCommand = """\
rm -rf $seqDir/$seqTypeDirName/$seqCenterName/$runDirName/${rawSequenceFile.pathName}${rawSequenceFile?.fileName}
rm -rf $seqDir/$seqTypeDirName/$seqCenterName/$runDirName/${rawSequenceFile.pathName}${rawSequenceFile?.fileName}
rm -rf $seqDir/$seqTypeDirName/$seqCenterName/$runDirName/${rawSequenceFile.pathName}${rawSequenceFile?.fileName}.md5sum
rm -rf $seqDir/$seqTypeDirName/${individual.pid}/$sampleTypeDirName/single/$runDirName/$vbpPath/$vbpFileName
rm -rf $seqDir/$seqTypeDirName/${individual.pid}/$sampleTypeDirName
rm -rf $seqDir/$seqTypeDirName/${individual.pid}
rm -rf $seqDir/$seqTypeDirName/${individual.pid}
"""
        when:
        String deletionScript = deletionService.deleteIndividual(seqTrack.individual)

        then:
        deletionScript == expectedScriptCommand
        Individual.count() == 0
        Sample.count() == 0
        SeqTrack.count() == 0
        SampleIdentifier.count() == 0
        ClusterJob.count() == 0
    }

    void "deleteProjectsExternalWorkflowConfigSelector, should remove project from ExternalWorkflowConfigSelector or delete selector completely"() {
        given:
        setupData()
        final Project project = createProject()
        createExternalWorkflowConfigSelector(projects: [project])
        ExternalWorkflowConfigSelector selector = createExternalWorkflowConfigSelector(projects: [createProject(), project, createProject()])

        when:
        deletionService.deleteProjectsExternalWorkflowConfigSelector(project)

        then:
        ExternalWorkflowConfigSelector.count == 1
        !selector.projects.contains(project)
    }

    void "deleteAllProcessingInformationAndResultOfOneSeqTrack, when seqTrack assigned to mergingWorkPackage with is assigned to SamplePair but no bam file exist"() {
        given:
        setupData()
        SeqTrack seqTrack1 = createSeqTrack()
        SeqTrack seqTrack2 = createSeqTrack()
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage([
                seqTracks: [seqTrack1, seqTrack2] as Set,
        ])
        DomainFactory.createSamplePair([
                mergingWorkPackage1: mergingWorkPackage,
        ])
        assert MergingWorkPackage.count() == 2
        assert SamplePair.count() == 1

        when:
        deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack1)

        then:
        MergingWorkPackage.count() == 1
        SamplePair.count() == 0
    }
}
