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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority

import javax.sql.DataSource

import static de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState.NOT_STARTED
import static de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState.UNKNOWN

@Rollback
@Integration
class SeqTrackServiceIntegrationSpec extends Specification {

    @Autowired
    DataSource dataSource

    @Unroll
    void "test seqTrackReadyToInstall"() {
        given:
        SeqTrackService service = new SeqTrackService([dataSource: dataSource])

        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        DomainFactory.createSeqTrack(
                seqType: seqType,
                dataInstallationState: state,
                laneId: "1"
        )

        SeqTrack seqTrack2 = DomainFactory.createSeqTrack(
                seqType: seqType,
                dataInstallationState: state,
                laneId: "2",

        )
        seqTrack2.project.processingPriority = priority.priority
        seqTrack2.save(flush: true)

        when:
        String laneId = service.seqTrackReadyToInstall(inputPriority)?.laneId

        then:
        expectedLaneId == laneId

        where:
        state       | priority                      | inputPriority                 || expectedLaneId
        UNKNOWN     | ProcessingPriority.NORMAL     | ProcessingPriority.NORMAL     || null
        NOT_STARTED | ProcessingPriority.NORMAL     | ProcessingPriority.NORMAL     || "1"
        NOT_STARTED | ProcessingPriority.FAST_TRACK | ProcessingPriority.NORMAL     || "2"
        NOT_STARTED | ProcessingPriority.FAST_TRACK | ProcessingPriority.FAST_TRACK || "2"
    }

    @SuppressWarnings('SeqTrackServiceIntegrationSpec')
    void "getSeqTrackReadyForFastqcProcessing basic lookup"() {
        given:
        SeqTrackService service = new SeqTrackService([dataSource: dataSource])
        DomainFactory.createAllAlignableSeqTypes()
        DomainFactory.createSeqTrack(params())

        when:
        SeqTrack result = service.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL)

        then:
        shouldFind ? result : !result

        where:
        shouldFind | params
        false      | { [fastqcState: UNKNOWN] }
        false      | { [fastqcState: UNKNOWN, seqType: SeqTypeService.getExomePairedSeqType()] }
        true       | { [fastqcState: NOT_STARTED] }
        true       | { [fastqcState: NOT_STARTED, seqType: SeqTypeService.getExomePairedSeqType()] }

    }

    void "getSeqTrackReadyForFastqcProcessing should prioritise alignable over rest"() {
        given:
        SeqTrackService service = new SeqTrackService([dataSource: dataSource])
        SeqType alignableSeqType = DomainFactory.createAllAlignableSeqTypes().first()

        DomainFactory.createSeqTrack([fastqcState: NOT_STARTED])
        SeqTrack alignableSeqTrack = DomainFactory.createSeqTrack([
                fastqcState: NOT_STARTED,
                seqType    : alignableSeqType,
        ])

        when:
        SeqTrack result = service.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL)

        then:
        result == alignableSeqTrack
    }

    void "getSeqTrackReadyForFastqcProcessing should prioritise older seqTracks"() {
        given:
        SeqTrackService service = new SeqTrackService([dataSource: dataSource])
        DomainFactory.createAllAlignableSeqTypes()

        SeqTrack oldestSeqTrack = DomainFactory.createSeqTrack([fastqcState: NOT_STARTED])
        DomainFactory.createSeqTrack([fastqcState: NOT_STARTED])

        when:
        SeqTrack result = service.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL)

        then:
        result == oldestSeqTrack
    }

    void "getSeqTrackReadyForFastqcProcessing should prioritise FastTrack"() {
        given:
        SeqTrackService service = new SeqTrackService([dataSource: dataSource])
        DomainFactory.createAllAlignableSeqTypes()

        DomainFactory.createSeqTrack([fastqcState: NOT_STARTED])
        SeqTrack importantSeqTrack = DomainFactory.createSeqTrack([fastqcState: NOT_STARTED])
        Project importantProject = importantSeqTrack.project
        importantProject.processingPriority = ProcessingPriority.FAST_TRACK.priority
        importantProject.save(flush: true)

        when: "asked for normal priority"
        SeqTrack result = service.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL)

        then:
        result == importantSeqTrack

        when: "asked for high priority fastTrack"
        result = service.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.FAST_TRACK)

        then:
        result == importantSeqTrack
    }

    void "getSeqTrackReadyForFastqcProcessing should ignore normal priority when asked for fastTrack"() {
        given:
        SeqTrackService service = new SeqTrackService([dataSource: dataSource])
        DomainFactory.createAllAlignableSeqTypes()
        DomainFactory.createSeqTrack([fastqcState: NOT_STARTED])

        when:
        SeqTrack result = service.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.FAST_TRACK)

        then:
        result == null
    }
}
