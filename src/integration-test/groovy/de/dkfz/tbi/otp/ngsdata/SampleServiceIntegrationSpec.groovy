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
package de.dkfz.tbi.otp.ngsdata

import org.grails.datastore.gorm.events.AutoTimestampEventListener

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.project.Project

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Rollback
@Integration
class SampleServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    SampleService sampleService
    AutoTimestampEventListener autoTimestampEventListener

    @Unroll
    void "test getCountOfSamplesForSpecifiedPeriodAndProjects for given date"() {
        given:
        Instant baseDate = LocalDate.of(2022, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC)
        Date startDate = startDateOffset  == null ? null : Date.from(baseDate.minus(startDateOffset, ChronoUnit.DAYS))
        Date endDate = endDateOffset == null ? null : Date.from(baseDate.minus(endDateOffset, ChronoUnit.DAYS))

        Sample sample

        autoTimestampEventListener.withoutDateCreated(Sample) {
            sample = createSample(dateCreated: Date.from(baseDate.minus(1, ChronoUnit.DAYS)))
        }

        when:
        int samples = sampleService.getCountOfSamplesForSpecifiedPeriodAndProjects(startDate, endDate, [sample.project])

        then:
        samples == expectedSamples

        where:
        startDateOffset | endDateOffset || expectedSamples
        2               | 0             || 1
        8               | 2             || 0
        null            | null          || 1
    }

    void "getSamplesOfProject, returns all samples of given project"() {
        given:
        Closure<Sample> createSampleForProject = { Project project ->
            return createSample(individual: createIndividual(project: project))
        }

        Project project1 = createProject()
        createSampleForProject(project1)
        createSampleForProject(project1)

        Project project2 = createProject()
        List<Sample> expectedSamples = [
                createSampleForProject(project2),
                createSampleForProject(project2),
        ]

        expect:
        TestCase.assertContainSame(expectedSamples, sampleService.getSamplesOfProject(project2))
    }
}
