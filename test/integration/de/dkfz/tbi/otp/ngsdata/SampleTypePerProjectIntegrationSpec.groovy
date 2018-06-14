package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.CollectionUtils
import grails.test.spock.IntegrationSpec

class SampleTypePerProjectIntegrationSpec extends IntegrationSpec {
    Project project2
    Project project1
    SampleType sampleType1
    SampleType sampleType2
    Sample sampleProject1Type1
    Sample sampleProject1Type2
    Sample sampleProject2Type1
    Sample sampleProject2Type2

    void setupSpec() {
        DomainFactory.createAllAlignableSeqTypes()
    }

    void cleanupSpec() {
        SeqType.findAll()*.delete(flush: true)
    }

    void setup() {
        // create cross-matrix of two projects and two sample types
        // (slightly involved because project is only linked via individual)
        project1 = DomainFactory.createProject()
        project2 = DomainFactory.createProject()
        sampleType1 = DomainFactory.createSampleType()
        sampleType2 = DomainFactory.createSampleType()

        sampleProject1Type1 = DomainFactory.createSample([
                individual: DomainFactory.createIndividual([project: project1]),
                sampleType: sampleType1
        ])
        sampleProject1Type2 = DomainFactory.createSample([
                individual: DomainFactory.createIndividual([project: project1]),
                sampleType: sampleType2
        ])
        sampleProject2Type1 = DomainFactory.createSample([
                individual: DomainFactory.createIndividual([project: project2]),
                sampleType: sampleType1
        ])
        sampleProject2Type2 = DomainFactory.createSample([
                individual: DomainFactory.createIndividual([project: project2]),
                sampleType: sampleType2
        ])
    }

    void "shouldn't find anything for database without seqtracks"() {
        expect:
        SampleTypePerProject.findMissingCombinations().isEmpty()
    }

    void "ChIPseq (which doesn't support SVN calling) shouldn't yield combinations"() {
        given:
        DomainFactory.createSeqTrack(sample: sampleProject1Type1, seqType: SeqType.getChipSeqPairedSeqType())

        expect:
        SampleTypePerProject.findMissingCombinations().isEmpty()
    }

    void "Should find missing combinations for all alignable seqtypes, but only distinct combinations - single sampletype&project"() {
        given: "a project with seqtracks for all alignable seqtypes"
        // since we only did `DomainFactory.createAllAlignableSeqTypes()`, this finds only alignable seqtypes
        def allAlignableSeqtypes = SeqType.findAll()
        allAlignableSeqtypes.each { SeqType alignableSeqtype ->
            DomainFactory.createSeqTrack(sample: sampleProject1Type1, seqType: alignableSeqtype)
        }

        expect: "only one distinct sampletype-project combination found"
        CollectionUtils.containSame(SampleTypePerProject.findMissingCombinations(), [
                [project1, sampleType1],
        ])
    }

    void "Should find missing combinations for all alignable seqtypes, but only distinct combinations - multiple sampletype&project"() {
        given: "four seqtracks for four combinations"
        SeqType exome = SeqType.getExomePairedSeqType()
        DomainFactory.createSeqTrack(sample: sampleProject1Type1, seqType: exome)
        DomainFactory.createSeqTrack(sample: sampleProject1Type2, seqType: exome)
        DomainFactory.createSeqTrack(sample: sampleProject2Type1, seqType: exome)
        DomainFactory.createSeqTrack(sample: sampleProject2Type2, seqType: exome)

        expect: "four different combinations found"
        CollectionUtils.containSame(SampleTypePerProject.findMissingCombinations(), [
                [project1, sampleType1],
                [project1, sampleType2],
                [project2, sampleType1],
                [project2, sampleType2],
        ])
    }


    void "combinations with (only) withdrawn data should be skipped" () {
        given: "A combination with only some datafiles withdrawn"
        SeqType wholeGenome = SeqType.getWholeGenomePairedSeqType()
        SeqType exome = SeqType.getExomePairedSeqType()

        final SeqTrack sampleProject1Type1Exome = DomainFactory.createSeqTrack(sample: sampleProject1Type1, seqType: exome)
        final SeqTrack sampleProject1Type1WholeGenome = DomainFactory.createSeqTrack(sample: sampleProject1Type1, seqType: wholeGenome)
        final FileType sequenceFileType = DomainFactory.createFileType([type: FileType.Type.SEQUENCE])
        def dataFileWGS = DomainFactory.createDataFile(fileType: sequenceFileType, seqTrack: sampleProject1Type1WholeGenome)
        def dataFileExome = DomainFactory.createDataFile(fileType: sequenceFileType, seqTrack: sampleProject1Type1Exome)
        // SeqTrack withdrawn, other SeqTrack for same combination is still valid

        when: "some, but not all datafiles withdrawn"
        dataFileWGS.fileWithdrawn = true
        dataFileWGS.save(flush: true)

        then: "expect to find all combinations, since there is a non-withdrawn datafile available"
        CollectionUtils.containSame(SampleTypePerProject.findMissingCombinations(), [
                [project1, sampleType1],
        ])

        when: "all datafiles for a sampleTypePerProject are withdrawn"
        dataFileExome.fileWithdrawn = true
        dataFileExome.save(flush: true)

        then: "expect to not-find the combination"
        SampleTypePerProject.findMissingCombinations().isEmpty()
    }

    void "shouldn't find previously-created SampleTypePerProject"() {
        given: "seqtracks for all four possible combinations"
        SeqType exome = SeqType.getExomePairedSeqType()

        DomainFactory.createSeqTrack(sample: sampleProject1Type1, seqType: exome)
        DomainFactory.createSeqTrack(sample: sampleProject1Type2, seqType: exome)
        DomainFactory.createSeqTrack(sample: sampleProject2Type1, seqType: exome)
        DomainFactory.createSeqTrack(sample: sampleProject2Type2, seqType: exome)

        when: "one combination is manually created"
        DomainFactory.createSampleTypePerProject([project: project2, sampleType: sampleType2])

        then: "only find the other three as 'missing'"
        CollectionUtils.containSame(SampleTypePerProject.findMissingCombinations(), [
                [project1, sampleType1],
                [project1, sampleType2],
                [project2, sampleType1],
        ])
    }
}
