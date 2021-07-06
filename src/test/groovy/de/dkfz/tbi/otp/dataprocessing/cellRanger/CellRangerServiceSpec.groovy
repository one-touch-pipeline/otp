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
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeIndexService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightCheckService
import de.dkfz.tbi.otp.utils.*

import java.nio.file.*

class CellRangerServiceSpec extends Specification implements CellRangerFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergedBamFile,
                CellRangerMergingWorkPackage,
                CellRangerConfig,
                DataFile,
                Individual,
                LibraryPreparationKit,
                FileType,
                MergingCriteria,
                MetaDataEntry,
                MetaDataKey,
                Pipeline,
                Project,
                Realm,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                ReferenceGenomeIndex,
                Run,
                FastqImportInstance,
                SeqTrack,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqType,
                SingleCellBamFile,
                SoftwareTool,
                ToolName,
        ]
    }

    @Rule
    TemporaryFolder temporaryFolder

    void "createInputDirectoryStructure, if singleCellBamFile, then recreate the input structure"() {
        given:
        new TestConfigService()

        String sampleIdentifier1 = "qwert"
        CellRangerMergingWorkPackage mwp = createMergingWorkPackage()
        SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithDataFiles(mwp,
                [ sampleIdentifier: sampleIdentifier1, ]
        )
        SingleCellBamFile singleCellBamFile = createBamFile([workPackage: mwp, seqTracks: [seqTrack1]])
        Path sampleDirectory = singleCellBamFile.sampleDirectory.toPath()

        Path mate1 = sampleDirectory.resolve(sampleIdentifier1).resolve("${singleCellBamFile.singleCellSampleName}_S1_L001_R1_001.fastq.gz")
        Path mate2 = sampleDirectory.resolve(sampleIdentifier1).resolve("${singleCellBamFile.singleCellSampleName}_S1_L001_R2_001.fastq.gz")

        String file1 = 'file1'
        String file2 = 'file2'

        Path filePath1 = Paths.get(file1)
        Path filePath2 = Paths.get(file2)

        String sampleIdentifier2 = "as *ÄÜ?°!§%&/()=?`dfg"
        String sampleIdentifier2DirName = "as________________dfg"
        Path mate3 = sampleDirectory.resolve(sampleIdentifier2DirName).resolve("${singleCellBamFile.singleCellSampleName}_S1_L001_R1_001.fastq.gz")
        Path mate4 = sampleDirectory.resolve(sampleIdentifier2DirName).resolve("${singleCellBamFile.singleCellSampleName}_S1_L001_R2_001.fastq.gz")

        String file3 = 'file1'
        String file4 = 'file2'

        Path filePath3 = Paths.get(file3)
        Path filePath4 = Paths.get(file4)

        Path sampleIdentifierPath = sampleDirectory.resolve(singleCellBamFile.seqTracks.first().sampleIdentifier)

        SeqTrack seqTrack2 = DomainFactory.createSeqTrackWithDataFiles(CellRangerMergingWorkPackage.all.find(),
                [ sampleIdentifier: sampleIdentifier2, ]
        )
        singleCellBamFile.seqTracks.add(seqTrack2)
        singleCellBamFile.save(flush: true)

        CellRangerService cellRangerService = new CellRangerService([
                fileSystemService: Mock(FileSystemService) {
                    1 * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                lsdfFilesService : Mock(LsdfFilesService),
                fileService      : Mock(FileService),
        ])

        when:
        cellRangerService.createInputDirectoryStructure(singleCellBamFile)

        then:
        1 * cellRangerService.fileService.deleteDirectoryRecursively(sampleDirectory)

        then:
        1 * cellRangerService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(sampleDirectory, _, _)

        then:
        1 * cellRangerService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(sampleIdentifierPath, _, _)

        2 * cellRangerService.lsdfFilesService.getFileViewByPidPath(_) >>> [
                file1,
                file2,
        ]
        1 * cellRangerService.fileService.createLink(mate1, filePath1, singleCellBamFile.realm, _)
        1 * cellRangerService.fileService.createLink(mate2, filePath2, singleCellBamFile.realm, _)

        then:
        1 * cellRangerService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(sampleDirectory.resolve(sampleIdentifier2DirName), _, _)

        2 * cellRangerService.lsdfFilesService.getFileViewByPidPath(_) >>> [
                file3,
                file4,
        ]
        1 * cellRangerService.fileService.createLink(mate3, filePath3, singleCellBamFile.realm, _)
        1 * cellRangerService.fileService.createLink(mate4, filePath4, singleCellBamFile.realm, _)
    }

    void "deleteOutputDirectoryStructureIfExists, if singleCellBamFile given, then delete the output directory"() {
        given:
        new TestConfigService()

        SingleCellBamFile singleCellBamFile = createBamFile()
        Path outputDirectory = singleCellBamFile.outputDirectory.toPath()

        CellRangerService cellRangerService = new CellRangerService([
                fileSystemService: Mock(FileSystemService) {
                    1 * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService      : Mock(FileService),
        ])

        when:
        cellRangerService.deleteOutputDirectoryStructureIfExists(singleCellBamFile)

        then:
        1 * cellRangerService.fileService.deleteDirectoryRecursively(outputDirectory)
    }

    void "validateFilesExistsInResultDirectory, if singleCellBamFile given and all files exist, then throw no exception"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        SingleCellBamFile singleCellBamFile = createBamFile()
        File result = singleCellBamFile.resultDirectory

        SingleCellBamFile.CREATED_RESULT_FILES.each {
            CreateFileHelper.createFile(new File(result, it))
        }

        SingleCellBamFile.CREATED_RESULT_DIRS.each {
            CreateFileHelper.createFile(new File(new File(result, it), 'DummyFile'))
        }

        CellRangerService cellRangerService = new CellRangerService([
                fileSystemService: Mock(FileSystemService) {
                    1 * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService      : new FileService(),
        ])

        when:
        cellRangerService.validateFilesExistsInResultDirectory(singleCellBamFile)

        then:
        noExceptionThrown()
    }

    @Unroll
    void "validateFilesExistsInResultDirectory, if singleCellBamFile given and file/directory '#missingFile' does not exist, then throw an assert"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        SingleCellBamFile singleCellBamFile = createBamFile()
        File result = singleCellBamFile.resultDirectory

        (SingleCellBamFile.CREATED_RESULT_FILES - missingFile).each {
            CreateFileHelper.createFile(new File(result, it))
        }

        (SingleCellBamFile.CREATED_RESULT_DIRS - missingFile).each {
            CreateFileHelper.createFile(new File(new File(result, it), 'DummyFile'))
        }

        CellRangerService cellRangerService = new CellRangerService([
                fileSystemService: Mock(FileSystemService) {
                    1 * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService      : new FileService(),
        ])

        when:
        cellRangerService.validateFilesExistsInResultDirectory(singleCellBamFile)

        then:
        AssertionError e = thrown()
        e.message.contains(missingFile)

        where:
        missingFile << SingleCellBamFile.CREATED_RESULT_FILES_AND_DIRS
    }

    @Unroll
    void "createCellRangerParameters, adds EXPECT_CELLS and FORCE_CELLS parameters depending on respective fields in singleCellBamFile (expect=#expectedKey)"() {
        given:
        new TestConfigService()

        final File indexFile = new File(TestCase.uniqueNonExistentPath, 'someIndex')

        String sampleIdentifier = "abc *ÄÜ?°!§%&/()=?`def"
        CellRangerMergingWorkPackage mwp = createMergingWorkPackage([
                expectedCells: expectedCells,
                enforcedCells: enforcedCells,
        ])
        SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithDataFiles(mwp,
                [ sampleIdentifier: sampleIdentifier, ]
        )
        SingleCellBamFile singleCellBamFile = createBamFile([workPackage: mwp, seqTracks: [seqTrack1]])

        CellRangerService cellRangerService = new CellRangerService([
                referenceGenomeIndexService: Mock(ReferenceGenomeIndexService) {
                    1 * getFile(_) >> indexFile
                },
                processingOptionService    : Mock(ProcessingOptionService) {
                    1 * findOptionAsString(ProcessingOption.OptionName.PIPELINE_CELLRANGER_CORE_COUNT) >> '15'
                    1 * findOptionAsString(ProcessingOption.OptionName.PIPELINE_CELLRANGER_CORE_MEM) >> '60'
                },
        ])

        when:
        Map<String, String> map = cellRangerService.createCellRangerParameters(singleCellBamFile)

        then:
        map[CellRangerParameters.ID.parameterName] == singleCellBamFile.singleCellSampleName
        map[CellRangerParameters.FASTQ.parameterName] == new File(singleCellBamFile.sampleDirectory, "abc________________def").absolutePath
        map[CellRangerParameters.TRANSCRIPTOME.parameterName] == indexFile.absolutePath
        map[CellRangerParameters.SAMPLE.parameterName] == singleCellBamFile.singleCellSampleName
        map[CellRangerParameters.LOCAL_CORES.parameterName] ==~ /\d+/
        map[CellRangerParameters.LOCAL_MEM.parameterName] ==~ /\d+/

        map[setKey.parameterName] == "5000"
        !map.containsKey(unsetKey.parameterName)

        where:
        expectedCells | enforcedCells | setKey                            | unsetKey
        5000          | null          | CellRangerParameters.EXPECT_CELLS | CellRangerParameters.FORCE_CELLS
        null          | 5000          | CellRangerParameters.FORCE_CELLS  | CellRangerParameters.EXPECT_CELLS
    }

    @Unroll
    void "finishCellRangerWorkflow, if bam state is #state, then do necessary work and update database"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        String md5sum = HelperUtils.randomMd5sum
        SingleCellBamFile singleCellBamFile = createBamFile([
                fileOperationStatus: state,
        ])
        singleCellBamFile.metaClass.isMostRecentBamFile = { -> true } //use criteria which do not work in unit tests

        createResultFiles(singleCellBamFile)

        CellRangerService cellRangerService = new CellRangerService([
                cellRangerWorkflowService   : Mock(CellRangerWorkflowService) {
                    1 * cleanupOutputDirectory(singleCellBamFile)
                    1 * linkResultFiles(singleCellBamFile)
                },
                abstractMergedBamFileService: Mock(AbstractMergedBamFileService) {
                    1 * updateSamplePairStatusToNeedProcessing(singleCellBamFile)
                },
                md5SumService               : Mock(Md5SumService) {
                    1 * extractMd5Sum(_) >> md5sum
                },
                qcTrafficLightCheckService  : Mock(QcTrafficLightCheckService) {
                    1 * handleQcCheck(singleCellBamFile, _) >> { AbstractMergedBamFile bam, Closure closure ->
                        closure()
                    }
                },
                fileSystemService           : Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
        ])

        when:
        cellRangerService.finishCellRangerWorkflow(singleCellBamFile)

        then:
        singleCellBamFile.refresh()
        singleCellBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
        singleCellBamFile.fileSize > 0
        singleCellBamFile.md5sum == md5sum
        singleCellBamFile.fileExists
        singleCellBamFile.dateFromFileSystem != null
        singleCellBamFile.mergingWorkPackage.bamFileInProjectFolder == singleCellBamFile

        where:
        state << [
                AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
                AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                AbstractMergedBamFile.FileOperationStatus.PROCESSED,
        ]
    }

    void "finishCellRangerWorkflow, if bam state is DECLARED, then throw assertion and do not change database"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        String md5sum = HelperUtils.randomMd5sum
        SingleCellBamFile singleCellBamFile = createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
        ])
        singleCellBamFile.metaClass.isMostRecentBamFile = { -> true } //use criteria which do not work in unit tests

        createResultFiles(singleCellBamFile)

        CellRangerService cellRangerService = new CellRangerService([
                cellRangerWorkflowService   : Mock(CellRangerWorkflowService) {
                    _ * cleanupOutputDirectory(singleCellBamFile)
                    _ * linkResultFiles(singleCellBamFile)
                },
                abstractMergedBamFileService: Mock(AbstractMergedBamFileService) {
                    0 * updateSamplePairStatusToNeedProcessing(singleCellBamFile)
                },
                md5SumService               : Mock(Md5SumService) {
                    _ * extractMd5Sum(_) >> md5sum
                },
                qcTrafficLightCheckService  : Mock(QcTrafficLightCheckService) {
                    0 * handleQcCheck(singleCellBamFile, _) >> { AbstractMergedBamFile bam, Closure closure ->
                        closure()
                    }
                },
                fileSystemService           : Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
        ])

        when:
        cellRangerService.finishCellRangerWorkflow(singleCellBamFile)

        then:
        AssertionError e = thrown()
        e.message.contains('contains(singleCellBamFile.fileOperationStatus)')

        singleCellBamFile.refresh()
        singleCellBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.DECLARED
        singleCellBamFile.md5sum == null
        !singleCellBamFile.fileExists
        singleCellBamFile.dateFromFileSystem == null
        singleCellBamFile.mergingWorkPackage.bamFileInProjectFolder == null
    }

    void "sampleIdentifierForDirectoryStructure, invalid characters are converted to _"() {
        expect:
        new CellRangerService().sampleIdentifierForDirectoryStructure("a${input}b") == "a_b"

        where:
        input << " *ÄÜ?°!§%&/()=?`".split("")
    }

    void "sampleIdentifierForDirectoryStructure, string without invalid characters, no changes"() {
        given:
        String validString = "abcABC123_-"

        expect:
        new CellRangerService().sampleIdentifierForDirectoryStructure(validString) == validString
    }
}
