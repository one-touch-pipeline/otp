package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import grails.validation.*
import spock.lang.*

@Mock([
    MetaDataFile,
    OtrsTicket,
    Run,
    RunSegment,
    SeqCenter,
    SeqPlatform,
    SeqPlatformGroup,
])
class OtrsTicketSpec extends Specification {

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
