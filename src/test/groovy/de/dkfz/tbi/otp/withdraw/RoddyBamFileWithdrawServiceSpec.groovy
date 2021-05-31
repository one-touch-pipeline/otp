/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.withdraw

import grails.testing.services.ServiceUnitTest

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory

class RoddyBamFileWithdrawServiceSpec extends WithdrawBamFileServiceSpec<RoddyBamFileWithdrawService> implements ServiceUnitTest<RoddyBamFileWithdrawService>, RoddyPancanFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return super.domainClassesToMock + [
                MergingWorkPackage,
                RoddyBamFile,
        ]
    }

    void "collectObjects, when called, then return all bamFiles containing the seqTracks"() {
        given:
        RoddyBamFile bamFile1 = createBamFile()
        RoddyBamFile bamFile2 = createBamFile([baseBamFile: bamFile1])
        RoddyBamFile bamFile3 = createBamFile([baseBamFile: bamFile2])
        createBamFile()
        createBamFile()

        when:
        List<RoddyBamFile> result = service.collectObjects([bamFile1.seqTracks.first()])

        then:
        TestCase.assertContainSame(result, [bamFile1, bamFile2, bamFile3])
    }
}
