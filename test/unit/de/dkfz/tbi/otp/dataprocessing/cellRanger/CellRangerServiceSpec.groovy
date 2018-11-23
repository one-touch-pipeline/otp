package de.dkfz.tbi.otp.dataprocessing.cellRanger

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

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
        1 * cellRangerService.fileService.createRelativeLink(mate1, filePath1)
        1 * cellRangerService.fileService.createRelativeLink(mate2, filePath2)
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
        new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().absolutePath,
        ])

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
        new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().absolutePath,
        ])

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
                processingOptionService: Mock(ProcessingOptionService) {
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
                processingOptionService: Mock(ProcessingOptionService) {
                    1 * findOptionAsString(ProcessingOption.OptionName.PIPELINE_CELLRANGER_CORE_COUNT) >> '15'
                    1 * findOptionAsString(ProcessingOption.OptionName.PIPELINE_CELLRANGER_CORE_MEM) >> '60'
                },
        ])

        when:
        Map<String, String> map = cellRangerService.createCellRangerParameters(singleCellBamFile)

        then:
        map[CellRangerParameters.FORCE_CELLS.parameterName]  == singleCellBamFile.mergingWorkPackage.enforcedCells.toString()
    }
}
