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
package de.dkfz.tbi.otp.workflowTest

import de.dkfz.tbi.otp.config.TypeValidators

enum WorkflowTestProperty {
    /**
     * Directory holding the reference data for workflow tests
     * This is used to specify the base directory for workflow test input data.
     * The value should be an absolute path, such as '/path/to/your/reference-data'.
     */
    TEST_WORKFLOW_INPUT_DIR('otp.testing.workflows.input', TypeValidators.ABSOLUTE_PATH),

    /**
     * Base Directory for running the workflow test. Each test creates inside its own sub directory.
     * This is used to specify the base directory for workflow test results.
     * The value should be an absolute path, such as '/path/to/your/test-result'.
     */
    TEST_WORKFLOW_RESULT_DIR('otp.testing.workflows.result', TypeValidators.ABSOLUTE_PATH),

    /**
     * Base directory for Roddy shared files.
     * This is used to specify the base directory for Roddy shared files.
     * The value should be an absolute path, such as '/path/to/your/legacy/share'.
     */
    TEST_WORKFLOW_RODDY_SHARED_FILES_BASE_DIRECTORY('otp.testing.workflows.roddy.sharedFiles', TypeValidators.ABSOLUTE_PATH),

    /**
     * Directory for Roddy virtual environments.
     * This is used to specify the directory where Roddy virtual environments are stored.
     * The value should be an absolute path, such as '/path/to/your/virtualenvs'.
     */
    TEST_WORKFLOW_RODDY_VIRTUAL_ENVS_DIRECTORY('otp.testing.workflows.roddy.virtualEnvs', TypeValidators.ABSOLUTE_PATH),

    /**
     * Queue for the workflow tests.
     * This is used to specify which queue should be used for the workflow tests.
     * The value should be a single word, such as 'your-queue'.
     * If you want to use a specific queue, you would specify its name here.
     */
    TEST_WORKFLOW_QUEUE('otp.testing.workflows.queue', TypeValidators.SINGLE_WORD_TEXT),

    /**
     * Suffix for the workflow test configuration.
     * This is used to specify the suffix that should be appended to the workflow test configuration.
     * The value should be a single word, such as 'your-config-suffix'.
     */
    TEST_WORKFLOW_CONFIG_SUFFIX('otp.testing.workflows.config.suffix', TypeValidators.SINGLE_WORD_TEXT),

    final String key
    final TypeValidators validator

    private WorkflowTestProperty(String key, TypeValidators validator) {
        this.key = key
        this.validator = validator
    }

    /**
     * Find the enum by its name.
     * @return the property with the given name, or null if not found.
     */
    static WorkflowTestProperty getByName(String name) {
        return values().find {
            it.name() == name
        }
    }
}
