package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.InformationReliability
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*

class MergingWorkPackageTests {

    @Test
    void testFindMergeableSeqTracksNoLibraryPreparationKit() {
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(libraryPreparationKit: null)

        createDataAndAssertResult(workPackage)
    }

    @Test
    void testFindMergeableSeqTracksWithLibraryPreparationKit() {
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(libraryPreparationKit: LibraryPreparationKit.build())
        SeqTrack incorrectLibraryPreparationKit = DomainFactory.createSeqTrackWithDataFiles(workPackage)
        incorrectLibraryPreparationKit.libraryPreparationKit = LibraryPreparationKit.build()
        incorrectLibraryPreparationKit.kitInfoReliability = InformationReliability.KNOWN
        assert incorrectLibraryPreparationKit.save(flush: true, failOnError: true)

        createDataAndAssertResult(workPackage)
    }



    private createDataAndAssertResult(MergingWorkPackage workPackage) {
        SeqTrack incorrectSample = DomainFactory.createSeqTrackWithDataFiles(workPackage)
        incorrectSample.sample = Sample.build()
        assert incorrectSample.save(failOnError: true, flush: true)

        SeqTrack incorrectSeqType = DomainFactory.createSeqTrackWithDataFiles(workPackage)
        incorrectSeqType.seqType = SeqType.build()
        assert incorrectSeqType.save(failOnError: true, flush: true)

        SeqTrack incorrectSeqPlatformGroup = DomainFactory.createSeqTrackWithDataFiles(workPackage)
        incorrectSeqPlatformGroup.seqPlatform.seqPlatformGroup = SeqPlatformGroup.build()
        assert incorrectSeqPlatformGroup.seqPlatform.save(failOnError: true, flush: true)

        SeqTrack noSeqPlatformGroup = DomainFactory.createSeqTrackWithDataFiles(workPackage)
        noSeqPlatformGroup.seqPlatform.seqPlatformGroup = null
        assert noSeqPlatformGroup.seqPlatform.save(failOnError: true, flush: true)

        SeqTrack correctWithdrawn = DomainFactory.createSeqTrackWithDataFiles(workPackage)
        DataFile dataFile = DataFile.findBySeqTrack(correctWithdrawn)
        dataFile.fileWithdrawn = true
        assert dataFile.save(failOnError: true, flush: true)

        SeqTrack incorrectLibraryPreparationKit = DomainFactory.createSeqTrackWithDataFiles(workPackage)
        incorrectLibraryPreparationKit.libraryPreparationKit = LibraryPreparationKit.build()
        incorrectLibraryPreparationKit.kitInfoReliability = InformationReliability.KNOWN
        assert incorrectLibraryPreparationKit.save(flush: true, failOnError: true)

        SeqTrack correct1 = DomainFactory.createSeqTrackWithDataFiles(workPackage)
        SeqTrack correct2 = DomainFactory.createSeqTrackWithDataFiles(workPackage)

        assert TestCase.containSame([correct1, correct2], workPackage.findMergeableSeqTracks())
    }
}
