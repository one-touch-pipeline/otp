/*
 * Copyright 2011-2021 The OTP authors
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
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.Path

class UnwithdrawServiceSpec extends Specification implements DomainFactoryCore, IsRoddy, DataTest, FastqcDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                CellRangerConfig,
                CellRangerMergingWorkPackage,
                FastqcProcessedFile,
                FastqImportInstance,
                FileType,
                IndelCallingInstance,
                Individual,
                MergingWorkPackage,
                Pipeline,
                ProcessingPriority,
                Project,
                Realm,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Sample,
                SamplePair,
                SampleType,
                SampleTypePerProject,
                SingleCellBamFile,
        ]
    }

    @TempDir
    Path tempDir

    TestConfigService configService

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
        LsdfFilesService lsdfFilesService = new LsdfFilesService(projectService: projectService,
                individualService: new IndividualService(projectService: projectService))
        FastqcDataFilesService fastqcDataFilesService = new FastqcDataFilesService(lsdfFilesService: lsdfFilesService)

        RoddyBamFileWithdrawService roddyBamFileWithdrawService = new RoddyBamFileWithdrawService(fileSystemService: fileSystemService)
        CellRangerBamFileWithdrawService cellRangerBamFileWithdrawService = new CellRangerBamFileWithdrawService(fileSystemService: fileSystemService)
        UnwithdrawService service = new UnwithdrawService([
                fileSystemService      : fileSystemService,
                withdrawBamFileServices: [
                        roddyBamFileWithdrawService,
                        cellRangerBamFileWithdrawService,
                ],
                lsdfFilesService       : lsdfFilesService,
                fastqcDataFilesService : fastqcDataFilesService,
        ])

        SeqTrack seqTrack = createSeqTrackWithTwoDataFile([:], [fileWithdrawn: true])
        List<FastqcProcessedFile> fastqcProcessedFiles = []
        if (fastqcAvailable) {
            seqTrack.dataFiles.each {
                fastqcProcessedFiles << createFastqcProcessedFile([dataFile: it,])
            }
        }
        state.seqTracksWithComment = [new SeqTrackWithComment(seqTrack, "comment")]

        List<Path> files = []
        seqTrack.dataFiles.each {
            files.addAll([
                    lsdfFilesService.getFileFinalPathAsPath(it),
                    lsdfFilesService.getFileMd5sumFinalPathAsPath(it),
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
                (lsdfFilesService.getFileFinalPathAsPath(seqTrack.dataFiles.first())): lsdfFilesService.getFileViewByPidPathAsPath(seqTrack.dataFiles.first()),
                (lsdfFilesService.getFileFinalPathAsPath(seqTrack.dataFiles.last())) : lsdfFilesService.getFileViewByPidPathAsPath(seqTrack.dataFiles.last()),
        ]
        state.pathsToChangeGroup.size() == pathsToChangeGroup
        state.mergedBamFiles == []
        seqTrack.dataFiles.every { !it.isFileWithdrawn() }

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
        AbstractMergedBamFileService abstractMergedBamFileService = new AbstractMergedBamFileService(
                individualService: new IndividualService(projectService: projectService))
        RoddyBamFileWithdrawService roddyBamFileWithdrawService = new RoddyBamFileWithdrawService(
                fileSystemService: fileSystemService, abstractMergedBamFileService: abstractMergedBamFileService)
        CellRangerBamFileWithdrawService cellRangerBamFileWithdrawService = new CellRangerBamFileWithdrawService(
                fileSystemService: fileSystemService, abstractMergedBamFileService: abstractMergedBamFileService)
        UnwithdrawService service = new UnwithdrawService([
                abstractMergedBamFileService: abstractMergedBamFileService,
                fileSystemService           : fileSystemService,
                withdrawBamFileServices     : [
                        roddyBamFileWithdrawService,
                        cellRangerBamFileWithdrawService,
                ],
        ])

        RoddyBamFile roddyBamFile = createBamFile(withdrawn: true)
        Path roddyPath = abstractMergedBamFileService.getBaseDirectory(roddyBamFile).resolve(roddyBamFile.bamFileName)
        CreateFileHelper.createFile(roddyPath)
        state.seqTracksWithComment.addAll(roddyBamFile.containedSeqTracks.collect { new SeqTrackWithComment(it, "roddy") })

        SingleCellBamFile scBamFile = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createBamFile(withdrawn: true)
        Path scPath = abstractMergedBamFileService.getBaseDirectory(scBamFile).resolve(scBamFile.bamFileName)
        CreateFileHelper.createFile(scPath)
        state.seqTracksWithComment.addAll(scBamFile.containedSeqTracks.collect { new SeqTrackWithComment(it, "sc") })

        when:
        service.unwithdrawBamFiles(state)

        then:
        state.linksToCreate == [:]
        state.pathsToChangeGroup == [(scPath.toString()): scBamFile.project.unixGroup, (roddyPath.toString()): roddyBamFile.project.unixGroup]
        state.mergedBamFiles == [roddyBamFile, scBamFile]
        [roddyBamFile, scBamFile].every { !it.withdrawn }
    }

    void "test unwithdrawBamFiles, files can't be unwithdrawn because the file was deleted, the data file is withdrawn or the processing is not finished"() {
        given:
        UnwithdrawStateHolder state = new UnwithdrawStateHolder()

        FileSystemService fileSystemService = new TestFileSystemService()
        ProjectService projectService = new ProjectService(configService: configService, fileSystemService: fileSystemService)
        AbstractMergedBamFileService abstractMergedBamFileService = new AbstractMergedBamFileService(
                individualService: new IndividualService(projectService: projectService))
        RoddyBamFileWithdrawService roddyBamFileWithdrawService = new RoddyBamFileWithdrawService(
                fileSystemService: fileSystemService, abstractMergedBamFileService: abstractMergedBamFileService)
        CellRangerBamFileWithdrawService cellRangerBamFileWithdrawService = new CellRangerBamFileWithdrawService(
                fileSystemService: fileSystemService, abstractMergedBamFileService: abstractMergedBamFileService)
        UnwithdrawService service = new UnwithdrawService([
                abstractMergedBamFileService: abstractMergedBamFileService,
                fileSystemService           : fileSystemService,
                withdrawBamFileServices     : [
                        roddyBamFileWithdrawService,
                        cellRangerBamFileWithdrawService,
                ],
        ])

        RoddyBamFile bamFileFileDeleted = createBamFile(withdrawn: true)
        state.seqTracksWithComment.addAll(bamFileFileDeleted.containedSeqTracks.collect { new SeqTrackWithComment(it, "") })

        SingleCellBamFile bamFileDataFileWithdrawn = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createBamFile(withdrawn: true)
        CreateFileHelper.createFile(abstractMergedBamFileService.getBaseDirectory(bamFileDataFileWithdrawn).resolve(bamFileDataFileWithdrawn.bamFileName))
        bamFileDataFileWithdrawn.containedSeqTracks.each {
            it.dataFiles*.fileWithdrawn = true
            it.dataFiles*.save()
        }
        state.seqTracksWithComment.addAll(bamFileDataFileWithdrawn.containedSeqTracks.collect { new SeqTrackWithComment(it, "") })

        SingleCellBamFile bamFileDataFileUnfinished = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createBamFile(
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED, withdrawn: true)
        CreateFileHelper.createFile(abstractMergedBamFileService.getBaseDirectory(bamFileDataFileUnfinished).resolve(bamFileDataFileUnfinished.bamFileName))
        state.seqTracksWithComment.addAll(bamFileDataFileUnfinished.containedSeqTracks.collect { new SeqTrackWithComment(it, "") })

        when:
        service.unwithdrawBamFiles(state)

        then:
        state.linksToCreate == [:]
        state.pathsToChangeGroup == [:]
        state.mergedBamFiles == []
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
        WithdrawAnalysisService withdrawAnalysisService = new WithdrawAnalysisService(
                bamFileAnalysisServiceFactoryService: new BamFileAnalysisServiceFactoryService(
                        indelCallingService: indelService
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

        List<AbstractMergedBamFile> bamFiles = [indelCallingInstanceBamFileWithdrawn.sampleType1BamFile, indelCallingInstanceBamFileWithdrawn.sampleType2BamFile,
                                                indelCallingInstanceFileDeleted.sampleType1BamFile, indelCallingInstanceFileDeleted.sampleType2BamFile,
                                                indelCallingInstanceUnfinished.sampleType1BamFile, indelCallingInstanceUnfinished.sampleType2BamFile]
        state.mergedBamFiles = bamFiles

        when:
        service.unwithdrawAnalysis(state)

        then:
        state.linksToCreate == [:]
        state.pathsToChangeGroup == [:]
        state.mergedBamFiles == bamFiles
        [indelCallingInstanceBamFileWithdrawn, indelCallingInstanceFileDeleted].every { it.withdrawn }
    }
}
