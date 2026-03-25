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
package de.dkfz.tbi.otp.workflow.jobs

import spock.lang.Specification

import de.dkfz.tbi.TestCase

class AbstractRoddyCheckFragmentKeysJobSpec extends Specification {

    private AbstractRoddyCheckFragmentKeysJob createJob(Collection<String> cvalues) {
        return new AbstractRoddyCheckFragmentKeysJob() {
            @Override
            Collection<String> getCvalues() { return cvalues }
        }
    }

    void "getKeyPaths, should return APPTAINER_KEYS and cvalues all prefixed with RODDY/cvalues/"() {
        given:
        Collection<String> cvalues = ["outputDirectory", "analysisMethodNameOnOutput"]
        AbstractRoddyCheckFragmentKeysJob job = createJob(cvalues)

        Set<String> expectedKeys = (AbstractRoddyCheckFragmentKeysJob.APPTAINER_KEYS + cvalues).collect {
            "RODDY/cvalues/${it}"
        } as Set

        expect:
        TestCase.assertContainSame(job.keyPaths, expectedKeys)
    }

    void "getKeyPaths, should return only APPTAINER_KEYS prefixed with RODDY/cvalues/ when cvalues is empty"() {
        given:
        AbstractRoddyCheckFragmentKeysJob job = createJob([])

        Set<String> expectedKeys = AbstractRoddyCheckFragmentKeysJob.APPTAINER_KEYS.collect {
            "RODDY/cvalues/${it}"
        } as Set

        expect:
        TestCase.assertContainSame(job.keyPaths, expectedKeys)
    }

    void "getKeyPaths, should deduplicate when cvalues contains a key that is also in APPTAINER_KEYS"() {
        given:
        String duplicateKey = AbstractRoddyCheckFragmentKeysJob.APPTAINER_KEYS.first()
        AbstractRoddyCheckFragmentKeysJob job = createJob([duplicateKey, "otherKey"])

        when:
        Set<String> result = job.keyPaths

        then:
        result.size() == AbstractRoddyCheckFragmentKeysJob.APPTAINER_KEYS.size() + 1
        result.contains("RODDY/cvalues/${duplicateKey}".toString())
        result.contains("RODDY/cvalues/otherKey")
    }

    void "APPTAINER_KEYS, should contain the expected keys"() {
        expect:
        TestCase.assertContainSame(AbstractRoddyCheckFragmentKeysJob.APPTAINER_KEYS, [
                "jobExecutionEnvironment",
                "apptainerArguments",
                "containerEnginePath",
                "containerImage",
                "containerMounts",
        ])
    }
}
