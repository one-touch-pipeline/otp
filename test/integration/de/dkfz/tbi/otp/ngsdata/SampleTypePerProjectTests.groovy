package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import grails.validation.ValidationException
import org.junit.Test

class SampleTypePerProjectTests extends TestCase {

    @Test
    void testUniqueConstraint() {
        Project project = TestData.createProject(
                        name: "project",
                        dirName: "/dirName/",
                        realmName: "DKFZ"
                        )
        assert project.save()

        SampleType sampleType = new SampleType(
                        name: "BLOOD"
                        )
        assert sampleType.save()

        SampleTypePerProject sampleTypePerProject = new SampleTypePerProject(
                        project: project,
                        sampleType: sampleType,
                        category: SampleType.Category.DISEASE,
                        )
        assert sampleTypePerProject.save(flush: true)

        SampleTypePerProject sampleTypePerProject1 = new SampleTypePerProject(
                        project: project,
                        sampleType: sampleType,
                        category: SampleType.Category.CONTROL,
                        )
        shouldFail ValidationException, {
            sampleTypePerProject1.save(flush: true)
        }
    }

    @Test
    void testFindMissingCombinations() {
        final SeqType wholeGenome = SeqType.build(name: SeqTypeNames.WHOLE_GENOME.seqTypeName, libraryLayout: 'PAIRED')
        final SeqType exome =  SeqType.build(name: SeqTypeNames.EXOME.seqTypeName, libraryLayout: 'PAIRED')
        final SeqType chipSeq = SeqType.build(name: SeqTypeNames.CHIP_SEQ.seqTypeName, libraryLayout: 'PAIRED')
        final FileType sequenceFileType = FileType.build(type: FileType.Type.SEQUENCE)
        final Project project1 = Project.build()
        final Project project2 = Project.build()
        final SampleType sampleType1 = SampleType.build()
        final SampleType sampleType2 = SampleType.build()
        final Sample sampleProject1Type1 = Sample.build(individual: Individual.build(project: project1), sampleType: sampleType1)
        final Sample sampleProject1Type2 = Sample.build(individual: Individual.build(project: project1), sampleType: sampleType2)
        final Sample sampleProject2Type1 = Sample.build(individual: Individual.build(project: project2), sampleType: sampleType1)
        final Sample sampleProject2Type2 = Sample.build(individual: Individual.build(project: project2), sampleType: sampleType2)
        // no SeqTracks in database
        assert SampleTypePerProject.findMissingCombinations().isEmpty()

        SeqTrack.build(sample: sampleProject1Type1, seqType: chipSeq)
        // on ChIP Seq data no SNV calling is done, so no problem if there is no SampleTypePerProject for it
        assert SampleTypePerProject.findMissingCombinations().isEmpty()

        final SeqTrack sampleProject1Type1WholeGenome = SeqTrack.build(sample: sampleProject1Type1, seqType: wholeGenome)
        final SeqTrack sampleProject1Type1Exome = SeqTrack.build(sample: sampleProject1Type1, seqType: exome)
        // distinct combination should be in the result only once
        assert containSame(SampleTypePerProject.findMissingCombinations(), [
                [project1, sampleType1],
        ])

        SeqTrack.build(sample: sampleProject1Type2, seqType: exome)
        SeqTrack.build(sample: sampleProject2Type1, seqType: exome)
        SeqTrack.build(sample: sampleProject2Type2, seqType: exome)
        // different combinations
        assert containSame(SampleTypePerProject.findMissingCombinations(), [
                [project1, sampleType1],
                [project1, sampleType2],
                [project2, sampleType1],
                [project2, sampleType2],
        ])

        DataFile.build(seqTrack: sampleProject1Type1WholeGenome, fileType: sequenceFileType, fileWithdrawn: true)
        // SeqTrack withdrawn, other SeqTrack for same combination is still valid
        assert containSame(SampleTypePerProject.findMissingCombinations(), [
                [project1, sampleType1],
                [project1, sampleType2],
                [project2, sampleType1],
                [project2, sampleType2],
        ])

        DataFile.build(seqTrack: sampleProject1Type1Exome, fileType: sequenceFileType, fileWithdrawn: true)
        // both withdrawn
        assert containSame(SampleTypePerProject.findMissingCombinations(), [
                [project1, sampleType2],
                [project2, sampleType1],
                [project2, sampleType2],
        ])

        SampleTypePerProject.build(project: project2, sampleType: sampleType2)
        // SampleTypePerProject created
        assert containSame(SampleTypePerProject.findMissingCombinations(), [
                [project1, sampleType2],
                [project2, sampleType1],
        ])
    }
}
