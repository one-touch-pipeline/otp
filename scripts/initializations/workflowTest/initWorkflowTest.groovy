/*
 * Copyright 2011-2025 The OTP authors
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

import groovy.transform.Field

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflow
import de.dkfz.tbi.otp.workflow.analysis.indel.IndelWorkflow
import de.dkfz.tbi.otp.workflow.analysis.snv.SnvWorkflow
import de.dkfz.tbi.otp.workflow.analysis.sophia.SophiaWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.commands.CreateCommand
import de.dkfz.tbi.otp.workflowTest.WorkflowTestProperty

/**
 * This script configures workflow test properties that were previously configured
 * in .otp.properties files.
 *
 * This script is automatically loaded by AbstractWorkflowSpec and WorkflowTestCase
 * during test setup if configured via otp.testing.workflows.init.script property
 * in .otp.properties file.
 *
 * Note: Adapt this file to fit your local workflow test environment.
 */

@Field
TestConfigService configService = ctx.configService

@Field
ConfigSelectorService configSelectorService = ctx.configSelectorService

/**
 * Map of workflow test properties.
 * Please adapt this map to fit your local workflow test environment.
 * @see WorkflowTestProperty
 */
Map<WorkflowTestProperty, String> workflowTestProperties = [
        // Required properties for workflow tests
        (WorkflowTestProperty.TEST_WORKFLOW_INPUT_DIR)                        : "/path/to/your-reference-data",
        (WorkflowTestProperty.TEST_WORKFLOW_RESULT_DIR)                       : "/path/to/your-test-result",
        (WorkflowTestProperty.TEST_WORKFLOW_RODDY_SHARED_FILES_BASE_DIRECTORY): '/path/to/your-legacy-share',
        (WorkflowTestProperty.TEST_WORKFLOW_RODDY_VIRTUAL_ENVS_DIRECTORY)     : '/path/to/your-virtualenvs',
        (WorkflowTestProperty.TEST_WORKFLOW_QUEUE)                            : 'your-queue',
        (WorkflowTestProperty.TEST_WORKFLOW_CONFIG_SUFFIX)                    : 'your-config-suffix',
]

/**
 * configure apptainer for roddy
 */
void configureApptainer() {
    println "configure Apptainer"
    List<Workflow> roddyWorkflows = """
        #alignment
        ${PanCancerWorkflow.WORKFLOW}
        ${WgbsWorkflow.WORKFLOW}
        ${RnaAlignmentWorkflow.WORKFLOW}

        # analysis
        ${SnvWorkflow.WORKFLOW}
        ${IndelWorkflow.WORKFLOW}
        ${SophiaWorkflow.WORKFLOW}
        ${AceseqWorkflow.WORKFLOW}

    """.split('\n')*.trim().findAll {
        it && !it.startsWith('#')
    }.collect {
        println "- ${it}"
        CollectionUtils.exactlyOneElement(Workflow.findAllByName(it), "Could not find '${it}")
    }

    println configSelectorService.create(new CreateCommand([
            selectorName: 'Roddy apptainer',
            type        : SelectorType.GENERIC,
            workflows   : roddyWorkflows,
            value       : """
                            {
                                "RODDY": {
                                    "cvalues": {
                                        "jobExecutionEnvironment": {
                                            "type": "string",
                                            "value": "apptainer"
                                        },
                                        "apptainerArguments": {
                                            "type": "string",
                                            "value": "--contain"
                                        },
                                        "containerEnginePath": {
                                            "type": "path",
                                            "value": "/usr/bin/apptainer"
                                        },
                                        "containerImage": {
                                            "type": "path",
                                            "value": "$YOUR_APPTAINER_IMAGE"
                                        },
                                        "containerMounts": {
                                            "type": "bashArray",
                                            "value": "( $YOUR_MOUNT_LIST )"
                                        }
                                    }
                                }
                            }
                            """
    ]))
}

/**
 * Configure workflow-specific settings with example configurations.
 * This method demonstrates how to configure various workflows
 * using the ConfigSelectorService
 * @see ConfigSelectorService#create().
 */
void configureWorkflowSpecificSettings() {
    // Example Weskit configuration for nf-seq-qc workflow
    // Uncomment and adapt as needed:
    /*
    String workflowName = "nf-seq-qc"
    Set<Workflow> workflowsByName = Workflow.findAllByName(workflowName) as Set<Workflow>
    if (!workflowsByName) {
        throw new IllegalArgumentException("No Workflow exists with name '${workflowName}'")
    }
    configSelectorService.create(new CreateCommand([
            selectorName: 'WeskitSelector',
            type: Selector.GENERIC,
            workflows: workflowsByName,
            value: groovy.json.JsonOutput.JsonOutput.toJson(["WESKIT", [
                    "MAX_MEMORY": "512M",
                    "MAX_RUNTIME": "24:00",
                    "PROFILES": "slurm,singularity",
            ]])
    ]))
    */
}

// Execute the configuration
try {
    println("=== Starting workflow test initialization ===")

    configService.storeWorkflowTestProperties(workflowTestProperties)
    configureApptainer()
    configureWorkflowSpecificSettings()

    println("=== Workflow test initialization completed successfully ===")
} catch (Exception e) {
    println("ERROR: Workflow test initialization failed: ${e.message}")
    e.printStackTrace()
    throw e
}
