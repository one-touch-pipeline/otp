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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.workflowTest.WorkflowTestProperty

/**
 * This script configures workflow test properties that were previously configured
 * in .otp.properties files for mocked workflow tests.
 *
 * This script is automatically loaded by AbstractWorkflowSpec and WorkflowTestCase
 * during test setup if configured via otp.testing.workflows.init.script property
 * in .otp.properties file.
 *
 * Note: Adapt this file to fit your local workflow test environment.
 */
ConfigService configService = ctx.configService

/**
 * Map of workflow test properties.
 * @see WorkflowTestProperty
 */
Map<WorkflowTestProperty, String> workflowTestProperties = [
        // Required properties for workflow tests
        (WorkflowTestProperty.TEST_WORKFLOW_INPUT_DIR)                        : "/workflows/reference-data",
        (WorkflowTestProperty.TEST_WORKFLOW_RESULT_DIR)                       : "/workflows/tests",
        (WorkflowTestProperty.TEST_WORKFLOW_RODDY_SHARED_FILES_BASE_DIRECTORY): '/workflows/ngs_share',
        (WorkflowTestProperty.TEST_WORKFLOW_RODDY_VIRTUAL_ENVS_DIRECTORY)     : '/workflows/virtualenvs',
        (WorkflowTestProperty.TEST_WORKFLOW_QUEUE)                            : 'devel',
        (WorkflowTestProperty.TEST_WORKFLOW_CONFIG_SUFFIX)                    : 'devel',
]

// Execute the configuration
try {
    println("=== Starting workflow test initialization ===")
    configService.storeWorkflowTestProperties(workflowTestProperties)

    println("=== Workflow test initialization completed successfully ===")
} catch (Exception e) {
    println("ERROR: Workflow test initialization failed: ${e.message}")
    e.printStackTrace()
    throw e
}
