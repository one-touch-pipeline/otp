package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.test.mixin.Mock
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
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightCheckService
import de.dkfz.tbi.otp.utils.*

import java.nio.file.*

@Mock([
        AbstractMergedBamFile,
        CellRangerMergingWorkPackage,
        CellRangerConfig,
        DataFile,
        Individual,
        LibraryPreparationKit,
        FileType,
        MergingCriteria,
        Pipeline,
        Project,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        ReferenceGenomeIndex,
        Run,
        RunSegment,
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
])
class CellRangerServiceSpec extends Specification implements CellRangerFactory {


    @Rule
    TemporaryFolder temporaryFolder

    void "createInputDirectoryStructure, if singleCellBamFile, then recreate the input structure"() {
        given:
        new TestConfigService()

        SingleCellBamFile singleCellBamFile = createBamFile()
        Path sampleDirectory = singleCellBamFile.sampleDirectory.toPath()

        Path mate1 = sampleDirectory.resolve("${singleCellBamFile.singleCellSampleName}_S1_L001_R1_001.fastq.gz")
        Path mate2 = sampleDirectory.resolve("${singleCellBamFile.singleCellSampleName}_S1_L001_R2_001.fastq.gz")

        String file1 = 'file1'
        String file2 = 'file2'

        Path filePath1 = Paths.get(file1)
        Path filePath2 = Paths.get(file2)

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
        1 * cellRangerService.fileService.createDirectoryRecursively(sampleDirectory)

        then:
        2 * cellRangerService.lsdfFilesService.getFileViewByPidPath(_) >>> [
                file1,
                file2,
        ]
        1 * cellRangerService.fileService.createLink(mate1, filePath1, singleCellBamFile.realm)
        1 * cellRangerService.fileService.createLink(mate2, filePath2, singleCellBamFile.realm)
        0 * cellRangerService.fileService._
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

    void "createCellRangerParameters, if singleCellBamFile given without enforcedCells cells, then return map of parameters without FORCE_CELLS"() {
        given:
        final File indexFile = new File(TestCase.uniqueNonExistentPath, 'someIndex')

        SingleCellBamFile singleCellBamFile = createBamFile()

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
        CellRangerParameters.values().each {
            assert map.containsKey(it.parameterName) == it.required
        }
        map[CellRangerParameters.ID.parameterName] == singleCellBamFile.singleCellSampleName
        map[CellRangerParameters.FASTQ.parameterName] == singleCellBamFile.sampleDirectory.absolutePath
        map[CellRangerParameters.TRANSCRIPTOME.parameterName] == indexFile.absolutePath
        map[CellRangerParameters.SAMPLE.parameterName] == singleCellBamFile.singleCellSampleName
        map[CellRangerParameters.EXPECT_CELLS.parameterName] == singleCellBamFile.mergingWorkPackage.expectedCells.toString()
        map[CellRangerParameters.LOCAL_CORES.parameterName] ==~ /\d+/
        map[CellRangerParameters.LOCAL_MEM.parameterName] ==~ /\d+/
    }

    void "createCellRangerParameters, if singleCellBamFile given with enforcedCells cells, then return map of parameters including FORCE_CELLS"() {
        given:
        final File indexFile = new File(TestCase.uniqueNonExistentPath, 'someIndex')

        SingleCellBamFile singleCellBamFile = createBamFile()
        singleCellBamFile.mergingWorkPackage.enforcedCells = 5

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
        map[CellRangerParameters.FORCE_CELLS.parameterName] == singleCellBamFile.mergingWorkPackage.enforcedCells.toString()
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
                    1 * correctFilePermissions(singleCellBamFile)
                    1 * linkResultFiles(singleCellBamFile)
                },
                abstractMergedBamFileService: Mock(AbstractMergedBamFileService) {
                    1 * setSamplePairStatusToNeedProcessing(singleCellBamFile)
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
                    _ * correctFilePermissions(singleCellBamFile)
                    _ * linkResultFiles(singleCellBamFile)
                },
                abstractMergedBamFileService: Mock(AbstractMergedBamFileService) {
                    0 * setSamplePairStatusToNeedProcessing(singleCellBamFile)
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
}
