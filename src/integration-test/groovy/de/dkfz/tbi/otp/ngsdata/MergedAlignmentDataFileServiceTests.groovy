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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.junit.Test

import static org.junit.Assert.assertEquals

@Rollback
@Integration
class MergedAlignmentDataFileServiceTests {

    MergedAlignmentDataFileService mergedAlignmentDataFileService

    @Test
    void testBuildRelativePath() {
        TestData testData = new TestData()
        testData.createObjects()
        SeqType seqType = testData.seqType
        Sample sample = testData.sample

        String expectedPath = "${testData.project.dirName}/sequencing/whole_genome_sequencing/view-by-pid/654321/tumor/paired/merged-alignment/"
        String actualPath = mergedAlignmentDataFileService.buildRelativePath(seqType, sample)

        assertEquals(expectedPath, actualPath)
    }
}
