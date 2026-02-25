/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.withdraw

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelWorkFileService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelLinkFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.filestore.BaseFolder
import de.dkfz.tbi.otp.filestore.WorkFolder

import java.nio.file.Path
import java.nio.file.Paths

class UnwithdrawServiceSpec extends Specification implements DomainFactoryCore, IsRoddy, DataTest, FastqcDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                BaseFolder,
                CellRangerConfig,
                CellRangerMergingWorkPackage,
                FastqFile,
                FastqcProcessedFile,
                FastqImportInstance,
                FileType,
                IndelCallingInstance,
                Individual,
                MergingWorkPackage,
                Pipeline,
                ProcessingPriority,
                Project,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Sample,
                SamplePair,
                SampleType,
                SampleTypePerProject,
                SingleCellBamFile,
                WorkFolder,
                Workflow,
                WorkflowArtefact,
                WorkflowRun,
        ]
    }

    @TempDir
    Path tempDir

    TestConfigService configService

    // Add the missing service property needed by the test with proper mock behavior
    IndelLinkFileService indelLinkFileService = Mock(IndelLinkFileService) {
        getDirectoryPath(_) >> Paths.get("/tmp/test-analysis-path")
    }

    void setup() {
        configService = new TestConfigService(tempDir)
    }

    void cleanup() {
        configService.clean()
    }

    @Unroll
    void "test unwithdrawSeqTracks (fastqcAvailable: #fastqcAvailable), unwithdraws successfully"() {
        given:
        UnwithdrawStateHolder state = new UnwithdrawStateHolder()

        FileSystemService fileSystemService = new TestFileSystemService()
        ProjectService projectService = new ProjectService(configService: configService, fileSystemService: fileSystemService)
        RawSequenceDataViewFileService rawSequenceDataViewFileService = new RawSequenceDataViewFileService(
                individualService: new IndividualService(projectService: projectService))
        LsdfFilesService lsdfFilesService = new LsdfFilesService([
                projectService                : projectService,
        ])
        FastqcDataFilesService fastqcDataFilesService = new FastqcDataFilesService([
                rawSequenceDataViewFileService: rawSequenceDataViewFileService,
        ])
        RawSequenceDataWorkFileService rawSequenceDataWorkFileService = new RawSequenceDataWorkFileService(lsdfFilesService: lsdfFilesService)

        RoddyBamFileWithdrawService roddyBamFileWithdrawService = new RoddyBamFileWithdrawService(fileSystemService: fileSystemService)
        CellRangerBamFileWithdrawService cellRangerBamFileWithdrawService = new CellRangerBamFileWithdrawService(fileSystemService: fileSystemService)
        UnwithdrawService service = new UnwithdrawService([
                fileSystemService             : fileSystemService,
                withdrawBamFileServices       : [
                        roddyBamFileWithdrawService,
                        cellRangerBamFileWithdrawService,
                ],
                fastqcDataFilesService        : fastqcDataFilesService,
                rawSequenceDataWorkFileService: rawSequenceDataWorkFileService,
                rawSequenceDataViewFileService: rawSequenceDataViewFileService,
        ])

        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile([:], [fileWithdrawn: true])
        List<FastqcProcessedFile> fastqcProcessedFiles = []
        if (fastqcAvailable) {
            seqTrack.sequenceFiles.each {
                fastqcProcessedFiles << createFastqcProcessedFile([sequenceFile: it,])
            }
        }
        state.seqTracksWithComment = [new SeqTrackWithComment(seqTrack, "comment")]

        List<Path> files = []
        seqTrack.sequenceFiles.each {
            files.addAll([
                    rawSequenceDataWorkFileService.getFilePath(it),
                    rawSequenceDataWorkFileService.getMd5sumPath(it),
            ])
        }
        fastqcProcessedFiles.each {
            files.addAll([
                    fastqcDataFilesService.fastqcOutputPath(it),
                    fastqcDataFilesService.fastqcOutputMd5sumPath(it),
                    fastqcDataFilesService.fastqcHtmlPath(it),
            ])
        }

        files.each {
            CreateFileHelper.createFile(it)
        }

        when:
        service.unwithdrawSeqTracks(state)

        then:
        state.linksToCreate == [
                (rawSequenceDataWorkFileService.getFilePath(seqTrack.sequenceFiles.first())): rawSequenceDataViewFileService.getFilePath(seqTrack.sequenceFiles.first()),
                (rawSequenceDataWorkFileService.getFilePath(seqTrack.sequenceFiles.last())) : rawSequenceDataViewFileService.getFilePath(seqTrack.sequenceFiles.last()),
        ]
        state.pathsToChangeGroup.size() == pathsToChangeGroup
        state.bamFiles == []
        seqTrack.sequenceFiles.every { !it.isFileWithdrawn() }

        where:
        fastqcAvailable | pathsToChangeGroup
        true            | 12
        false           | 6
    }

    void "test unwithdrawBamFiles, unwithdraws successfully"() {
        given:
        UnwithdrawStateHolder state = new UnwithdrawStateHolder()

        FileSystemService fileSystemService = new TestFileSystemService()
        ProjectService projectService = new ProjectService(configService: configService, fileSystemService: fileSystemService)
        AbstractBamFileService abstractBamFileService = new AbstractBamFileService(
                individualService: new IndividualService(projectService: projectService))
        RoddyBamFileWithdrawService roddyBamFileWithdrawService = new RoddyBamFileWithdrawService(
                fileSystemService: fileSystemService, abstractBamFileService: abstractBamFileService)
        CellRangerBamFileWithdrawService cellRangerBamFileWithdrawService = new CellRangerBamFileWithdrawService(
                fileSystemService: fileSystemService, abstractBamFileService: abstractBamFileService)
        UnwithdrawService service = new UnwithdrawService([
                abstractBamFileService : abstractBamFileService,
                fileSystemService      : fileSystemService,
                withdrawBamFileServices: [
                        roddyBamFileWithdrawService,
                        cellRangerBamFileWithdrawService,
                ],
        ])

        RoddyBamFile roddyBamFile = createBamFile(withdrawn: true)
        Path roddyPath = abstractBamFileService.getBaseDirectory(roddyBamFile).resolve(roddyBamFile.bamFileName)
        CreateFileHelper.createFile(roddyPath)
        state.seqTracksWithComment.addAll(roddyBamFile.containedSeqTracks.collect { new SeqTrackWithComment(it, "roddy") })

        SingleCellBamFile scBamFile = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createBamFile(withdrawn: true)
        Path scPath = abstractBamFileService.getBaseDirectory(scBamFile).resolve(scBamFile.bamFileName)
        CreateFileHelper.createFile(scPath)
        state.seqTracksWithComment.addAll(scBamFile.containedSeqTracks.collect { new SeqTrackWithComment(it, "sc") })

        when:
        service.unwithdrawBamFiles(state)

        then:
        state.linksToCreate == [:]
        state.pathsToChangeGroup == [(scPath.toString()): scBamFile.project.unixGroup, (roddyPath.toString()): roddyBamFile.project.unixGroup]
        state.bamFiles == [roddyBamFile, scBamFile]
        [roddyBamFile, scBamFile].every { !it.withdrawn }
    }

    void "test unwithdrawBamFiles, files can't be unwithdrawn because the file was deleted, the data file is withdrawn or the processing is not finished"() {
        given:
        UnwithdrawStateHolder state = new UnwithdrawStateHolder()

        FileSystemService fileSystemService = new TestFileSystemService()
        ProjectService projectService = new ProjectService(configService: configService, fileSystemService: fileSystemService)
        AbstractBamFileService abstractBamFileService = new AbstractBamFileService(
                individualService: new IndividualService(projectService: projectService))
        RoddyBamFileWithdrawService roddyBamFileWithdrawService = new RoddyBamFileWithdrawService(
                fileSystemService: fileSystemService, abstractBamFileService: abstractBamFileService)
        CellRangerBamFileWithdrawService cellRangerBamFileWithdrawService = new CellRangerBamFileWithdrawService(
                fileSystemService: fileSystemService, abstractBamFileService: abstractBamFileService)
        UnwithdrawService service = new UnwithdrawService([
                abstractBamFileService : abstractBamFileService,
                fileSystemService      : fileSystemService,
                withdrawBamFileServices: [
                        roddyBamFileWithdrawService,
                        cellRangerBamFileWithdrawService,
                ],
        ])

        RoddyBamFile bamFileFileDeleted = createBamFile(withdrawn: true)
        state.seqTracksWithComment.addAll(bamFileFileDeleted.containedSeqTracks.collect { new SeqTrackWithComment(it, "") })

        SingleCellBamFile bamFileDataFileWithdrawn = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createBamFile(withdrawn: true)
        CreateFileHelper.createFile(abstractBamFileService.getBaseDirectory(bamFileDataFileWithdrawn).resolve(bamFileDataFileWithdrawn.bamFileName))
        bamFileDataFileWithdrawn.containedSeqTracks.each {
            it.sequenceFiles.each {
                it.fileWithdrawn = true
                it.save()
            }
        }
        state.seqTracksWithComment.addAll(bamFileDataFileWithdrawn.containedSeqTracks.collect { new SeqTrackWithComment(it, "") })

        SingleCellBamFile bamFileDataFileUnfinished = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createBamFile(
                fileOperationStatus: AbstractBamFile.FileOperationStatus.DECLARED, withdrawn: true)
        CreateFileHelper.createFile(abstractBamFileService.getBaseDirectory(bamFileDataFileUnfinished).resolve(bamFileDataFileUnfinished.bamFileName))
        state.seqTracksWithComment.addAll(bamFileDataFileUnfinished.containedSeqTracks.collect { new SeqTrackWithComment(it, "") })

        when:
        service.unwithdrawBamFiles(state)

        then:
        state.linksToCreate == [:]
        state.pathsToChangeGroup == [:]
        state.bamFiles == []
        [bamFileFileDeleted, bamFileDataFileWithdrawn, bamFileDataFileUnfinished].every { it.withdrawn }
    }

    void "test unwithdrawAnalysis, analyses can't be unwithdrawn because the file was deleted, the bam file is withdrawn or the processing is not finished"() {
        given:
        UnwithdrawStateHolder state = new UnwithdrawStateHolder()

        IndelCallingService indelService = new IndelCallingService()
        indelService.individualService = new IndividualService()
        indelService.individualService.projectService = new ProjectService()
        indelService.individualService.projectService.configService = configService
        indelService.individualService.projectService.fileSystemService = new TestFileSystemService()

        FileSystemService fileSystemService = new TestFileSystemService()

        // Set up the new analysisLinkFileServiceFactoryService with proper dependencies
        AnalysisLinkFileServiceFactoryService analysisLinkFileServiceFactory = new AnalysisLinkFileServiceFactoryService(
                indelLinkFileService: indelLinkFileService
        )

        // Create IndelWorkFileService with proper dependencies
        IndelWorkFileService indelWorkFileService = new IndelWorkFileService()
        indelWorkFileService.analysisLinkFileServiceFactoryService = analysisLinkFileServiceFactory

        WithdrawAnalysisService withdrawAnalysisService = new WithdrawAnalysisService(
                analysisLinkFileServiceFactoryService: analysisLinkFileServiceFactory,
                analysisWorkFileServiceFactoryService: new AnalysisWorkFileServiceFactoryService(
                        indelWorkFileService: indelWorkFileService
                )
        )
        UnwithdrawService service = new UnwithdrawService([
                fileSystemService      : fileSystemService,
                withdrawAnalysisService: withdrawAnalysisService,
        ])

        IndelCallingInstance indelCallingInstanceBamFileWithdrawn = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(
                processingState: AnalysisProcessingStates.FINISHED, withdrawn: true)
        indelCallingInstanceBamFileWithdrawn.sampleType1BamFile.withdrawn = true
        indelCallingInstanceBamFileWithdrawn.sampleType1BamFile.save(flush: true)
        CreateFileHelper.createFile(indelService.getWorkDirectory(indelCallingInstanceBamFileWithdrawn))

        IndelCallingInstance indelCallingInstanceFileDeleted = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(
                processingState: AnalysisProcessingStates.FINISHED, withdrawn: true)

        IndelCallingInstance indelCallingInstanceUnfinished = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(
                processingState: AnalysisProcessingStates.IN_PROGRESS, withdrawn: true)
        CreateFileHelper.createFile(indelService.getWorkDirectory(indelCallingInstanceUnfinished))

        List<AbstractBamFile> bamFiles = [indelCallingInstanceBamFileWithdrawn.sampleType1BamFile, indelCallingInstanceBamFileWithdrawn.sampleType2BamFile,
                                          indelCallingInstanceFileDeleted.sampleType1BamFile, indelCallingInstanceFileDeleted.sampleType2BamFile,
                                          indelCallingInstanceUnfinished.sampleType1BamFile, indelCallingInstanceUnfinished.sampleType2BamFile]
        state.bamFiles = bamFiles

        service.withdrawAnalysisService.fileService = Mock(FileService) {
            fileExists(_) >> true
        }

        when:
        service.unwithdrawAnalysis(state)

        then:
        state.linksToCreate == [:]
        state.pathsToChangeGroup == [:]
        state.bamFiles == bamFiles
        [indelCallingInstanceBamFileWithdrawn, indelCallingInstanceFileDeleted].every { it.withdrawn }
    }

    void "test unWithdrawRawSequenceFiles, when old workflow data given, then collect paths for permission restoration"() {
        given:
        RawSequenceFile oldWorkflowFile = createFastqFile([fileWithdrawn: true, withdrawnComment: "test withdrawal"])
        oldWorkflowFile.seqTrack.workflowArtefact = null // Simulate old workflow data

        final Path oldFilePath = CreateFileHelper.createFile(tempDir.resolve("oldFastq.gz"))
        final Path oldMd5Path = CreateFileHelper.createFile(tempDir.resolve("oldFastq.gz.md5sum"))

        UnwithdrawService service = new UnwithdrawService()
        service.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService) {
            3 * getFilePath(oldWorkflowFile) >> oldFilePath
            2 * getMd5sumPath(oldWorkflowFile) >> oldMd5Path
        }
        service.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            2 * getFilePath(oldWorkflowFile) >> Paths.get("/tmp/view")
        }
        service.fastqcDataFilesService = Mock(FastqcDataFilesService)

        UnwithdrawStateHolder holder = new UnwithdrawStateHolder()

        when:
        service.unwithdrawRawSequenceFiles(oldWorkflowFile, "unwithdraw comment", holder)

        then:
        holder.pathsToChangePermissions.containsKey(oldFilePath.toString())
        holder.pathsToChangePermissions.get(oldFilePath.toString()) == "444"
        holder.pathsToChangePermissions.containsKey(oldMd5Path.toString())
        holder.pathsToChangePermissions.get(oldMd5Path.toString()) == "444"
        holder.pathsToChangeGroup.size() > 0

        !oldWorkflowFile.fileWithdrawn
        oldWorkflowFile.withdrawnDate == null
    }

    void "test unWithdrawRawSequenceFiles, when new workflow data given, then skip permission restoration"() {
        given:
        RawSequenceFile newWorkflowFile = createFastqFile([fileWithdrawn: true, withdrawnComment: "test withdrawal"])
        BaseFolder baseFolder = new BaseFolder(
            path: "/tmp/test-base-folder-${System.currentTimeMillis()}",
            writable: true
        )
        baseFolder.save(flush: true)
        WorkFolder workFolder = new WorkFolder(
            baseFolder: baseFolder,
            uuid: UUID.randomUUID(),
            size: 0
        )
        workFolder.save(flush: true)

        Workflow workflow = new Workflow(
            name: "TestWorkflow-${System.currentTimeMillis()}",
            enabled: true,
            maxParallelWorkflows: 1
        )
        workflow.save(flush: true)

        WorkflowRun workflowRun = new WorkflowRun(
            workFolder: workFolder,
            state: WorkflowRun.State.SUCCESS,
            displayName: "Test Workflow Run",
            shortDisplayName: "TestRun",
            project: newWorkflowFile.project,
            workflow: workflow,
            priority: newWorkflowFile.seqTrack.sample.individual.project.processingPriority
        )
        workflowRun.save(flush: true)

        WorkflowArtefact workflowArtefact = new WorkflowArtefact(
            producedBy: workflowRun,
            state: WorkflowArtefact.State.SUCCESS,
            displayName: "Test Workflow Artefact",
            artefactType: ArtefactType.FASTQ,
            outputRole: "test"
        )
        workflowArtefact.save(flush: true)

        newWorkflowFile.seqTrack.workflowArtefact = workflowArtefact
        newWorkflowFile.seqTrack.save(flush: true)

        final Path newFilePath = CreateFileHelper.createFile(tempDir.resolve("newFastq.gz"))
        final Path newMd5Path = CreateFileHelper.createFile(tempDir.resolve("newFastq.gz.md5sum"))

        UnwithdrawService service = new UnwithdrawService()
        service.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService) {
            2 * getFilePath(newWorkflowFile) >> newFilePath
            1 * getMd5sumPath(newWorkflowFile) >> newMd5Path
        }
        service.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            2 * getFilePath(newWorkflowFile) >> Paths.get("/tmp/view")
        }
        service.fastqcDataFilesService = Mock(FastqcDataFilesService)

        UnwithdrawStateHolder holder = new UnwithdrawStateHolder()

        when:
        service.unwithdrawRawSequenceFiles(newWorkflowFile, "unwithdraw comment", holder)

        then:
        holder.pathsToChangePermissions.isEmpty()

        holder.pathsToChangeGroup.size() > 0

        !newWorkflowFile.fileWithdrawn
        newWorkflowFile.withdrawnDate == null
    }

    void "test createBashScript, when permission changes given, then generate chmod commands"() {
        given:
        UnwithdrawService service = new UnwithdrawService()
        UnwithdrawStateHolder holder = new UnwithdrawStateHolder()
        holder.script = []
        holder.linksToCreate = [(Paths.get("/tmp/source")): Paths.get("/tmp/link")]
        holder.pathsToChangeGroup = ["/tmp/file1": "group1", "/tmp/file2": "group2"]
        holder.pathsToChangePermissions = [
                "/tmp/fastq1.gz": "444",
                "/tmp/fastq2.gz.md5sum": "444",
                "/tmp/fastqc.zip": "444",
        ]

        when:
        service.createBashScript(holder)

        then:
        String script = holder.script.join('\n')

        holder.pathsToChangePermissions.each { filePath, permission ->
            assert script.contains("chmod ${permission} ${filePath}")
        }

        holder.pathsToChangeGroup.each { path, group ->
            assert script.contains("chgrp --recursive --verbose ${group} ${path}")
        }

        assert script.contains("mkdir -p")
        assert script.contains("ln -rs")
    }

    void "test unWithdrawRawSequenceFiles, when fastq new workflow but fastqc old workflow, then restore fastqc permissions only"() {
        given:
        RawSequenceFile newWorkflowFastqFile = createFastqFile([fileWithdrawn: true, withdrawnComment: "test withdrawal"])
        BaseFolder baseFolder = new BaseFolder(
                path: "/tmp/test-base-folder-${System.currentTimeMillis()}",
                writable: true
        )
        baseFolder.save(flush: true)
        WorkFolder workFolder = new WorkFolder(
                baseFolder: baseFolder,
                uuid: UUID.randomUUID(),
                size: 0
        )
        workFolder.save(flush: true)

        Workflow workflow = new Workflow(
                name: "TestWorkflow-${System.currentTimeMillis()}",
                enabled: true,
                maxParallelWorkflows: 1
        )
        workflow.save(flush: true)

        WorkflowRun workflowRun = new WorkflowRun(
                workFolder: workFolder,
                state: WorkflowRun.State.SUCCESS,
                displayName: "Test Workflow Run",
                shortDisplayName: "TestRun",
                project: newWorkflowFastqFile.project,
                workflow: workflow,
                priority: newWorkflowFastqFile.seqTrack.sample.individual.project.processingPriority
        )
        workflowRun.save(flush: true)

        WorkflowArtefact workflowArtefact = new WorkflowArtefact(
                producedBy: workflowRun,
                state: WorkflowArtefact.State.SUCCESS,
                displayName: "Test Workflow Artefact",
                artefactType: ArtefactType.FASTQ,
                outputRole: "test"
        )
        workflowArtefact.save(flush: true)

        newWorkflowFastqFile.seqTrack.workflowArtefact = workflowArtefact
        newWorkflowFastqFile.seqTrack.save(flush: true)

        FastqcProcessedFile oldWorkflowFastqcFile = createFastqcProcessedFile([
                sequenceFile: newWorkflowFastqFile,
                workflowArtefact: null, // Old workflow - no artefact
        ])

        final Path newFastqPath = CreateFileHelper.createFile(tempDir.resolve("newFastq.gz"))
        final Path newMd5Path = CreateFileHelper.createFile(tempDir.resolve("newFastq.gz.md5sum"))
        final Path oldFastqcZip = CreateFileHelper.createFile(tempDir.resolve("oldFastqc.zip"))
        final Path oldFastqcMd5 = CreateFileHelper.createFile(tempDir.resolve("oldFastqc.zip.md5sum"))
        final Path oldFastqcHtml = CreateFileHelper.createFile(tempDir.resolve("oldFastqc.html"))

        UnwithdrawService service = new UnwithdrawService()
        service.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService) {
            2 * getFilePath(newWorkflowFastqFile) >> newFastqPath
            1 * getMd5sumPath(newWorkflowFastqFile) >> newMd5Path
        }
        service.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            2 * getFilePath(newWorkflowFastqFile) >> Paths.get("/tmp/view")
        }
        service.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            2 * fastqcOutputPath(oldWorkflowFastqcFile) >> oldFastqcZip
            2 * fastqcOutputMd5sumPath(oldWorkflowFastqcFile) >> oldFastqcMd5
            2 * fastqcHtmlPath(oldWorkflowFastqcFile) >> oldFastqcHtml
        }

        UnwithdrawStateHolder holder = new UnwithdrawStateHolder()

        when:
        service.unwithdrawRawSequenceFiles(newWorkflowFastqFile, "unwithdraw comment", holder)

        then:
        !holder.pathsToChangePermissions.containsKey(newFastqPath.toString())
        !holder.pathsToChangePermissions.containsKey(newMd5Path.toString())

        holder.pathsToChangePermissions.containsKey(oldFastqcZip.toString())
        holder.pathsToChangePermissions.get(oldFastqcZip.toString()) == "444"
        holder.pathsToChangePermissions.containsKey(oldFastqcMd5.toString())
        holder.pathsToChangePermissions.get(oldFastqcMd5.toString()) == "444"
        holder.pathsToChangePermissions.containsKey(oldFastqcHtml.toString())
        holder.pathsToChangePermissions.get(oldFastqcHtml.toString()) == "444"

        holder.pathsToChangeGroup.size() > 0

        !newWorkflowFastqFile.fileWithdrawn
        newWorkflowFastqFile.withdrawnDate == null
    }
}
