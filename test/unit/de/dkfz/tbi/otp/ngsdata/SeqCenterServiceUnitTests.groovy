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

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.*

@TestFor(SeqCenterService)
@Build([
        SeqCenter
])
class SeqCenterServiceUnitTests {

    SeqCenterService seqCenterService

    final static String SEQ_CENTER ="SeqCenter"

    final static String DIFFERENT_SEQ_CENTER ="DifferentSeqCenter"

    final static String SEQ_CENTER_DIR = "SeqCenterDir"

    final static String DIFFERENT_SEQ_CENTER_DIR = "DifferentSeqCenterDir"

    @Before
    void setUp() throws Exception {
        seqCenterService = new SeqCenterService()
    }


    @After
    void tearDown() throws Exception {
        seqCenterService = null
    }

    @Test
    void testCreateSeqCenterUsingSeqCenterAndSeqCenterDir() {
        assertEquals(
                seqCenterService.createSeqCenter(SEQ_CENTER, SEQ_CENTER_DIR),
                SeqCenter.findByNameAndDirName(SEQ_CENTER ,SEQ_CENTER_DIR)
        )
    }

    @Test
    void testCreateSeqCenterUsingSeqCenterTwiceAndSeqCenterDir() {
        seqCenterService.createSeqCenter(SEQ_CENTER, SEQ_CENTER_DIR)
        shouldFail(AssertionError) {
            seqCenterService.createSeqCenter(SEQ_CENTER, DIFFERENT_SEQ_CENTER_DIR)
        }
    }

    @Test
    void testCreateSeqCenterUsingSeqCenterAndSeqCenterDirTwice() {
        seqCenterService.createSeqCenter(SEQ_CENTER, SEQ_CENTER_DIR)
        shouldFail(AssertionError) {
            seqCenterService.createSeqCenter(DIFFERENT_SEQ_CENTER, SEQ_CENTER_DIR)
        }
    }

    @Test
    void testCreateSeqCenterUsingNullAndSeqCenterDir() {
        shouldFail(AssertionError) {
            seqCenterService.createSeqCenter(null, SEQ_CENTER_DIR)
        }
    }

    @Test
    void testCreateSeqCenterUsingSeqCenterAndNull() {
        shouldFail(AssertionError) {
            seqCenterService.createSeqCenter(SEQ_CENTER, null)
        }
    }
}

