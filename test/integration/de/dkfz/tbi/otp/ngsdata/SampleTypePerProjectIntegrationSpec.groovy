package de.dkfz.tbi.otp.ngsdata

import grails.test.spock.IntegrationSpec

class SampleTypePerProjectIntegrationSpec extends IntegrationSpec {
    Project project2
    Project project1
    SampleType sampleType1
    SampleType sampleType2
    Sample sampleProject1Type1
    Sample sampleProject1Type2
    Sample sampleProject2Type1
    Sample sampleProject2Type2

    void setupSpec() {
        DomainFactory.createAllAlignableSeqTypes()
    }

    void cleanupSpec() {
        SeqType.findAll()*.delete(flush: true)
    }

    void setup() {
        // create cross-matrix of two projects and two sample types
        // (slightly involved because project is only linked via individual)
        project1 = DomainFactory.createProject()
        project2 = DomainFactory.createProject()
        sampleType1 = DomainFactory.createSampleType()
        sampleType2 = DomainFactory.createSampleType()

        sampleProject1Type1 = DomainFactory.createSample([
                individual: DomainFactory.createIndividual([project: project1]),
                sampleType: sampleType1,
        ])
        sampleProject1Type2 = DomainFactory.createSample([
                individual: DomainFactory.createIndividual([project: project1]),
                sampleType: sampleType2,
        ])
        sampleProject2Type1 = DomainFactory.createSample([
                individual: DomainFactory.createIndividual([project: project2]),
                sampleType: sampleType1,
        ])
        sampleProject2Type2 = DomainFactory.createSample([
                individual: DomainFactory.createIndividual([project: project2]),
                sampleType: sampleType2,
        ])
    }
}
