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
package de.dkfz.tbi.otp

import de.dkfz.tbi.otp.dataInstallation.DataInstallationWorkflowTests

/*
 * The idea of the test is that with the old jobExecutionPlan the test would fail since the outputParameter "Realm"
 * would be missing. Since the test do not fail it is shown that the old jobExecutionPlan was updated to the newer one
 * which provides "Realm" as outputParameter.
 *
 * @Deprecated no replacement
 */
@Deprecated
class WorkflowExecutionPlanUpdateTests extends DataInstallationWorkflowTests {

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/OldDataInstallationWorkflow.groovy",
                "scripts/workflows/DataInstallationWorkflow.groovy",
        ]
    }
}
