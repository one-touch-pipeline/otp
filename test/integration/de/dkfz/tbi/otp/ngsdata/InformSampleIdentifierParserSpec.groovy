package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import spock.lang.Specification
import spock.lang.Unroll


class InformSampleIdentifierParserSpec extends Specification{
    InformSampleIdentifierParser informSampleIdentifierParser = new InformSampleIdentifierParser()


    def setup() {
        Individual i123_456 = DomainFactory.createIndividual(pid: 'I123_456')
        Individual i654_321 = DomainFactory.createIndividual(pid: 'I654_321')
        Individual i124_456 = DomainFactory.createIndividual(pid: 'I124_456')

        SampleType tumor00 = DomainFactory.createSampleType(name: "TUMOR00")
        SampleType tumor01 = DomainFactory.createSampleType(name: "TUMOR01")
        SampleType control00 = DomainFactory.createSampleType(name: "CONTROL00")

        Sample i123_456_tumor00 = DomainFactory.createSample(individual: i123_456, sampleType: tumor00)
        Sample i123_456_tumor01 = DomainFactory.createSample(individual: i123_456, sampleType: tumor01)
        Sample i123_456_control00 = DomainFactory.createSample(individual: i123_456, sampleType: control00)
        Sample i654_321_tumor00 = DomainFactory.createSample(individual: i654_321, sampleType: tumor00)
        Sample i124_456_tumor00 = DomainFactory.createSample(individual: i124_456, sampleType: tumor00)

        DomainFactory.createSampleIdentifier(name: 'I123_456_0T1_D1', sample: i123_456_tumor00)
        DomainFactory.createSampleIdentifier(name: 'I123_456_0T2_D1', sample: i123_456_tumor00)
        DomainFactory.createSampleIdentifier(name: 'I123_456_1T1_D1', sample: i123_456_tumor01)
        DomainFactory.createSampleIdentifier(name: 'I123_456_0C1_D1', sample: i123_456_control00)
        DomainFactory.createSampleIdentifier(name: 'I654_321_0C1_D1', sample: i654_321_tumor00)
        DomainFactory.createSampleIdentifier(name: 'I124_456_0T1_D1', sample: i124_456_tumor00)
        DomainFactory.createSampleIdentifier(name: 'I124_456_1T1_D1', sample: i124_456_tumor00)
    }

    @Unroll('INFORM identifier #input is parsed to PID #pid, sample type name #sampleTypeDbName and Full Sample Name #fullSampleName')
    void "test parse valid input"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = informSampleIdentifierParser.tryParse(input)

        then:
        defaultParsedSampleIdentifier.projectName == 'INFORM'
        defaultParsedSampleIdentifier.pid == pid
        defaultParsedSampleIdentifier.sampleTypeDbName == sampleTypeDbName
        defaultParsedSampleIdentifier.fullSampleName == input

        where:
        input               || pid          | sampleTypeDbName
        'I123_456_0T3_D1'   || 'I123_456'   | 'TUMOR00'
        'I123_456_1T2_D1'   || 'I123_456'   | 'TUMOR01'
        'I123_456_2T1_D1'   || 'I123_456'   | 'TUMOR02'
        'I123_456_3T1_D1'   || 'I123_456'   | 'TUMOR03'
        'I123_456_XT1_D1'   || 'I123_456'   | 'TUMOR'
        'I123_456_0C2_D1'   || 'I123_456'   | 'CONTROL00'
        'I123_456_1C1_D1'   || 'I123_456'   | 'CONTROL01'
        'I123_456_3C1_D1'   || 'I123_456'   | 'CONTROL03'
        'I123_456_0M1_D1'   || 'I123_456'   | 'METASTASIS00'
        'I123_456_0F1_D1'   || 'I123_456'   | 'FFPE00'
        'I123_456_0L1_D1'   || 'I123_456'   | 'PLASMA00'
        'I123_457_0T3_D1'   || 'I123_457'   | 'TUMOR00'
        'I124_456_0C0_D1'   || 'I124_456'   | 'CONTROL00'
    }

    @Unroll
    void "test parse invalid input #input"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = informSampleIdentifierParser.tryParse(input)

        then:
        defaultParsedSampleIdentifier == null


        where:
        input               | _
        ''                  | _
        null                | _
        and: 'Input with invalid pid'
        'Z123_456_1T1_D1'   | _
        and: 'Input with invalid tissueTypeKey'
        'I123_456_1Z1_D1'   | _
        and: 'Same sample as invalid preexisting Database entry'
        'I124_456_0T1_D1'   | _
        and: 'Input with same PID and sampleTypeNumber as invalid preexisting Database entry'
        'I124_456_0T0_D1'   | _
        and: 'Input with same PID and different sampleTypeNumber as invalid preexisting Database entry'
        'I124_456_2T0_D1'   | _
    }
}
