package de.dkfz.tbi.otp.ngsdata

import grails.test.spock.IntegrationSpec
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority

import javax.sql.DataSource

import static de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState.NOT_STARTED
import static de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState.UNKNOWN

class SeqTrackServiceIntegrationSpec extends IntegrationSpec {

    @Autowired
    DataSource dataSource

    @Unroll
    void "test seqTrackReadyToInstall"() {
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
        seqTrack2.project.processingPriority = priority.priority

        when:
        String laneId = seqTrackService.seqTrackReadyToInstall(inputPriority)?.laneId

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
        SeqTrackService seqTrackService = new SeqTrackService([dataSource: dataSource])
        DomainFactory.createAllAlignableSeqTypes()
        DomainFactory.createSeqTrack(params())

        when:
        SeqTrack result = seqTrackService.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL)

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
