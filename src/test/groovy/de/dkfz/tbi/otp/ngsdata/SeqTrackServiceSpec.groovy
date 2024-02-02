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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.LogMessage
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

class SeqTrackServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                RawSequenceFile,
                FastqFile,
                LogMessage,
                MergingCriteria,
                Pipeline,
                RoddyWorkflowConfig,
                FastqImportInstance,
                SeqCenter,
                SeqPlatformGroup,
                SeqTrack,
        ]
    }

    SeqTrackService seqTrackService

    void setup() throws Exception {
        seqTrackService = new SeqTrackService()
        DomainFactory.createPanCanPipeline()
    }

    void "test mayAlign, when everything is okay, return true"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile(
                run: createRun(
                        seqPlatform: createSeqPlatformWithSeqPlatformGroup(
                                seqPlatformGroups: [createSeqPlatformGroup()])))
        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        expect:
        SeqTrackService.mayAlign(seqTrack)
    }

    void "test mayAlign, when data file is withdrawn, return false"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile([:], [
                fileWithdrawn: true,
        ])

        expect:
        !SeqTrackService.mayAlign(seqTrack)
    }

    void "test mayAlign, when no data file, return false"() {
        given:
        SeqTrack seqTrack = createSeqTrack()

        expect:
        !SeqTrackService.mayAlign(seqTrack)
    }

    void "test mayAlign, when wrong file type, return false"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile([:], [
                fileType: createFileType(type: FileType.Type.SOURCE),
        ])

        expect:
        !SeqTrackService.mayAlign(seqTrack)
    }

    void "test mayAlign, when exome kit information reliability is UNKNOWN_VERIFIED, return false"() {
        given:
        SeqTrack seqTrack = DomainFactory.createExomeSeqTrack(
                libraryPreparationKit: null,
                kitInfoReliability: InformationReliability.UNKNOWN_VERIFIED,
        )
        DomainFactory.createFastqFile(seqTrack: seqTrack)

        expect:
        !SeqTrackService.mayAlign(seqTrack)
    }

    void "test mayAlign, when seq platform group is null, return false"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile([
                run: createRun(seqPlatform: createSeqPlatform()),
        ])
        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        expect:
        !SeqTrackService.mayAlign(seqTrack)
    }

    void "test fillBaseCount, throws exception when one of the DataFiles does not contain nReads or SequenceLength"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        createFastqFile(seqTrack: seqTrack)
        createFastqFile([seqTrack: seqTrack] + add)

        when:
        seqTrackService.fillBaseCount(seqTrack)

        then:
        thrown(AssertionError)

        where:
        add                    | _
        [nReads: null]         | _
        [sequenceLength: null] | _
    }

    void "test fillBaseCount, sets nBasePairs to 0 when there are no DataFiles"() {
        given:
        SeqTrack seqTrack = createSeqTrack()

        when:
        seqTrackService.fillBaseCount(seqTrack)

        then:
        seqTrack.nBasePairs == 0
    }

    void "test fillBaseCount, ignores indexFiles"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        createFastqFile(seqTrack: seqTrack, nReads: 1, sequenceLength: 1)
        createFastqFile(seqTrack: seqTrack, nReads: 2, sequenceLength: 1)
        createFastqFile(seqTrack: seqTrack, nReads: 3, sequenceLength: 1, indexFile: true)

        when:
        seqTrackService.fillBaseCount(seqTrack)

        then:
        seqTrack.nBasePairs == 3
    }

    void "getSeqTrackSetsGroupedBySeqTypeAndSampleType, basic grouping by seqType and sampleType"() {
        given:
        Closure<Map<String, Object>> getProperties = { SeqType seqType, SampleType sampleType ->
            return [seqType: seqType, sample: createSample(sampleType: sampleType)]
        }
        Map<SeqType, Map<SampleType, SeqTrackSet>> result
        SampleType sampleTypeA = createSampleType()
        SampleType sampleTypeB = createSampleType()
        SampleType sampleTypeC = createSampleType()
        SeqType seqTypeA = createSeqType()
        SeqType seqTypeB = createSeqType()
        List<SeqTrack> seqTracks = [
                createSeqTrack(getProperties(seqTypeA, sampleTypeA)),
                createSeqTrack(getProperties(seqTypeA, sampleTypeB)),
                createSeqTrack(getProperties(seqTypeB, sampleTypeC)),
                createSeqTrack(getProperties(seqTypeB, sampleTypeA)),
                createSeqTrack(getProperties(seqTypeA, sampleTypeB)),
                createSeqTrack(getProperties(seqTypeA, sampleTypeC)),
                createSeqTrack(getProperties(seqTypeB, sampleTypeA)),
                createSeqTrack(getProperties(seqTypeB, sampleTypeB)),
                createSeqTrack(getProperties(seqTypeA, sampleTypeC)),
        ]

        when:
        result = seqTrackService.groupSeqTracksBySeqTypeAndSampleType(seqTracks)

        then:
        seqTracks.each {
            result[it.seqType][it.sampleType].seqTracks.contains(it)
        }
    }

    @Unroll
    void "findAllBySampleAndSeqTypeAndAntibodyTarget, when no SeqTrack exist and #name, then return empty list"() {
        given:
        SeqType seqType = createSeqType([
                hasAntibodyTarget: hasAntibodyTarget,
        ])
        Sample sample = createSample()
        createSeqTrack([seqType: seqType])
        createSeqTrack([sample: sample])
        AntibodyTarget antibodyTarget = hasAntibodyTarget ? createAntibodyTarget() : null

        SeqTrackService service = new SeqTrackService()

        when:
        List<SeqTrack> seqTracks = service.findAllBySampleAndSeqTypeAndAntibodyTarget(sample, seqType, antibodyTarget)

        then:
        seqTracks.empty

        where:
        hasAntibodyTarget | _
        true              | _
        false             | _

        name = "hasAntibodyTarget: ${hasAntibodyTarget}"
    }

    @Unroll
    void "findAllBySampleAndSeqTypeAndAntibodyTarget, when mergingWorkPackage exist and #name, then return the correct one"() {
        given:
        SeqType seqType = createSeqType([
                hasAntibodyTarget: hasAntibodyTarget
        ])
        SeqTrack seqTrack = createSeqTrack([seqType: seqType])
        createSeqTrack([seqType: seqType])
        createSeqTrack([sample: seqTrack.sample])

        SeqTrackService service = new SeqTrackService()

        when:
        List<SeqTrack> seqTracks = service.findAllBySampleAndSeqTypeAndAntibodyTarget(seqTrack.sample, seqTrack.seqType, seqTrack.antibodyTarget)

        then:
        seqTracks == [seqTrack]

        where:
        hasAntibodyTarget | _
        true              | _
        false             | _

        name = "hasAntibodyTarget: ${hasAntibodyTarget}"
    }
}
