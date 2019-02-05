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

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.Individual.Type

import static org.junit.Assert.assertEquals

@TestMixin(GrailsUnitTestMixin)
@TestFor(Individual)
@Mock([Project, Sample])
@Build([Individual, Realm, SampleType, SeqType])
class IndividualUnitTests {

    TestConfigService configService

    @Test
    void testSaveIndividual_AllCorrect() {
        Individual individual = createIndividual()
        assert individual.validate()
    }

    @Test
    void testSaveIndividual_NoPid() {
        Individual individual = createIndividual()
        individual.pid = null
        assert !individual.validate()
    }

    @Test
    void testSaveIndividual_NoMockPid() {
        Individual individual = createIndividual()
        individual.mockPid = null
        assert !individual.validate()
    }

    @Test
    void testSaveIndividual_NoMockFullName() {
        Individual individual = createIndividual()
        individual.mockFullName = null
        assert !individual.validate()
    }

    @Test
    void testSaveIndividual_NoInternIdentifier() {
        Individual individual = createIndividual()
        individual.internIdentifier = null
        assert individual.validate()
    }

    @Test
    void testSaveIndividual_NoType() {
        Individual individual = createIndividual()
        individual.type = null
        assert !individual.validate()
    }

    @Test
    void testSaveIndividual_NoProject() {
        Individual individual = createIndividual()
        individual.project = null
        assert !individual.validate()
    }

    @Test
    void testSaveIndividual_PidNotUnique() {
        Individual individual1 = createIndividual()
        assert individual1.validate()
        assert individual1.save(flush: true)

        Individual individual2 = createIndividual()

        shouldFail(AssertionError, { assert individual2.validate() })
    }

    @Test
    void testGetSamples() {
        Individual individual = createIndividual()
        assert individual.validate()
        assert individual.save()

        Sample sample1 = new Sample(
                individual: individual,
                sampleType: SampleType.build(name: "name1")
        )
        assert sample1.save()

        assertEquals([sample1], individual.getSamples())

        Sample sample2 = new Sample(
                individual: individual,
                sampleType: SampleType.build(name: "name2")
        )
        assert sample2.save()

        assertEquals([sample1, sample2], individual.getSamples())
    }

    @Test
    void testGetViewByPidPathBase() {
        Individual individual = Individual.build()
        SeqType seqType = DomainFactory.createSeqType()
        configService = new TestConfigService()

        String expectedPath = "${configService.getRootPath()}/${individual.project.dirName}/sequencing/${seqType.dirName}/view-by-pid"
        String actualPath = individual.getViewByPidPathBase(seqType).absoluteDataManagementPath

        assert expectedPath == actualPath
    }

    @Test
    void testGetViewByPidPath() {
        Individual individual = Individual.build()
        SeqType seqType = DomainFactory.createSeqType()
        configService = new TestConfigService()

        String expectedPath = "${configService.getRootPath()}/${individual.project.dirName}/sequencing/${seqType.dirName}/view-by-pid/${individual.pid}"
        String actualPath = individual.getViewByPidPath(seqType).absoluteDataManagementPath

        assert expectedPath == actualPath
    }


    @Test
    void testGetResultsPerPidPath() {
        Realm realm = DomainFactory.createRealm()
        assert realm.save()

        Project project = DomainFactory.createProject(
                dirName: "projectDirName",
                realm: realm
        )

        Individual individual = createIndividual()
        individual.project = project

        configService = new TestConfigService()

        String expectedPath = "${configService.getRootPath()}/${project.dirName}/results_per_pid/${individual.pid}"
        String actualPath = individual.getResultsPerPidPath().absoluteDataManagementPath

        assertEquals(expectedPath, actualPath)
    }


    private Individual createIndividual() {
        return new Individual(
                pid: "pid",
                mockPid: "mockPid",
                mockFullName: "mockFullName",
                internIdentifier: "internIdentifier",
                type: Type.REAL,
                project: DomainFactory.createProject()
        )
    }
}
