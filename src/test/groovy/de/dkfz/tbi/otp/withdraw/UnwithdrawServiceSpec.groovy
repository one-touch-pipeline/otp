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
import de.dkfz.tbi.otp.dataprocessing.indelcalling.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.infrastructure.fastqc.FastqcLinkFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflowExecution.*
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
        RawSequenceDataAllWellFileService rawSequenceDataAllWellFileService = new RawSequenceDataAllWellFileService([
                individualService: new IndividualService(projectService: projectService),
        ])
        LsdfFilesService lsdfFilesService = new LsdfFilesService([
                projectService: projectService,
        ])
        FastqcLinkFileService fastqcLinkFileService = new FastqcLinkFileService([
                rawSequenceDataViewFileService: rawSequenceDataViewFileService,
        ])
        RawSequenceDataWorkFileService rawSequenceDataWorkFileService = new RawSequenceDataWorkFileService(lsdfFilesService: lsdfFilesService)

        RoddyBamFileWithdrawService roddyBamFileWithdrawService = new RoddyBamFileWithdrawService(fileSystemService: fileSystemService)
        CellRangerBamFileWithdrawService cellRangerBamFileWithdrawService = new CellRangerBamFileWithdrawService(fileSystemService: fileSystemService)
        UnwithdrawService service = new UnwithdrawService([
                fileSystemService                : fileSystemService,
                withdrawBamFileServices          : [
                        roddyBamFileWithdrawService,
                        cellRangerBamFileWithdrawService,
                ],
                fastqcLinkFileService            : fastqcLinkFileService,
                rawSequenceDataAllWellFileService: rawSequenceDataAllWellFileService,
                rawSequenceDataWorkFileService   : rawSequenceDataWorkFileService,
                rawSequenceDataViewFileService   : rawSequenceDataViewFileService,
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
                    fastqcLinkFileService.fastqcOutputPath(it),
                    fastqcLinkFileService.fastqcOutputMd5sumPath(it),
                    fastqcLinkFileService.fastqcHtmlPath(it),
            ])
        }

        files.each {
            CreateFileHelper.createFile(it)
        }

        when:
        service.unwithdrawSeqTracks(state)

        then:
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
        state.pathsToChangeGroup == [:]
        state.bamFiles == bamFiles
        [indelCallingInstanceBamFileWithdrawn, indelCallingInstanceFileDeleted].every { it.withdrawn }
    }

    void "test createBashScript, when permission changes given, then generate chmod commands"() {
        given:
        UnwithdrawService service = new UnwithdrawService()
        UnwithdrawStateHolder holder = new UnwithdrawStateHolder()
        holder.script = []
        holder.pathsToChangeGroup = ["/tmp/file1": "group1", "/tmp/file2": "group2"]
        holder.pathsToChangePermissions = [
                "/tmp/fastq1.gz"       : "444",
                "/tmp/fastq2.gz.md5sum": "444",
                "/tmp/fastqc.zip"      : "444",
        ]

        when:
        service.createBashScript(holder)

        then:
        String script = holder.script.join('\n')

        holder.pathsToChangePermissions.each { filePath, permission ->
            assert script.contains("chmod '${permission}' '${filePath}'")
        }

        holder.pathsToChangeGroup.each { path, group ->
            assert script.contains("chgrp --no-dereference --recursive --verbose")
            assert script.contains("'${group}' '${path}'")
        }
    }
}
