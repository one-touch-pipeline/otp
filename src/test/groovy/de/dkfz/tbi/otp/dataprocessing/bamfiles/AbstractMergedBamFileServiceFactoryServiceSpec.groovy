/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.bamfiles

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile

class AbstractMergedBamFileServiceFactoryServiceSpec extends Specification implements ServiceUnitTest<AbstractMergedBamFileServiceFactoryService>, DataTest {

    static ExternallyProcessedMergedBamFileService externallyProcessedMergedBamFileService = new ExternallyProcessedMergedBamFileService()
    static RnaRoddyBamFileService rnaRoddyBamFileService = new RnaRoddyBamFileService()
    static RoddyBamFileService roddyBamFileService = new RoddyBamFileService()
    static SingleCellBamFileService singleCellBamFileService = new SingleCellBamFileService()

    void "test getService"() {
        given:
        service.externallyProcessedMergedBamFileService = externallyProcessedMergedBamFileService
        service.rnaRoddyBamFileService = rnaRoddyBamFileService
        service.roddyBamFileService = roddyBamFileService
        service.singleCellBamFileService = singleCellBamFileService

        expect:
        service.getService(bamFile) == result

        where:
        bamFile                                || result
        new ExternallyProcessedMergedBamFile() || externallyProcessedMergedBamFileService
        new RnaRoddyBamFile()                  || rnaRoddyBamFileService
        new RoddyBamFile()                     || roddyBamFileService
        new SingleCellBamFile()                || singleCellBamFileService
    }
}
