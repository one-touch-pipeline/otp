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

package de.dkfz.tbi.otp.tracking


import grails.testing.gorm.DataTest
import grails.validation.ValidationException
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ThreadUtils

class OtrsTicketSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {[
            MetaDataFile,
            OtrsTicket,
            Run,
            RunSegment,
            SeqCenter,
            SeqPlatform,
            SeqPlatformGroup,
    ]}

    def 'test creation, correct String' () {
        given:
        OtrsTicket otrsTicket

        when:
        otrsTicket = DomainFactory.createOtrsTicket([ticketNumber: '2000010112345678'])

        then:
        otrsTicket.ticketNumber == '2000010112345678'
    }

    def 'test creation, incorrect String' () {
        when:
        DomainFactory.createOtrsTicket([ticketNumber: '20000101a2345678'])

        then:
        ValidationException ex = thrown()
        ex.message.contains("does not match the required pattern")
    }

    def 'test creation, duplicate String' () {
        when:
        DomainFactory.createOtrsTicket([ticketNumber: '2000010112345678'])
        DomainFactory.createOtrsTicket([ticketNumber: '2000010112345678'])

        then:
        ValidationException ex = thrown()
        ex.message.contains("OtrsTicket.ticketNumber.unique.error")
    }

    def 'test creation, incorrect date' () {
        when:
        DomainFactory.createOtrsTicket([ticketNumber: '2000910112345678'])

        then:
        ValidationException ex = thrown()
        ex.message.contains("Cannot parse \"20009101\": Value 91 for monthOfYear must be in the range [1,12]")
    }

    def 'getFirstImportTimestamp and getLastImportTimestamp return expected result'() {
        given:
        OtrsTicket otrsTicketA = DomainFactory.createOtrsTicket()
        OtrsTicket otrsTicketB = DomainFactory.createOtrsTicket()

        RunSegment runSegmentA = DomainFactory.createRunSegment(otrsTicket: otrsTicketA)
        RunSegment runSegmentB = DomainFactory.createRunSegment(otrsTicket: otrsTicketB)

        MetaDataFile metadataFileB1 = DomainFactory.createMetaDataFile([runSegment: runSegmentB])
        assert ThreadUtils.waitFor({ System.currentTimeMillis() > metadataFileB1.dateCreated.time }, 1, 1)
        MetaDataFile metadataFileA1 = DomainFactory.createMetaDataFile([runSegment: runSegmentA])
        assert ThreadUtils.waitFor({ System.currentTimeMillis() > metadataFileA1.dateCreated.time }, 1, 1)
        MetaDataFile metadataFileA2 = DomainFactory.createMetaDataFile([runSegment: runSegmentA])

        assert metadataFileA1.dateCreated > metadataFileB1.dateCreated
        assert metadataFileA2.dateCreated > metadataFileA1.dateCreated

        expect:
        otrsTicketA.firstImportTimestamp == metadataFileA1.dateCreated
        otrsTicketA.lastImportTimestamp == metadataFileA2.dateCreated
        otrsTicketB.firstImportTimestamp == metadataFileB1.dateCreated
        otrsTicketB.lastImportTimestamp == metadataFileB1.dateCreated
    }
}
