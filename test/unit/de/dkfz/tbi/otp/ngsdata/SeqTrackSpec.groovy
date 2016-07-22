package de.dkfz.tbi.otp.ngsdata;

import spock.lang.*

@Mock([
    DataFile,
    FileType,
    Individual,
    Project,
    ProjectCategory,
    Run,
    Sample,
    SampleType,
    SeqCenter,
    SeqPlatform,
    SeqPlatformGroup,
    SeqTrack,
    SeqType,
    SoftwareTool,
])
public class SeqTrackSpec extends Specification {

    void "test getNReads, returns null"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createDataFile([seqTrack: seqTrack, nReads: null])
        DomainFactory.createDataFile([seqTrack: seqTrack, nReads: input])

        expect:
        seqTrack.getNReads() == null

        where:
        input   | _
        null    | _
        420     | _
    }

    void "test getNReads, returns sum"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createDataFile([seqTrack: seqTrack, nReads: 525])
        DomainFactory.createDataFile([seqTrack: seqTrack, nReads: 25])

        expect:
        seqTrack.getNReads() == 550
    }
}
