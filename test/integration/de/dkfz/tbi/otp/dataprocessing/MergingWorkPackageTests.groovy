package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.InformationReliability
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*

class MergingWorkPackageTests {

    @Test
    void testFindMergeableSeqTracksNoLibraryPreparationKit() {
        MergingWorkPackage workPackage = MergingWorkPackage.build(libraryPreparationKit: null)

        createDataAndAssertResult(workPackage)
    }

    @Test
    void testFindMergeableSeqTracksWithLibraryPreparationKit() {
        MergingWorkPackage workPackage = MergingWorkPackage.build(libraryPreparationKit: LibraryPreparationKit.build())
        SeqTrack incorrectLibraryPreparationKit = DomainFactory.buildSeqTrackWithDataFile(workPackage)
        incorrectLibraryPreparationKit.libraryPreparationKit = LibraryPreparationKit.build()
        incorrectLibraryPreparationKit.kitInfoReliability = InformationReliability.KNOWN
        assert incorrectLibraryPreparationKit.save(flush: true, failOnError: true)

        createDataAndAssertResult(workPackage)
    }



    private createDataAndAssertResult(MergingWorkPackage workPackage) {
        SeqTrack incorrectSample = DomainFactory.buildSeqTrackWithDataFile(workPackage)
        incorrectSample.sample = Sample.build()
        assert incorrectSample.save(failOnError: true)

        SeqTrack incorrectSeqType = DomainFactory.buildSeqTrackWithDataFile(workPackage)
        incorrectSeqType.seqType = SeqType.build()
        assert incorrectSeqType.save(failOnError: true)

        SeqTrack incorrectSeqPlatformGroup = DomainFactory.buildSeqTrackWithDataFile(workPackage)
        incorrectSeqPlatformGroup.seqPlatform.seqPlatformGroup = SeqPlatformGroup.build()
        assert incorrectSeqPlatformGroup.seqPlatform.save(failOnError: true)

        SeqTrack noSeqPlatformGroup = DomainFactory.buildSeqTrackWithDataFile(workPackage)
        noSeqPlatformGroup.seqPlatform.seqPlatformGroup = null
        assert noSeqPlatformGroup.seqPlatform.save(failOnError: true)

        SeqTrack correctWithdrawn = DomainFactory.buildSeqTrackWithDataFile(workPackage)
        DataFile dataFile = DataFile.findBySeqTrack(correctWithdrawn)
        dataFile.fileWithdrawn = true
        assert dataFile.save(failOnError: true)

        SeqTrack incorrectLibraryPreparationKit = DomainFactory.buildSeqTrackWithDataFile(workPackage)
        incorrectLibraryPreparationKit.libraryPreparationKit = LibraryPreparationKit.build()
        incorrectLibraryPreparationKit.kitInfoReliability = InformationReliability.KNOWN
        assert incorrectLibraryPreparationKit.save(flush: true, failOnError: true)

        SeqTrack correct1 = DomainFactory.buildSeqTrackWithDataFile(workPackage)
        SeqTrack correct2 = DomainFactory.buildSeqTrackWithDataFile(workPackage)

        assert TestCase.containSame([correct1, correct2], workPackage.findMergeableSeqTracks())
    }
}
