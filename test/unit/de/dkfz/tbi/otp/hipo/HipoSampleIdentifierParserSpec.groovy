package de.dkfz.tbi.otp.hipo

import spock.lang.*

class HipoSampleIdentifierParserSpec extends Specification {

    HipoSampleIdentifierParser parser = new HipoSampleIdentifierParser()

    @Unroll
    void 'tryParse, when H059, uses sample number exactly as given'(String sampleNumber) {
        given:
        String fullSampleName = "H059-ABCDEF-T${sampleNumber}-D1"

        when:
        HipoSampleIdentifier identifier = parser.tryParse(fullSampleName)
        boolean validPid = parser.tryParsePid(fullSampleName.substring(0, 11))

        then:
        validPid
        identifier.sampleNumber == sampleNumber
        identifier.sampleTypeDbName == "TUMOR${sampleNumber}".toString()
        identifier.fullSampleName == fullSampleName

        where:
        sampleNumber << ['0', '00', '1', '01', '2', '02', '10', '11']
    }

    @Unroll
    void 'tryParse, when not H059 and sample number has two digits, returns null'(String sampleNumber) {
        given:
        String fullSampleName = "H123-ABCDEF-T${sampleNumber}-D1"

        expect:
        parser.tryParse(fullSampleName) == null

        where:
        sampleNumber << ['00', '01', '02', '10', '11']
    }

    @Unroll
    void "test parsePid invalid input #pid"() {
        given:
        boolean validPid

        when:
        validPid = parser.tryParsePid(pid)

        then:
        !validPid

        where:
        pid                 | _
        ''                  | _
        null                | _
        and: 'Input with invalid pid'
        'INVALID_PID'          | _
    }

    @Unroll
    void "test tryParseCellPosition is not implemented and always returns null"() {
        given:
        String cellPosition

        when:
        cellPosition = parser.tryParseCellPosition(identifier)

        then:
        cellPosition == null

        where:
        identifier << [
                'H059-ABCDEF-T0-D1',
                'H059-ABCDEF-T01-D1',
                'INVALID_PID',
        ]
    }
}
