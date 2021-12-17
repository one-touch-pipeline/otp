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
package de.dkfz.tbi.otp.job.jobs.cellRanger

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.ClusterJobSchedulerService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import java.nio.file.FileSystems
import java.nio.file.Path

class ExecuteCellRangerJobSpec extends Specification implements CellRangerFactory, DataTest {

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

        String expectedSimplifiedScript = [
                loadModul,
                "${enableModul} ${singleCellBamFile.mergingWorkPackage.config.programVersion}",
                "cd ${singleCellBamFile.workDirectory}",
                "cellranger count ${parameterKey}=${parameterValue} --disable-ui",
                "echo \"cellranger count ${parameterKey}=${parameterValue} --disable-ui\" > ${resultDirectory.resolve("${singleCellBamFile.singleCellSampleName}_${SingleCellBamFile.CELL_RANGER_COMMAND_FILE_NAME}")}",
                "md5sum ${resultDirectory.resolve(SingleCellBamFile.ORIGINAL_BAM_FILE_NAME)} | " +
                        "sed -e 's#  ${resultDirectory.resolve(SingleCellBamFile.ORIGINAL_BAM_FILE_NAME)}##' > " +
                        "${resultDirectory.resolve(SingleCellBamFile.ORIGINAL_BAM_MD5SUM_FILE_NAME)}",
        ].join('\n')

        when:
        job.maybeSubmit()

        then:
        0 * job.fileService._
        1 * job.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(workDirectory, _, _)

        then:
        1 * job.fileService.setPermissionViaBash(workDirectory, _, FileService.OWNER_DIRECTORY_PERMISSION_STRING)

        then:
        1 * job.cellRangerService.deleteOutputDirectoryStructureIfExists(singleCellBamFile)

        then:
        1 * job.cellRangerService.createInputDirectoryStructure(singleCellBamFile)

        then:
        1 * job.cellRangerService.createCellRangerParameters(singleCellBamFile) >> [(parameterKey): parameterValue]
        1 * job.clusterJobSchedulerService.executeJob(singleCellBamFile.realm, _) >> { Realm realm, String script ->
            String simplifiedScript = script.split('\n')*.trim().findAll().join('\n')
            assert expectedSimplifiedScript == simplifiedScript
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
