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

package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.validation.Errors
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class CellRangerConfigurationServiceIntegrationSpec extends Specification implements CellRangerFactory, UserAndRoles {

    CellRangerConfigurationService cellRangerConfigurationService
    Project project
    SeqType seqType
    Individual individual
    SampleType sampleType
    Sample sample
    Sample sample2
    SeqTrack seqTrack

    void setupData() {
        createUserAndRoles()

        project = createProject()
        seqType = createSeqType()
        Pipeline pipeline = findOrCreatePipeline()
        createConfig(project: project, seqType: seqType, pipeline: pipeline)

        DomainFactory.createMergingCriteria(project: project, seqType: seqType)

        individual = DomainFactory.createIndividual(project: project)
        sampleType = DomainFactory.createSampleType()
        sample = createSample(individual: individual, sampleType: sampleType)
        seqTrack = DomainFactory.createSeqTrack(seqType: seqType, sample: sample)
        sample.refresh()

        Individual individual2 = DomainFactory.createIndividual(project: project)
        SampleType sampleType2 = DomainFactory.createSampleType()
        sample2 = createSample(individual: individual2, sampleType: sampleType2)
        DomainFactory.createSeqTrack(seqType: seqType, sample: sample2)
        sample2.refresh()
    }

    void "test getSamples"() {
        given:
        setupData()

        when:
        CellRangerConfigurationService.Samples samples = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.getSamples(project, individual, sampleType)
        }

        then:
        samples.allSamples == [sample, sample2]
        samples.selectedSamples == [sample]
    }

    void "test getSamples for whole project"() {
        given:
        setupData()

        when:
        CellRangerConfigurationService.Samples samples = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.getSamples(project, null, null)
        }

        then:
        samples.allSamples == [sample, sample2]
        samples.selectedSamples == [sample, sample2]
    }

    @Unroll
    void "test createMergingWorkPackage (expectedCells=#expectedCells, enforcedCells=#enforcedCells)"() {
        given:
        setupData()

        when:
        Errors errors = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.createMergingWorkPackage(expectedCells, enforcedCells, project, individual, sampleType)
        }

        then:
        !errors
        CellRangerMergingWorkPackage mwp = CollectionUtils.exactlyOneElement(CellRangerMergingWorkPackage.all)
        mwp.sample == sample
        mwp.seqTracks == [seqTrack] as Set
        mwp.expectedCells == expectedCells
        mwp.enforcedCells == enforcedCells
        mwp.project == project
        mwp.seqType == seqType
        mwp.individual == individual

        where:
        expectedCells | enforcedCells
        5000          | null
        null          | 5000
    }

    void "test createMergingWorkPackage for whole project"() {
        given:
        setupData()

        when:
        Errors errors = SpringSecurityUtils.doWithAuth(ADMIN) {
            cellRangerConfigurationService.createMergingWorkPackage(1, null, project, null, null)
        }

        then:
        !errors
        CellRangerMergingWorkPackage.all.size() == 2
        CellRangerMergingWorkPackage.all*.sample as Set == [sample, sample2] as Set
    }
}
