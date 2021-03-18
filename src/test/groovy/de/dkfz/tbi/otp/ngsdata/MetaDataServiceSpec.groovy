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

import spock.lang.Specification

class MetaDataServiceSpec extends Specification {

    void "test ensurePairedSequenceFileNameConsistency with correct file names"() {
        when:
        MetaDataService.ensurePairedSequenceFileNameConsistency('abc1abc.fastq', 'abc2abc.fastq')

        then:
        notThrown(IllegalFileNameException)
    }

    void "test ensurePairedSequenceFileNameConsistency with incorrect file names"() {
        when:
        MetaDataService.ensurePairedSequenceFileNameConsistency(mate1FileName, mate2FileName)

        then:
        thrown(IllegalFileNameException)

        where:
        mate1FileName    | mate2FileName    || _
        'abc1abc.fastq'  | 'abc2abcd.fastq' || _
        'abc1abc.fastq'  | 'abc1abc.fastq'  || _
        'abc2abc.fastq'  | 'abc1abc.fastq'  || _
        'abc0abc.fastq'  | 'abc2abc.fastq'  || _
        'abc1abc.fastq'  | 'abc3abc.fastq'  || _
        'abc1abc1.fastq' | 'abc2abc2.fastq' || _
        'abc11abc.fastq' | 'abc22abc.fastq' || _
    }

    void "test ensurePairedSequenceFileNameOrder with correct file names"() {
        when:
        MetaDataService.ensurePairedSequenceFileNameOrder(fileNames.collect { new File(it) })

        then:
        notThrown(IllegalFileNameException)

        where:
        fileNames                                                                                                                        || _
        []                                                                                                                               || _
        ['/a/whatever_L001_R1.fastq.gz', '/a/whatever_L001_R2.fastq.gz']                                                                 || _
        ['/a/whatever_L001_R1.fastq.gz', '/a/whatever_L001_R2.fastq.gz', '/a/whatever_L002_R1.fastq.gz', '/a/whatever_L002_R2.fastq.gz'] || _
        ['/a/whatever_L001_R1.fastq.gz', '/a/whatever_L001_R2.fastq.gz', '/b/whatever_L002_R1.fastq.gz', '/b/whatever_L002_R2.fastq.gz'] || _
    }

    void "test ensurePairedSequenceFileNameOrder with incorrect file names"() {
        when:
        MetaDataService.ensurePairedSequenceFileNameOrder(fileNames.collect { new File(it) })

        then:
        thrown(IllegalFileNameException)

        where:
        fileNames                                                                                                                        || _
        ['/a/whatever_R1_L001.fastq.gz', '/a/whatever_R2_L001.fastq.gz', '/a/whatever_R1_L002.fastq.gz', '/a/whatever_R2_L002.fastq.gz'] || _
        ['/a/whatever_L001_R1.fastq.gz', '/b/whatever_L001_R2.fastq.gz', '/a/whatever_L002_R1.fastq.gz', '/b/whatever_L002_R2.fastq.gz'] || _
    }
}
