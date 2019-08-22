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
package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import static de.dkfz.tbi.otp.ngsdata.MetaDataService.ensurePairedSequenceFileNameConsistency

@TestMixin(GrailsUnitTestMixin)
@TestFor(MetaDataService)
class MetaDataServiceUnitTests {

    @Test
    void testEnsurePairedSequenceFileNameConsistency_okay() {
        ensurePairedSequenceFileNameConsistency('abc1abc.fastq', 'abc2abc.fastq')
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_differentLengths() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc1abc.fastq', 'abc2abcd.fastq') }
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_same() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc1abc.fastq', 'abc1abc.fastq') }
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_incorrectOrder() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc2abc.fastq', 'abc1abc.fastq') }
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_illegalMateNumber1() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc0abc.fastq', 'abc2abc.fastq') }
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_illegalMateNumber2() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc1abc.fastq', 'abc3abc.fastq') }
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_tooManyDifferences() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc1abc1.fastq', 'abc2abc2.fastq') }
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_tooLongDifference() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc11abc.fastq', 'abc22abc.fastq') }
    }
}
