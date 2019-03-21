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

import de.dkfz.tbi.otp.LogMessage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig

// If the class is named SeqTrackServiceSpec, Grails tries to autowire the service, which fails.
// tested with Grails 2.5.1
class SeqTrackServiceNoAutowireSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {[
            FileType,
            Individual,
            LogMessage,
            Pipeline,
            Project,
            Realm,
            RoddyWorkflowConfig,
            Run,
            RunSegment,
            Sample,
            SampleType,
            SeqCenter,
            SeqPlatform,
            SeqPlatformGroup,
            SeqPlatformModelLabel,
            SeqTrack,
            SeqType,
            SoftwareTool,
    ]}

    SeqTrackService service = new SeqTrackService()

    void setup() {
        DomainFactory.createPanCanPipeline()
    }


    void "test determineAndStoreIfFastqFilesHaveToBeLinked, seqTrack is null, should fail"() {
        when:
        service.determineAndStoreIfFastqFilesHaveToBeLinked(null, true)

        then:
        thrown(AssertionError)
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, can be linked"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()

        when:
        service.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == true
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, will be aligned false, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()

        when:
        service.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, false)

        then:
        seqTrack.linkedExternally == false
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, seq center doesn't allow linking, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
        seqTrack.run.seqCenter.importDirsAllowLinking = []

        when:
        service.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == false
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, other path, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked("/other_path")

        when:
        service.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == false
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, project doesn't allow linking, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
        seqTrack.sample.individual.project.forceCopyFiles = true

        when:
        service.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == false
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, WGBS data, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
        SeqType wgbsSeqType = DomainFactory.createWholeGenomeBisulfiteSeqType()
        seqTrack.seqType = wgbsSeqType

        when:
        service.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

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
        service.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

        then:
        seqTrack.linkedExternally == false
    }

    void "test determineAndStoreIfFastqFilesHaveToBeLinked, with adapter trimming, has to be copied"() {
        given:
        SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
        DomainFactory.createRoddyWorkflowConfig([seqType: seqTrack.seqType, individual: seqTrack.individual, adapterTrimmingNeeded: true])

        when:
        service.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

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
        service.doesImportDirAllowLinking(null)

        then:
        thrown(AssertionError)
    }

    void "test doesImportDirAllowLinking, data files on linkable"() {
        given:
        SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage()

        expect:
        true == service.doesImportDirAllowLinking(seqTrack)
    }

    void "test doesImportDirAllowLinking, data files in other linkable dir"() {
        given:
        SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage("/link_this")

        expect:
        true == service.doesImportDirAllowLinking(seqTrack)
    }

    void "test doesImportDirAllowLinking, data files in other non linkable dir"() {
        given:
        SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage("/link_this_too")

        expect:
        false == service.doesImportDirAllowLinking(seqTrack)
    }

    void "test doesImportDirAllowLinking, data files not in linkable dir"() {
        given:
        SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage("/other_path")

        expect:
        false == service.doesImportDirAllowLinking(seqTrack)
    }

    void "test doesImportDirAllowLinking, seq center doesn't have linkable dir"() {
        given:
        SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage()

        seqTrack.run.seqCenter.importDirsAllowLinking = []
        seqTrack.run.seqCenter.save(flush: true)

        expect:
        false == service.doesImportDirAllowLinking(seqTrack)
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
