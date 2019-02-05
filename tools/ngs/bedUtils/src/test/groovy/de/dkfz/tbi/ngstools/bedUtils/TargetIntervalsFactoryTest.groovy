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

package de.dkfz.tbi.ngstools.bedUtils

import org.junit.*

class TargetIntervalsFactoryTest extends GroovyTestCase {

    File file

    @Test
    void testCreate() {
        file = new File("/tmp/kitname.bed")
        if (file.exists()) file.delete()
        file << "chr1\t0\t100\nchr2\t32\t105\nchr3\t10000000\t249250621"
        file.deleteOnExit()
        List<String> referenceGenomeEntryNames = ["chr1", "chr2", "chr3", "chr4", "chr5"]
        TargetIntervals targetIntervals = TargetIntervalsFactory.create(file.absolutePath, referenceGenomeEntryNames)
        assertNotNull targetIntervals
    }

    @After
    void tearDown() {
        file.delete()
    }
}
