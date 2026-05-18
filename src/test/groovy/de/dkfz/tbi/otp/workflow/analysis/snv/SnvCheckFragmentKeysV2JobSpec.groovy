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
package de.dkfz.tbi.otp.workflow.analysis.snv

import spock.lang.Specification

import de.dkfz.tbi.TestCase

class SnvCheckFragmentKeysV2JobSpec extends Specification {

    private SnvCheckFragmentKeysV2Job job

    void "getCvalues, should return expected keys"() {
        given:
        job = new SnvCheckFragmentKeysV2Job()
        Collection<String> expectedKeys = [
                "tbiLsfVirtualEnvDir",
                "VEP_BINARY",
                "VEP_VERSION",
                "VEP_FORKS",
                "VEP_FA_INDEX",
                "VEP_CACHE_BASE",
                "VEP_PLUGIN_CADD_SNV",
                "VEP_PLUGIN_SPLICEAI_SNV",
                "VEP_PLUGIN_SPLICEAI_INDEL",
                "VEP_SPECIES",
                "VEP_ASSEMBLY",
                "VEP_OUT_FORMAT",
        ]

        expect:
        TestCase.assertContainSame(job.cvalues, expectedKeys)
    }
}
