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

import grails.testing.gorm.DataTest
import spock.lang.Specification

class SeqCenterServiceSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {[
            SeqCenter,
    ]}


    SeqCenterService seqCenterService

    final static String SEQ_CENTER = "SeqCenter"

    final static String DIFFERENT_SEQ_CENTER = "DifferentSeqCenter"

    final static String SEQ_CENTER_DIR = "SeqCenterDir"

    final static String DIFFERENT_SEQ_CENTER_DIR = "DifferentSeqCenterDir"


    void setup() throws Exception {
        seqCenterService = new SeqCenterService()
    }


    void "test createSeqCenter"() {
        expect:
        seqCenterService.createSeqCenter(SEQ_CENTER, SEQ_CENTER_DIR) ==
                SeqCenter.findByNameAndDirName(SEQ_CENTER, SEQ_CENTER_DIR)
    }

    void "test createSeqCenter, with existing name, should fail"() {
        given:
        seqCenterService.createSeqCenter(SEQ_CENTER, SEQ_CENTER_DIR)

        when:
        seqCenterService.createSeqCenter(SEQ_CENTER, DIFFERENT_SEQ_CENTER_DIR)

        then:
        thrown(AssertionError)
    }

    void "test createSeqCenter, with existing dir, should fail"() {
        given:
        seqCenterService.createSeqCenter(SEQ_CENTER, SEQ_CENTER_DIR)

        when:
        seqCenterService.createSeqCenter(DIFFERENT_SEQ_CENTER, SEQ_CENTER_DIR)

        then:
        thrown(AssertionError)
    }

    void "test createSeqCenter, when seq center is null, should fail"() {
        when:
        seqCenterService.createSeqCenter(null, SEQ_CENTER_DIR)

        then:
        thrown(AssertionError)
    }

    void "test createSeqCenter, when seq center dir is null, should fail"() {
        when:
        seqCenterService.createSeqCenter(SEQ_CENTER, null)

        then:
        thrown(AssertionError)
    }
}

