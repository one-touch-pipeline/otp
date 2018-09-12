package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import grails.test.spock.*
import org.springframework.beans.factory.annotation.*
import spock.lang.*

import javax.sql.DataSource

import static de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState.*

class SeqTrackServiceIntegrationSpec extends IntegrationSpec {

    @Autowired
    DataSource dataSource

    @Unroll
    void "test seqTracksReadyToInstall"() {
        given:
        SeqTrackService seqTrackService = new SeqTrackService([dataSource: dataSource])

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
        seqTrack2.project.processingPriority = priority

        when:
        List<String> laneIds = seqTrackService.seqTracksReadyToInstall(inputPriority)*.laneId

        then:
        expectedLaneIds == laneIds

        where:
        state                                    | priority                               | inputPriority                          || expectedLaneIds
        UNKNOWN     | ProcessingPriority.NORMAL_PRIORITY     | ProcessingPriority.NORMAL_PRIORITY     || []
        NOT_STARTED | ProcessingPriority.NORMAL_PRIORITY     | ProcessingPriority.NORMAL_PRIORITY     || ["1", "2"]
        NOT_STARTED | ProcessingPriority.FAST_TRACK_PRIORITY | ProcessingPriority.NORMAL_PRIORITY     || ["2", "1"]
        NOT_STARTED | ProcessingPriority.FAST_TRACK_PRIORITY | ProcessingPriority.FAST_TRACK_PRIORITY || ["2"]

    }

    void "getSeqTrackReadyForFastqcProcessing basic lookup"() {
        given:
        SeqTrackService seqTrackService = new SeqTrackService([dataSource: dataSource])
        DomainFactory.createAllAlignableSeqTypes()
        DomainFactory.createSeqTrack(params())

        when:
        SeqTrack result = seqTrackService.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL_PRIORITY)

        then:
        shouldFind? result : !result

        where:
        shouldFind | params
        false | {[ fastqcState: UNKNOWN ]}
        false | {[ fastqcState: UNKNOWN,     seqType: SeqType.getExomePairedSeqType()]}
        true  | {[ fastqcState: NOT_STARTED ]}
        true  | {[ fastqcState: NOT_STARTED, seqType: SeqType.getExomePairedSeqType()]}

    }

    void "getSeqTrackReadyForFastqcProcessing should prioritise alignable over rest"() {
        given:
        SeqTrackService service = new SeqTrackService([dataSource: dataSource])
        SeqType alignableSeqType = DomainFactory.createAllAlignableSeqTypes().first()

        DomainFactory.createSeqTrack ([ fastqcState: NOT_STARTED ])
        SeqTrack alignableSeqTrack = DomainFactory.createSeqTrack ([
                fastqcState: NOT_STARTED,
                seqType: alignableSeqType,
        ])

        when:
        SeqTrack result = service.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL_PRIORITY)

        then:
        result == alignableSeqTrack
    }

    void "getSeqTrackReadyForFastqcProcessing should prioritise older seqTracks"() {
        given:
        SeqTrackService service = new SeqTrackService([dataSource: dataSource])
        DomainFactory.createAllAlignableSeqTypes()

        SeqTrack oldestSeqTrack = DomainFactory.createSeqTrack ([fastqcState: NOT_STARTED])
        DomainFactory.createSeqTrack ([fastqcState: NOT_STARTED])

        when:
        SeqTrack result = service.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL_PRIORITY)

        then:
        result == oldestSeqTrack
    }

    void  "getSeqTrackReadyForFastqcProcessing should prioritise FastTrack"() {
        given:
        SeqTrackService service = new SeqTrackService([dataSource: dataSource])
        DomainFactory.createAllAlignableSeqTypes()

        DomainFactory.createSeqTrack ([fastqcState: NOT_STARTED])
        SeqTrack importantSeqTrack = DomainFactory.createSeqTrack ([fastqcState: NOT_STARTED])
        Project importantProject = importantSeqTrack.project
        importantProject.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        importantProject.save(flush: true)

        when: "asked for normal priority"
        SeqTrack result = service.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL_PRIORITY)
        then:
        result == importantSeqTrack

        when: "asked for high priority fastTrack"
        result = service.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.FAST_TRACK_PRIORITY)
        then:
        result == importantSeqTrack
    }

    void  "getSeqTrackReadyForFastqcProcessing should ignore normal priority when asked for fastTrack"() {
        given:
        SeqTrackService service = new SeqTrackService([dataSource: dataSource])
        DomainFactory.createAllAlignableSeqTypes()
        DomainFactory.createSeqTrack ([fastqcState: NOT_STARTED])

        when:
        SeqTrack result = service.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.FAST_TRACK_PRIORITY)

        then:
        result == null
    }
}
