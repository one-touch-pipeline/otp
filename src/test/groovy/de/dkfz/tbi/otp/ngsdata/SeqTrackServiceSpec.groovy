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


class SeqTrackServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ExomeSeqTrack,
                DataFile,
                LogMessage,
                MergingCriteria,
                Pipeline,
                RoddyWorkflowConfig,
                RunSegment,
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
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile(
                run: DomainFactory.createRun(
                        seqPlatform: DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                                seqPlatformGroups: [DomainFactory.createSeqPlatformGroup()])))
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        expect:
        SeqTrackService.mayAlign(seqTrack)
    }

    void "test mayAlign, when data file is withdrawn, return false"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile([:], [
                fileWithdrawn: true,
        ])

        expect:
        !SeqTrackService.mayAlign(seqTrack)
    }

    void "test mayAlign, when no data file, return false"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        expect:
        !SeqTrackService.mayAlign(seqTrack)
    }

    void "test mayAlign, when wrong file type, return false"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile([:], [
                fileType: DomainFactory.createFileType(type: FileType.Type.SOURCE),
        ])

        expect:
        !SeqTrackService.mayAlign(seqTrack)
    }

    void "test mayAlign, when run segment must not align, return false"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        RunSegment runSegment = DomainFactory.createRunSegment(
                align: false,
        )
        DomainFactory.createDataFile(
                seqTrack: seqTrack,
                runSegment: runSegment,
        )

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
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile([
                run: DomainFactory.createRun(seqPlatform: DomainFactory.createSeqPlatform()),
        ])
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        expect:
        !SeqTrackService.mayAlign(seqTrack)
    }

    void "test fillBaseCount, when sequence length does not exist, should fail"() {
        given:
        String sequenceLength = null
        Long nReads = 12345689
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)

        when:
        seqTrackService.fillBaseCount(seqTrack)

        then:
        thrown(AssertionError)
    }

    void "test fillBaseCount, when nReads does not exist, should fail"() {
        given:
        String sequenceLength = "101"
        Long nReads = null
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)

        when:
        seqTrackService.fillBaseCount(seqTrack)

        then:
        thrown(AssertionError)
    }


    void "test fillBaseCount, when sequence length is single value and library layout single"() {
        given:
        String sequenceLength = "101"
        Long nReads = 12345689
        Long expectedBasePairs = sequenceLength.toInteger() * nReads
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)

        when:
        seqTrackService.fillBaseCount(seqTrack)

        then:
        seqTrack.nBasePairs == expectedBasePairs
    }


    void "test fillBaseCount, when sequence length is single value and library layout paired"() {
        given:
        String sequenceLength = "101"
        Long nReads = 12345689
        Long expectedBasePairs = sequenceLength.toInteger() * nReads * 2
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)
        DomainFactory.createSequenceDataFile([nReads: nReads, sequenceLength: sequenceLength, seqTrack: seqTrack])
        seqTrack.seqType.libraryLayout = LibraryLayout.PAIRED

        when:
        seqTrackService.fillBaseCount(seqTrack)

        then:
        seqTrack.nBasePairs == expectedBasePairs
    }

    void "test fillBaseCount, when sequence length is integer range"() {
        given:
        String sequenceLength = "90-100"
        int meanSequenceLength = sequenceLength.split('-').sum { it.toInteger() } / 2
        Long nReads = 12345689
        Long expectedBasePairs = meanSequenceLength * nReads
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)

        when:
        seqTrackService.fillBaseCount(seqTrack)

        then:
        seqTrack.nBasePairs == expectedBasePairs
    }

    private SeqTrack createTestSeqTrack(String sequenceLength, Long nReads) {
        return DomainFactory.createSeqTrackWithOneDataFile([:], [nReads: nReads, sequenceLength: sequenceLength])
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
        RunSegment runSegment = DomainFactory.createRunSegment()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                run: run,
                sample: DomainFactory.createSample(
                        individual: DomainFactory.createIndividual(
                                project: DomainFactory.createProject(forceCopyFiles: false)
                        )
                )
        )
        DomainFactory.createDataFile(seqTrack: seqTrack, run: run, runSegment: runSegment, initialDirectory: path ?: "/link_this")
        DomainFactory.createDataFile(seqTrack: seqTrack, run: run, runSegment: runSegment, initialDirectory: path ?: "/link_this")
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
        RunSegment runSegment = DomainFactory.createRunSegment()
        SeqCenter seqCenter = DomainFactory.createSeqCenter(importDirsAllowLinking: ["/link_me", "/link_this"])
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        seqTrack.run.seqCenter = seqCenter
        seqTrack.run.save(flush: true)
        DomainFactory.createDataFile(seqTrack: seqTrack, runSegment: runSegment, initialDirectory: path ?: "/link_me")
        DomainFactory.createDataFile(seqTrack: seqTrack, runSegment: runSegment, initialDirectory: path ?: "/link_me")
        return seqTrack
    }
}
