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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*

class MergingSetSpec extends Specification implements DataTest {

    MergingWorkPackage workPackage = null

    @Override
    Class[] getDomainClassesToMock() {
        [
                DataFile,
                FileType,
                MergingCriteria,
                MergingSet,
                MergingSetAssignment,
                ProcessedBamFile,
        ]
    }

    void "save, if all are fine, should save"() {
        given:
        MergingSet mergingSet = new MergingSet([
                status            : MergingSet.State.DECLARED,
                mergingWorkPackage: DomainFactory.createMergingWorkPackage([
                        pipeline: DomainFactory.createDefaultOtpPipeline(),
                ]),
        ])
        when:
        mergingSet.validate()

        then:
        !mergingSet.errors.hasErrors()

        expect:
        mergingSet.save(flush: true)
    }

    @Unroll
    void "validate, if #property is #value, then validation should fail therefor"() {
        given:
        MergingSet mergingSet = new MergingSet([
                mergingWorkPackage: DomainFactory.createMergingWorkPackage([
                        pipeline: DomainFactory.createDefaultOtpPipeline(),
                ]),
        ] + [
                (property): value,
        ])

        expect:
        TestCase.assertValidateError(mergingSet, property, constraint, value)

        where:
        property             | constraint | value
        'status'             | 'nullable' | null
        'mergingWorkPackage' | 'nullable' | null
    }

    void testIsLatestSet() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                pipeline: DomainFactory.createDefaultOtpPipeline(),
        ])

        MergingSet mergingSet = new MergingSet([
                identifier        : 1,
                status            : MergingSet.State.DECLARED,
                mergingWorkPackage: mergingWorkPackage,
        ])

        MergingSet mergingSet2 = new MergingSet([
                identifier        : 2,
                status            : MergingSet.State.DECLARED,
                mergingWorkPackage: mergingWorkPackage,
        ])

        when:
        mergingSet.save(flush: true)

        then:
        mergingSet.isLatestSet()

        when:
        mergingSet2.save(flush: true)

        then:
        mergingSet2.isLatestSet()
        !mergingSet.isLatestSet()
    }

    void "getContainedSeqTracks, when seqtrack is only added once in a hierarrchy, then return expected seqTracks"() {
        given:
        SeqTrack seqTrack1 = DomainFactory.createSeqTrack()
        SeqTrack seqTrack2 = DomainFactory.createSeqTrack()
        SeqTrack seqTrack3 = DomainFactory.createSeqTrack()

        Set<SeqTrack> seqTracks1 = [seqTrack1, seqTrack2].toSet()
        Set<SeqTrack> seqTracks2 = [seqTrack3].toSet()
        Set<SeqTrack> allSeqTracks = seqTracks1 + seqTracks2
        assert allSeqTracks.size() == 3

        MergingSet mergingSet = DomainFactory.createMergingSet()

        expect:
        mergingSet.containedSeqTracks == Collections.emptySet()

        when:
        ProcessedBamFile bamFile1 = DomainFactory.assignNewProcessedBamFile(mergingSet)
        bamFile1.metaClass.getContainedSeqTracks = { seqTracks1 }

        then:
        mergingSet.containedSeqTracks == seqTracks1

        when:
        ProcessedBamFile bamFile2 = DomainFactory.assignNewProcessedBamFile(mergingSet)
        bamFile2.metaClass.getContainedSeqTracks = { seqTracks2 }

        then:
        mergingSet.containedSeqTracks == allSeqTracks
    }

    void "getContainedSeqTracks, when a seqTrack is contained twiced, then fail"() {
        given:
        Set<SeqTrack> seqTracks = [DomainFactory.createSeqTrack()].toSet()
        MergingSet mergingSet = DomainFactory.createMergingSet()
        ProcessedBamFile bamFile1 = DomainFactory.assignNewProcessedBamFile(mergingSet)
        bamFile1.metaClass.getContainedSeqTracks = { seqTracks }
        ProcessedBamFile bamFile2 = DomainFactory.assignNewProcessedBamFile(mergingSet)
        bamFile2.metaClass.getContainedSeqTracks = { seqTracks }

        when:
        mergingSet.containedSeqTracks()

        then:
        IllegalStateException e = thrown()
        e.message.contains('more than once')
    }
}
