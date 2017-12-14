package de.dkfz.tbi.otp.hipo

import de.dkfz.tbi.otp.ngsdata.*
import spock.lang.*

class Hipo2SampleIdentifierParserSpec extends Specification {

    Hipo2SampleIdentifierParser parser = new Hipo2SampleIdentifierParser()

    @Unroll
    void 'tryParse, when identifier is #identifier, parses correctly'() {

        when:
        ParsedSampleIdentifier parsed = parser.tryParse(identifier)
        boolean validPid = parser.tryParsePid(identifier.split("-")[0,1].join("-"))

        then:
        validPid
        parsed.projectName == "hipo_${identifier.substring(0,4)}"
        parsed.pid == identifier.split("-")[0,1].join("-")
        parsed.sampleTypeDbName == sampleTypeDbName
        parsed.fullSampleName == identifier

        where:

        identifier            || sampleTypeDbName

        'K12A-123ABC-N0-D1'   || 'CONTROL0'

        'K12A-123ABC-T0-D1'   || 'TUMOR0'
        'K12A-123ABC-T1-D1'   || 'TUMOR1'
        'K12A-123ABC-T2-D1'   || 'TUMOR2'

        'K12A-123ABC-T3-R1'   || 'TUMOR3'
        'K12A-123ABC-T3-P1'   || 'TUMOR3'
        'K12A-123ABC-T3-A1'   || 'TUMOR3'
        'K12A-123ABC-T3-W1'   || 'TUMOR3'
        'K12A-123ABC-T3-Y1'   || 'TUMOR3'
        'K12A-123ABC-T3-E1'   || 'TUMOR3'
        'K12A-123ABC-T3-T1'   || 'TUMOR3'
        'K12A-123ABC-T3-M1'   || 'TUMOR3'

        'K12A-123ABC-T3-L0'   || 'TUMOR3-L0'
        'K12A-123ABC-T3-L1'   || 'TUMOR3-L1'
        'K12A-123ABC-T3-L2'   || 'TUMOR3-L2'

        'K12A-123ABC-T3-0C00' || 'TUMOR3-0C00'
        'K12A-123ABC-T3-1C00' || 'TUMOR3-1C00'
        'K12A-123ABC-T3-0C01' || 'TUMOR3-0C01'
        'K12A-123ABC-T3-1C02' || 'TUMOR3-1C02'
        'K12A-123ABC-T3-1C20' || 'TUMOR3-1C20'

        'K12A-123ABC-T3-1G20' || 'TUMOR3-1G20'
        'K12A-123ABC-T3-1H20' || 'TUMOR3-1H20'

        'K12A-123A-N0-D1'     || 'CONTROL0'
        'K12A-123A-T0-D1'     || 'TUMOR0'

        'S12A-123A-T0-D1'     || 'TUMOR0'
        'T12A-123A-T0-D1'     || 'TUMOR0'
    }

    @Unroll
    void 'tryParse, when identifier is #identifier and project is hipo_K26K, parses correctly'() {
        when:
        ParsedSampleIdentifier parsed = parser.tryParse(identifier)

        then:
        parsed.projectName == 'hipo_K26K'
        parsed.pid == 'K26K-123456'
        parsed.sampleTypeDbName == sampleTypeDbName
        parsed.fullSampleName == identifier

        where:
        identifier            || sampleTypeDbName
        'K26K-123456-M1-D1'   || 'METASTASIS1'
        'K26K-123456-M01-D1'  || 'METASTASIS01'
        'K26K-123456-M10-D1'  || 'METASTASIS10'
    }

    @Unroll
    void 'tryParse, when identifier is #identifier, returns null'() {

        expect:
        parser.tryParse(identifier) == null

        where:
        identifier << [
                'H12A-123ABC-T0-D1',
                'KX2A-123ABC-T0-D1',
                'K1XA-123ABC-T0-D1',
                'K123-123ABC-T0-D1',
                'K12-123ABC-T0-D1',
                'K1A-123ABC-T0-D1',
                'K123A-123ABC-T0-D1',
                'K12AB-123ABC-T0-D1',
                'K12A-123AB-T0-D1',
                'K12A-123ABCD-T0-D1',
                'K12A-12_ABC-T0-D1',
                'K12A-123ABC-Y0-D1',
                'K12A-123ABC-00-D1',
                'K12A-123ABC-TA-D1',
                'K12A-123ABC-T-D1',
                'K12A-123ABC-T00-D1',
                'K12A-123ABC-T01-D1',
                'K12A-123ABC-T10-D1',
                'K12A-123ABC-T0-X1',
                'K12A-123ABC-T0-01',
                'K12A-123ABC-T0-D',
                'K12A-123ABC-T0-D01',
                'K12A-123ABC-T0-DX',
                'K12A-123ABC-T0-1D12',
                'K12A-123ABC-T0-1R12',
                'K12A-123ABC-T0-1P12',
                'K12A-123ABC-T0-1A12',
                'K12A-123ABC-T0-1W12',
                'K12A-123ABC-T0-1Y12',
                'K12A-123ABC-T0-1L12',
                'K12A-123ABC-T0-C1',
                'K12A-123ABC-T0-G1',
                'K12A-123ABC-T0-H1',
                'K12A-123ABC-T0-C123',
                'K12A-123ABC-T0-12C3',
                'K12A-123ABC-T0-123C',
                'K12A-123ABC-T0-1C123',
                'K12A-123ABC-T0-12C34',
                'K12A-123ABC-T0-1C0X',
                'K12A-123ABC-T0-XC01',
        ]
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
}
