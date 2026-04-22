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

import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerFileNames
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion

import java.nio.file.Path
import java.nio.file.Paths

class CellRangerExecuteJobSpec extends Specification {

    CellRangerService cellRangerService
    CellRangerWorkFileService cellRangerWorkFileService
    ProcessingOptionService processingOptionService

    static final String MODULE_LOADER = "source /etc/modules"
    static final String MODULE_ENABLE = "module load"
    static final String PROGRAM_VERSION = "10.0.0"
    static final String LOCAL_CORES = "16"
    static final String LOCAL_MEM = "64"
    static final String COMBINED_CONFIG = '{"OTP_CLUSTER": {"CORES": "16", "MEMORY": "64", "WALLTIME": "PT100H"}}'
    static final String SAMPLE_NAME = "pid1_tumor"
    static final Path WORK_DIR = Paths.get("/work/dir")
    static final Path RESULT_DIR = Paths.get("/result/dir")

    void setup() {
        cellRangerService = Mock(CellRangerService)
        cellRangerWorkFileService = Mock(CellRangerWorkFileService)
        processingOptionService = Mock(ProcessingOptionService)
    }

    CellRangerExecuteJob overrideJob(SingleCellBamFile bamFile) {
        return new CellRangerExecuteJob(cellRangerService, cellRangerWorkFileService, processingOptionService) {
            @Override
            SingleCellBamFile getBamFile(WorkflowStep workflowStep) {
                return bamFile
            }
        }
    }

    WorkflowStep mockWorkflowStep() {
        WorkflowVersion workflowVersion = Mock(WorkflowVersion) {
            getWorkflowVersion() >> PROGRAM_VERSION
        }
        WorkflowRun workflowRun = Mock(WorkflowRun) {
            getWorkflowVersion() >> workflowVersion
            getCombinedConfig() >> COMBINED_CONFIG
        }
        return Mock(WorkflowStep) {
            getWorkflowRun() >> workflowRun
        }
    }

    SingleCellBamFile mockBamFile() {
        CellRangerMergingWorkPackage workPackage = Mock(CellRangerMergingWorkPackage)
        return Mock(SingleCellBamFile) {
            getSingleCellSampleName() >> SAMPLE_NAME
            getMergingWorkPackage() >> workPackage
        }
    }

    void "createScripts returns a single-element list containing the cluster script"() {
        given:
        WorkflowStep workflowStep = mockWorkflowStep()
        SingleCellBamFile bamFile = mockBamFile()
        CellRangerExecuteJob job = overrideJob(bamFile)

        Map<String, String> params = [
                "--id"           : "42",
                "--fastqs"       : "/fastqs/dir",
                "--transcriptome": "/ref/genome",
                "--sample"       : SAMPLE_NAME,
        ]

        when:
        List<String> scripts = job.createScripts(workflowStep)

        then:
        1 * processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER) >> MODULE_LOADER
        1 * processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ENABLE_MODULE) >> MODULE_ENABLE
        1 * cellRangerService.createCellRangerParameters(bamFile, PROGRAM_VERSION, LOCAL_CORES, LOCAL_MEM) >> params
        1 * cellRangerWorkFileService.getDirectoryPath(bamFile) >> WORK_DIR
        1 * cellRangerWorkFileService.getResultDirectory(bamFile) >> RESULT_DIR
        1 * cellRangerService.deleteOutputDirectoryStructureIfExists(bamFile)

        scripts.size() == 1
        String script = scripts[0]
        script.contains(MODULE_LOADER)
        script.contains("${MODULE_ENABLE} ${PROGRAM_VERSION}")
        script.contains("cd \"${WORK_DIR}\"")
        script.contains("cellranger count")
        script.contains("--disable-ui")
        script.contains("--id=42")
        script.contains("--fastqs=/fastqs/dir")
        script.contains('echo "cellranger count')
        script.contains("${RESULT_DIR}/${SAMPLE_NAME}_${CellRangerFileNames.CELL_RANGER_COMMAND_FILE_NAME}")
        script.contains("md5sum")
        script.contains("${RESULT_DIR}/${CellRangerFileNames.ORIGINAL_BAM_FILE_NAME}")
        script.contains("${RESULT_DIR}/${CellRangerFileNames.ORIGINAL_BAM_MD5SUM_FILE_NAME}")
    }

    void "createScripts #description optional cell count parameter"() {
        given:
        WorkflowStep workflowStep = mockWorkflowStep()
        SingleCellBamFile bamFile = mockBamFile()
        CellRangerExecuteJob job = overrideJob(bamFile)

        when:
        List<String> scripts = job.createScripts(workflowStep)

        then:
        1 * processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER) >> MODULE_LOADER
        1 * processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ENABLE_MODULE) >> MODULE_ENABLE
        1 * cellRangerService.createCellRangerParameters(bamFile, PROGRAM_VERSION, LOCAL_CORES, LOCAL_MEM) >> params
        1 * cellRangerWorkFileService.getDirectoryPath(bamFile) >> WORK_DIR
        1 * cellRangerWorkFileService.getResultDirectory(bamFile) >> RESULT_DIR
        1 * cellRangerService.deleteOutputDirectoryStructureIfExists(bamFile)

        assert (flags.every { scripts[0].contains(it) }) == shouldContain

        where:
        description                                      | params                                              | flags                                         | shouldContain
        "includes --expect-cells for"                    | ["--expect-cells": "5000"]                          | ["--expect-cells=5000"]                       | true
        "excludes --expect-cells for absent"             | [:]                                                 | ["--expect-cells"]                            | false
        "includes --force-cells for"                     | ["--force-cells": "3000"]                           | ["--force-cells=3000"]                        | true
        "excludes --force-cells for absent"              | [:]                                                 | ["--force-cells"]                             | false
        "includes both --expect-cells and --force-cells" | ["--expect-cells": "5000", "--force-cells": "3000"] | ["--expect-cells=5000", "--force-cells=3000"] | true
    }
}
