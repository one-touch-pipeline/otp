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

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.LogMessage
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

class SeqTrackServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        [
                DataFile,
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
        SeqTrack seqTrack = createSeqTrackWithOneDataFile(
                run: createRun(
                        seqPlatform: createSeqPlatformWithSeqPlatformGroup(
                                seqPlatformGroups: [createSeqPlatformGroup()])))
        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        expect:
        SeqTrackService.mayAlign(seqTrack)
    }

    void "test mayAlign, when data file is withdrawn, return false"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneDataFile([:], [
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
        SeqTrack seqTrack = createSeqTrackWithOneDataFile([:], [
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
        DomainFactory.createDataFile(seqTrack: seqTrack)

        expect:
        !SeqTrackService.mayAlign(seqTrack)
    }

    void "test mayAlign, when seq platform group is null, return false"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneDataFile([
                run: createRun(seqPlatform: createSeqPlatform()),
        ])
        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        expect:
        !SeqTrackService.mayAlign(seqTrack)
    }

    void "test fillBaseCount, throws exception when one of the DataFiles does not contain nReads or SequenceLength"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        createDataFile(seqTrack: seqTrack)
        createDataFile([seqTrack: seqTrack] + add)

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
        createDataFile(seqTrack: seqTrack, nReads: 1, sequenceLength: 1)
        createDataFile(seqTrack: seqTrack, nReads: 2, sequenceLength: 1)
        createDataFile(seqTrack: seqTrack, nReads: 3, sequenceLength: 1, indexFile: true)

        when:
        seqTrackService.fillBaseCount(seqTrack)

        then:
        seqTrack.nBasePairs == 3
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, seqTrack is null, should fail"() {
        when:
        seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(null, true)

        then:
        thrown(AssertionError)
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, can be linked"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()

        when:
        seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == true
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, will be aligned false, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()

        when:
        seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, false)

        then:
        seqTrack.linkedExternally == false
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, seq center doesn't allow linking, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
        seqTrack.run.seqCenter.importDirsAllowLinking = []

        when:
        seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == false
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, other path, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked("/other_path")

        when:
        seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == false
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, project doesn't allow linking, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
        seqTrack.sample.individual.project.forceCopyFiles = true

        when:
        seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == false
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, WGBS data, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
        SeqType wgbsSeqType = DomainFactory.createWholeGenomeBisulfiteSeqType()
        seqTrack.seqType = wgbsSeqType

        when:
        seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == false
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, seqTrack has indexFile, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
        DomainFactory.createDataFile([
                seqTrack: seqTrack,
                indexFile: true,
        ])

        when:
        seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == false
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, RNA data, has to be copied"() {
        given:
        DomainFactory.createRnaPipeline()

        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()

        SeqType rnaSeqType = DomainFactory.createRnaPairedSeqType()
        seqTrack.seqType = rnaSeqType

        when:
        seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == false
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, with adapter trimming, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
        DomainFactory.createRoddyWorkflowConfig([seqType: seqTrack.seqType, individual: seqTrack.individual, adapterTrimmingNeeded: true])

        when:
        seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == false
    }

    private SeqTrack createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked(String path = null) {
        Run run = DomainFactory.createRun(seqCenter: DomainFactory.createSeqCenter(importDirsAllowLinking: ["/link_this", "/link_that"]))
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                run: run,
                sample: DomainFactory.createSample(
                        individual: DomainFactory.createIndividual(
                                project: DomainFactory.createProject(forceCopyFiles: false)
                        )
                )
        )
        DomainFactory.createDataFile(seqTrack: seqTrack, run: run, fastqImportInstance: fastqImportInstance, initialDirectory: path ?: "/link_this")
        DomainFactory.createDataFile(seqTrack: seqTrack, run: run, fastqImportInstance: fastqImportInstance, initialDirectory: path ?: "/link_this")
        return seqTrack
    }

    void "test doesImportDirAllowLinking, seqTrack is null, should fail"() {
        when:
        seqTrackService.doesImportDirAllowLinking(null)

        then:
        thrown(AssertionError)
    }

    void "test doesImportDirAllowLinking, data files on linkable"() {
        given:
        SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage()

        expect:
        true == seqTrackService.doesImportDirAllowLinking(seqTrack)
    }

    void "test doesImportDirAllowLinking, data files in other linkable dir"() {
        given:
        SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage("/link_this")

        expect:
        true == seqTrackService.doesImportDirAllowLinking(seqTrack)
    }

    void "test doesImportDirAllowLinking, data files in other non linkable dir"() {
        given:
        SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage("/link_this_too")

        expect:
        false == seqTrackService.doesImportDirAllowLinking(seqTrack)
    }

    void "test doesImportDirAllowLinking, data files not in linkable dir"() {
        given:
        SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage("/other_path")

        expect:
        false == seqTrackService.doesImportDirAllowLinking(seqTrack)
    }

    void "test doesImportDirAllowLinking, seq center doesn't have linkable dir"() {
        given:
        SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage()

        seqTrack.run.seqCenter.importDirsAllowLinking = []
        seqTrack.run.seqCenter.save(flush: true)

        expect:
        false == seqTrackService.doesImportDirAllowLinking(seqTrack)
    }

    private SeqTrack createDataForAreFilesLocatedOnMidTermStorage(String path = null) {
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance()
        SeqCenter seqCenter = DomainFactory.createSeqCenter(importDirsAllowLinking: ["/link_me", "/link_this"])
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        seqTrack.run.seqCenter = seqCenter
        seqTrack.run.save(flush: true)
        DomainFactory.createDataFile(seqTrack: seqTrack, fastqImportInstance: fastqImportInstance, initialDirectory: path ?: "/link_me")
        DomainFactory.createDataFile(seqTrack: seqTrack, fastqImportInstance: fastqImportInstance, initialDirectory: path ?: "/link_me")
        return seqTrack
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
        result = seqTrackService.getSeqTrackSetsGroupedBySeqTypeAndSampleType(seqTracks)

        then:
        seqTracks.each {
            result[it.seqType][it.sampleType].seqTracks.contains(it)
        }
    }
}
