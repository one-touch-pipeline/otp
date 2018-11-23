package de.dkfz.tbi.otp.job.jobs.cellRanger


import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
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
class ExecuteCellRangerJobSpec extends Specification implements CellRangerFactory {


    void "maybeSubmit, when all fine, then send expected script to cluster"() {
        given:
        new TestConfigService()

        final String loadModul = 'load modul system'
        final String enableModul = 'load modul'
        final String parameterKey = 'param'
        final String parameterValue = 'value'

        SingleCellBamFile singleCellBamFile = createBamFile()
        Path workDirectory = singleCellBamFile.workDirectory.toPath()
        Path resultDirectory = singleCellBamFile.resultDirectory.toPath()

        ExecuteCellRangerJob job = new ExecuteCellRangerJob([
                cellRangerService         : Mock(CellRangerService),
                clusterJobSchedulerService: Mock(ClusterJobSchedulerService),
                fileService               : Mock(FileService) {
                    0 * _
                },
                processingOptionService   : Mock(ProcessingOptionService) {
                    1 * findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER) >> loadModul
                    1 * findOptionAsString(ProcessingOption.OptionName.COMMAND_ENABLE_MODULE) >> enableModul
                },
                fileSystemService         : Mock(FileSystemService) {
                    1 * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
        ])
        job.metaClass.getProcessParameterObject = { ->
            return singleCellBamFile
        }

        String expectedSimplefiedScript = [
                loadModul,
                "${enableModul} ${singleCellBamFile.mergingWorkPackage.config.programVersion}",
                "cd ${singleCellBamFile.workDirectory}",
                "cellranger count ${parameterKey}=${parameterValue}",
                "md5sum ${resultDirectory.resolve(SingleCellBamFile.ORIGINAL_BAM_FILE_NAME)} > ${resultDirectory.resolve(SingleCellBamFile.ORIGINAL_BAM_MD5SUM_FILE_NAME)}",
        ].join('\n')

        when:
        job.maybeSubmit()

        then:
        0 * job.fileService._
        1 * job.fileService.createDirectoryRecursively(workDirectory)

        then:
        1 * job.fileService.setPermission(workDirectory, FileService.OWNER_DIRECTORY_PERMISSION)

        then:
        1 * job.cellRangerService.deleteOutputDirectoryStructureIfExists(singleCellBamFile)

        then:
        1 * job.cellRangerService.createInputDirectoryStructure(singleCellBamFile)

        then:
        1 * job.cellRangerService.createCellRangerParameters(singleCellBamFile) >> [(parameterKey): parameterValue]
        1 * job.clusterJobSchedulerService.executeJob(singleCellBamFile.realm, _) >> { Realm realm, String script ->
            String simplifiedScript = script.split('\n')*.trim().findAll().join('\n')
            assert expectedSimplefiedScript == simplifiedScript
        }
    }

    void "validate, call validateFilesExistInResultDirectory"() {
        given:
        SingleCellBamFile singleCellBamFile = createBamFile()

        ExecuteCellRangerJob job = new ExecuteCellRangerJob([
                cellRangerService: Mock(CellRangerService),
        ])
        job.metaClass.getProcessParameterObject = { ->
            return singleCellBamFile
        }

        when:
        job.validate()

        then:
        1 * job.cellRangerService.validateFilesExistsInResultDirectory(singleCellBamFile)
    }
}
