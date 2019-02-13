package de.dkfz.tbi.otp.ngsdata

import grails.test.spock.IntegrationSpec
import spock.lang.Unroll


class SampleServiceIntegrationSpec extends IntegrationSpec {
    SampleService sampleService

    @Unroll
    void "test getCountOfSamplesForSpecifiedPeriodAndProjects for given date"() {
        given:
        Date baseDate = new Date(0, 0, 10)
        Date startDate = startDateOffset  == null ? null : baseDate.minus(startDateOffset)
        Date endDate = endDateOffset == null ? null : baseDate.minus(endDateOffset)

        DataFile dataFile = DomainFactory.createDataFile()
        dataFile.dateCreated = baseDate.minus(1)

        when:
        int samples = sampleService.getCountOfSamplesForSpecifiedPeriodAndProjects(startDate, endDate, [dataFile.project])

        then:
        samples == expectedSamples

        where:
        startDateOffset | endDateOffset || expectedSamples
        2               | 0             || 1
        8               | 2             || 0
        null            | null          || 1
    }
}
