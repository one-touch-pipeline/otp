/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.validation.ValidationException
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject

@Rollback
@Integration
class SamplePairIntegrationSpec extends Specification {

    void "test setProcessingStatus with NEEDS_PROCESSING"() {
        expect:
        testSetNeedsProcessing(ProcessingStatus.NEEDS_PROCESSING)
    }

    void "test setProcessingStatus with NO_PROCESSING_NEEDED"() {
        expect:
        testSetNeedsProcessing(ProcessingStatus.NO_PROCESSING_NEEDED)
    }

    void "test setProcessingStatus with DISABLED"() {
        expect:
        testSetNeedsProcessing(ProcessingStatus.DISABLED)
    }

    private void testSetNeedsProcessing(final ProcessingStatus processingStatus) {
        MergingWorkPackage mwp1 = DomainFactory.createMergingWorkPackage()
        DomainFactory.createSampleTypePerProject(project: mwp1.project, sampleType: mwp1.sampleType, category: SampleTypePerProject.Category.DISEASE)
        final SamplePair nonPersistedSamplePair = new SamplePair(
                mergingWorkPackage1: mwp1,
                mergingWorkPackage2: DomainFactory.createMergingWorkPackage(mwp1),
                snvProcessingStatus: processingStatus,  // Tests that the instance is persisted even if it already has the correct value.
        )
        final SamplePair persistedSamplePair = DomainFactory.createSamplePair(
                mergingWorkPackage1: mwp1,
                mergingWorkPackage2: DomainFactory.createMergingWorkPackage(mwp1),
                snvProcessingStatus: processingStatus == ProcessingStatus.NEEDS_PROCESSING ? ProcessingStatus.NO_PROCESSING_NEEDED : ProcessingStatus.NEEDS_PROCESSING,
        )

        nonPersistedSamplePair.snvProcessingStatus = processingStatus
        nonPersistedSamplePair.save(flush: true)
        persistedSamplePair.snvProcessingStatus = processingStatus
        persistedSamplePair.save(flush: true)

        assert nonPersistedSamplePair.snvProcessingStatus == processingStatus
        assert nonPersistedSamplePair.id
        assert persistedSamplePair.snvProcessingStatus == processingStatus
    }

    void "test constraints with different individual should fail"() {
        given:
        SamplePair samplePair = DomainFactory.createSamplePair()
        MergingWorkPackage mergingWorkPackage1 = samplePair.mergingWorkPackage1
        mergingWorkPackage1.sample.individual = DomainFactory.createIndividual()
        assert mergingWorkPackage1.sample.save(flush: true)

        when:
        samplePair.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains("individual")
    }

    void "test constraints with different seqType should fail"() {
        given:
        SamplePair samplePair = DomainFactory.createSamplePair()
        MergingWorkPackage mergingWorkPackage1 = samplePair.mergingWorkPackage1
        mergingWorkPackage1.seqType = DomainFactory.createSeqType()
        mergingWorkPackage1.save(flush: true)

        when:
        samplePair.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains("seqType")
    }

    void "test findLatestSnvCallingInstance when no SnvCallingInstance exists should return null"() {
        given:
        SamplePair sp = DomainFactory.createSamplePair()

        expect:
        null == sp.findLatestSnvCallingInstance()
    }

    void "test findLatestSnvCallingInstance when SnvCallingInstance exists should return latest"() {
        given:
        AbstractSnvCallingInstance first = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()
        AbstractSnvCallingInstance latest = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles([samplePair: first.samplePair, instanceName: '2015-08-25_15h32'])

        expect:
        latest == latest.samplePair.findLatestSnvCallingInstance()
    }

    void "test findLatestIndelCallingInstance when no IndelCallingInstance exists should return null"() {
        given:
        SamplePair sp = DomainFactory.createSamplePair()

        expect:
        null == sp.findLatestIndelCallingInstance()
    }

    void "test findLatestIndelCallingInstance when IndelCallingInstance exists should return latest"() {
        given:
        IndelCallingInstance first = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles([instanceName: 'instance1'])
        IndelCallingInstance latest = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles([samplePair: first.samplePair, instanceName: 'instance2'])

        expect:
        latest == latest.samplePair.findLatestIndelCallingInstance()
    }

    void "test findLatestSophiaInstance when SophiaInstance exists should return null"() {
        given:
        SamplePair sp = DomainFactory.createSamplePair()

        expect:
        null == sp.findLatestSophiaInstance()
    }

    void "test findLatestSophiaCallingInstance when SophiaInstances exists should return latest"() {
        given:
        SophiaInstance first = DomainFactory.createSophiaInstanceWithRoddyBamFiles([instanceName: 'instance1'])
        SophiaInstance latest = DomainFactory.createSophiaInstanceWithRoddyBamFiles([samplePair: first.samplePair, instanceName: 'instance2'])

        expect:
        latest == latest.samplePair.findLatestSophiaInstance()
    }
}
