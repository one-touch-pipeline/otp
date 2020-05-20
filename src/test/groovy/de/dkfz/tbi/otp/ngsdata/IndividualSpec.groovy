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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.Individual.Type
import de.dkfz.tbi.otp.project.Project

class IndividualSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Individual,
                Project,
                Realm,
                Sample,
                SampleType,
                SeqType,
        ]
    }


    TestConfigService configService

    void "test validate"() {
        given:
        Individual individual = createIndividual()

        expect:
        individual.validate()
    }

    void "test validate, when pid is null"() {
        given:
        Individual individual = createIndividual()
        individual.pid = null

        expect:
        !individual.validate()
    }

    void "test validate, when mockPid is null"() {
        given:
        Individual individual = createIndividual()
        individual.mockPid = null

        expect:
        !individual.validate()
    }

    void "test validate, when mockFullName is null"() {
        given:
        Individual individual = createIndividual()
        individual.mockFullName = null

        expect:
        !individual.validate()
    }

    void "test validate, when internIdentifier is null"() {
        given:
        Individual individual = createIndividual()
        individual.internIdentifier = null

        expect:
        individual.validate()
    }

    void "test validate, when type is null"() {
        given:
        Individual individual = createIndividual()
        individual.type = null

        expect:
        !individual.validate()
    }

    void "test validate, when project is null"() {
        given:
        Individual individual = createIndividual()
        individual.project = null

        expect:
        !individual.validate()
    }

    void "test validate, when pid is not unique"() {
        given:
        Individual individual1 = createIndividual()
        assert individual1.validate()
        assert individual1.save(flush: true)
        Individual individual2 = createIndividual()

        expect:
        !individual2.validate()
    }

    void "test getSamples, with one sample"() {
        given:
        Individual individual = createIndividual()
        assert individual.validate()
        assert individual.save(flush: true)

        Sample sample1 = new Sample(
                individual: individual,
                sampleType: DomainFactory.createSampleType(name: "name1")
        )
        assert sample1.save(flush: true)

        expect:
        [sample1] == individual.getSamples()
    }


    void "test getSamples, with multiple samples"() {
        given:
        Individual individual = createIndividual()
        assert individual.validate()
        assert individual.save(flush: true)

        Sample sample1 = new Sample(
                individual: individual,
                sampleType: DomainFactory.createSampleType(name: "name1")
        )
        assert sample1.save(flush: true)

        Sample sample2 = new Sample(
                individual: individual,
                sampleType: DomainFactory.createSampleType(name: "name2")
        )
        assert sample2.save(flush: true)

        expect:
        [sample1, sample2] == individual.getSamples()
    }

    void "test getViewByPidPathBase"() {
        given:
        Individual individual = DomainFactory.createIndividual()
        SeqType seqType = DomainFactory.createSeqType()
        configService = new TestConfigService()
        String expectedPath = "${configService.getRootPath()}/${individual.project.dirName}/sequencing/${seqType.dirName}/view-by-pid"

        when:
        String actualPath = individual.getViewByPidPathBase(seqType).absoluteDataManagementPath

        then:
        expectedPath == actualPath
    }

    void "test getViewByPidPath"() {
        given:
        Individual individual = DomainFactory.createIndividual()
        SeqType seqType = DomainFactory.createSeqType()
        configService = new TestConfigService()
        String expectedPath = "${configService.getRootPath()}/${individual.project.dirName}/sequencing/${seqType.dirName}/view-by-pid/${individual.pid}"

        when:
        String actualPath = individual.getViewByPidPath(seqType).absoluteDataManagementPath

        then:
        expectedPath == actualPath
    }


    void "test getResultsPerPidPath"() {
        given:
        Realm realm = DomainFactory.createRealm()
        assert realm.save(flush: true)

        Project project = DomainFactory.createProject(
                dirName: "projectDirName",
                realm: realm
        )

        Individual individual = createIndividual()
        individual.project = project

        configService = new TestConfigService()

        String expectedPath = "${configService.getRootPath()}/${project.dirName}/results_per_pid/${individual.pid}"

        when:
        String actualPath = individual.getResultsPerPidPath().absoluteDataManagementPath

        then:
        expectedPath == actualPath
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
