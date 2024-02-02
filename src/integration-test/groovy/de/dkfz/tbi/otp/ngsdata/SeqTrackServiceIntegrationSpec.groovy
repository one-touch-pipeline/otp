/*
 * Copyright 2011-2024 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class SeqTrackServiceIntegrationSpec extends Specification implements DomainFactoryCore, UserAndRoles {

    SeqTrackService seqTrackService

    void setupData() {
        createUserAndRoles()
    }

    void "getSeqTrackSet, should return all fitting seqTracks, when fitting seqTracks exist"() {
        given:
        setupData()
        createSeqTrack()
        Individual individual = createIndividual()
        SampleType sampleType = createSampleType()
        Sample sample = createSample([individual: individual, sampleType: sampleType])
        SeqType seqType = createSeqType()

        SeqTrack seqTrack1 = createSeqTrack([sample: sample, seqType: seqType])
        SeqTrack seqTrack2 = createSeqTrack([sample: sample, seqType: seqType])

        when:
        List<SeqTrack> result = doWithAuth(ADMIN) {
            seqTrackService.getSeqTrackSet(individual, sampleType, seqType)
        }

        then:
        TestCase.assertContainSame(result, [seqTrack1, seqTrack2])
    }

    void "getSeqTrackSet, should return an empty list, when no fitting seqTracks exist"() {
        given:
        setupData()
        createSeqTrack()
        createSeqTrack()

        Individual individual = createIndividual()
        SampleType sampleType = createSampleType()
        SeqType seqType = createSeqType()

        expect:
        doWithAuth(ADMIN) {
            seqTrackService.getSeqTrackSet(individual, sampleType, seqType)
        } == []
    }

    void "getSeqTracksByMultiInput, should return all fitting seqTracks, when fitting seqTrack exist"() {
        given:
        setupData()
        SequencingReadType readType = SequencingReadType.PAIRED
        String readTypeName = readType.name()

        Boolean singleCell = true

        Individual individual = createIndividual()
        String pid = individual.pid

        SampleType sampleType = createSampleType()
        String sampleTypeName = sampleType.name
        Sample sample = createSample([individual: individual, sampleType: sampleType])

        SeqType seqType1 = createSeqType([libraryLayout: readType, singleCell: singleCell])
        String seqTypeName = seqType1.name
        SeqType seqType2 = createSeqType([displayName: seqTypeName, libraryLayout: readType, singleCell: singleCell])
        SeqType seqType3 = createSeqType([importAlias: [seqTypeName, 'alias2'], libraryLayout: readType, singleCell: singleCell])

        SeqType seqType4 = createSeqType([libraryLayout: SequencingReadType.SINGLE, singleCell: singleCell])

        SeqTrack seqTrack1 = createSeqTrack([sample: sample, seqType: seqType1])
        SeqTrack seqTrack2 = createSeqTrack([sample: sample, seqType: seqType2])
        SeqTrack seqTrack3 = createSeqTrack([sample: sample, seqType: seqType3])
        createSeqTrack([sample: sample, seqType: seqType4])

        when:
        List<SeqTrack> result = doWithAuth(ADMIN) {
            seqTrackService.getSeqTracksByMultiInput(pid, sampleTypeName, seqTypeName, readTypeName, singleCell)
        }

        then:
        TestCase.assertContainSame(result, [seqTrack1, seqTrack2, seqTrack3])
    }

    void "getSeqTracksByMultiInput, should return an empty list, when no fitting seqTracks exist"() {
        given:
        setupData()
        SequencingReadType readType = SequencingReadType.PAIRED
        String readTypeName = readType.name()

        Boolean singleCell = true

        Individual individual1 = createIndividual()
        String pid = individual1.pid
        Individual individual2 = createIndividual()

        SampleType sampleType1 = createSampleType()
        String sampleTypeName = sampleType1.name
        SampleType sampleType2 = createSampleType()

        Sample sample1 = createSample([individual: individual1, sampleType: sampleType1])
        Sample sample2 = createSample([individual: individual2, sampleType: sampleType1])
        Sample sample3 = createSample([individual: individual1, sampleType: sampleType2])

        SeqType seqType1 = createSeqType([libraryLayout: readType, singleCell: singleCell])
        String seqTypeName = seqType1.name

        SeqType seqType2 = createSeqType([libraryLayout: SequencingReadType.SINGLE, singleCell: singleCell])

        createSeqTrack([sample: sample1, seqType: seqType2])
        createSeqTrack([sample: sample2, seqType: seqType1])
        createSeqTrack([sample: sample3, seqType: seqType1])

        expect:
        doWithAuth(ADMIN) {
            seqTrackService.getSeqTracksByMultiInput(pid, sampleTypeName, seqTypeName, readTypeName, singleCell)
        } == []
    }
}
