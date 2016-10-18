package de.dkfz.tbi.otp.hipo

import spock.lang.*

class HipoSampleIdentifierSpec extends Specification {

    HipoSampleIdentifierParser parser = new HipoSampleIdentifierParser()

    @Unroll
    void 'tryParse, when H059, uses sample number exactly as given'(String sampleNumber) {
        given:
        String fullSampleName = "H059-ABCDEF-T${sampleNumber}-D1"

        when:
        HipoSampleIdentifier identifier = parser.tryParse(fullSampleName)

        then:
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
}
