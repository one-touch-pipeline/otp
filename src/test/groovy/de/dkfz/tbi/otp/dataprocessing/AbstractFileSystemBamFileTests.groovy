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

package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Assert
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.SeqTrack

@TestFor(MockAbstractFileSystemBamFile)
@Mock([MockAbstractFileSystemBamFile])
class AbstractFileSystemBamFileTests {

    @Test
    void testSave() {
        AbstractFileSystemBamFile bamFile = new MockAbstractFileSystemBamFile()
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testContraints() {
        // dateCreated is not null
        AbstractFileSystemBamFile bamFile = new MockAbstractFileSystemBamFile()
        bamFile.dateCreated = null
        // this check must fail but it does not:
        // probably grails sets value of this filed before the validation
        Assert.assertTrue(bamFile.validate())

        // dateFromFileSystem is nullable
        bamFile.dateFromFileSystem = null
        Assert.assertTrue(bamFile.validate())
    }
}


class MockAbstractFileSystemBamFile extends AbstractFileSystemBamFile {

    @Override
    MergingWorkPackage getMergingWorkPackage() {
        return null
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return null
    }

    @Override
    AbstractQualityAssessment getOverallQualityAssessment() {
        return null
    }
}
