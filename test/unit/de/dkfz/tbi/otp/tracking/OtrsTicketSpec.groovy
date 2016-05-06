package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.ngsdata.*
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
        given:
        OtrsTicket otrsTicket

        when:
        DomainFactory.createOtrsTicket([ticketNumber: '20000101a2345678'])

        then:
        ValidationException ex = thrown()
        ex.message.contains("does not match the required pattern")
    }

    def 'test creation, duplicate String' () {
        given:
        OtrsTicket otrsTicket

        when:
        DomainFactory.createOtrsTicket([ticketNumber: '2000010112345678'])
        DomainFactory.createOtrsTicket([ticketNumber: '2000010112345678'])

        then:
        ValidationException ex = thrown()
        ex.message.contains("OtrsTicket.ticketNumber.unique.error")
    }

    def 'test creation, incorrect date' () {
        given:
        OtrsTicket otrsTicket

        when:
        DomainFactory.createOtrsTicket([ticketNumber: '2000910112345678'])

        then:
        ValidationException ex = thrown()
        ex.message.contains("Cannot parse \"20009101\": Value 91 for monthOfYear must be in the range [1,12]")
    }

    def 'test getImportDate' () {
        given:
        OtrsTicket otrsTicket01 = DomainFactory.createOtrsTicket()
        OtrsTicket otrsTicket02 = DomainFactory.createOtrsTicket()

        RunSegment runSegment01 = DomainFactory.createRunSegment(otrsTicket: otrsTicket01)
        RunSegment runSegment02 = DomainFactory.createRunSegment(otrsTicket: otrsTicket02)

        MetaDataFile metadataFile01 = DomainFactory.createMetaDatafile([runSegment: runSegment02])
        MetaDataFile metadataFile02 = DomainFactory.createMetaDatafile([runSegment: runSegment01])
        MetaDataFile metadataFile03 = DomainFactory.createMetaDatafile([runSegment: runSegment01])


        expect:
        otrsTicket02.getImportDate().is(metadataFile01.dateCreated)
        otrsTicket01.getImportDate().is(metadataFile02.dateCreated)
        !otrsTicket01.getImportDate().is(metadataFile03.dateCreated)
    }
}
