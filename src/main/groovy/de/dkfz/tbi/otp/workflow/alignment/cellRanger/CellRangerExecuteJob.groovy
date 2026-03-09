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
package de.dkfz.tbi.otp.workflow.alignment.cellRanger

import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerFileNames
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteClusterPipelineJob
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class CellRangerExecuteJob extends AbstractExecuteClusterPipelineJob implements CellRangerShared {

    private static final String CELLRANGER_COUNT_COMMAND = 'cellranger count'
    private static final String DISABLE_UI_FLAG = '--disable-ui'

    private final CellRangerService cellRangerService
    private final CellRangerWorkFileService cellRangerWorkFileService
    private final ProcessingOptionService processingOptionService

    CellRangerExecuteJob(CellRangerService cellRangerService, CellRangerWorkFileService cellRangerWorkFileService,
                         ProcessingOptionService processingOptionService) {
        this.cellRangerService = cellRangerService
        this.cellRangerWorkFileService = cellRangerWorkFileService
        this.processingOptionService = processingOptionService
    }

    @Override
    protected List<String> createScripts(WorkflowStep workflowStep) {
        SingleCellBamFile bamFile = getBamFile(workflowStep)

        cellRangerService.deleteOutputDirectoryStructureIfExists(bamFile)

        String moduleLoader = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)
        String moduleEnable = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ENABLE_MODULE)

        String programVersion = workflowStep.workflowRun.workflowVersion.workflowVersion
        Map otpCluster = JSON.parse(workflowStep.workflowRun.combinedConfig)[ExternalWorkflowConfigFragment.Type.OTP_CLUSTER.name()] as Map
        String localCores = otpCluster?.get('CORES')
        String localMem = otpCluster?.get('MEMORY')

        Map<String, String> parameters = cellRangerService.createCellRangerParameters(bamFile, programVersion, localCores, localMem)

        List<String> paramList = parameters.collect { key, value -> "${key}=${value}" } as List<String>
        String command = ([CELLRANGER_COUNT_COMMAND] + paramList + [DISABLE_UI_FLAG]).join(' ')

        Path workDir = cellRangerWorkFileService.getDirectoryPath(bamFile)
        Path resultDir = cellRangerWorkFileService.getResultDirectory(bamFile)
        Path commandFile = resultDir.resolve("${bamFile.singleCellSampleName}_${CellRangerFileNames.CELL_RANGER_COMMAND_FILE_NAME}")
        Path bamFilePath = resultDir.resolve(CellRangerFileNames.ORIGINAL_BAM_FILE_NAME)
        Path md5sumFile = resultDir.resolve(CellRangerFileNames.ORIGINAL_BAM_MD5SUM_FILE_NAME)

        String script = """\
            |${moduleLoader}
            |${moduleEnable} ${programVersion}
            |
            |cd "${workDir}"
            |${command}
            |
            |echo "${command}" > "${commandFile}"
            |
            |md5sum "${bamFilePath}" | sed -e 's#  ${bamFilePath}##' > "${md5sumFile}"
            |""".stripMargin()

        return [script]
    }
}
