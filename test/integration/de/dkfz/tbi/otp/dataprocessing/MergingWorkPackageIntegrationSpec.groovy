package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*

import static de.dkfz.tbi.otp.ngsdata.DomainFactory.getRandomProcessedBamFileProperties

class MergingWorkPackageIntegrationSpec extends IntegrationSpec {

    // Must be an integration test, because "String-based queries like[findAll] are
    // currently not supported in this implementation of GORM."
    void 'when seqType is WGBS, methods disregard libraryPreparationKit'() {

        given:
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(
                seqType: DomainFactory.createSeqType(name: SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName),
        )

        SeqTrack libPrepKitNull = DomainFactory.createSeqTrack(workPackage)
        assert libPrepKitNull.libraryPreparationKit == null
        DomainFactory.createSequenceDataFile(seqTrack: libPrepKitNull)

        SeqTrack libPrepKitSet = DomainFactory.createSeqTrack(workPackage,
                [libraryPreparationKit: DomainFactory.createLibraryPreparationKit()])
        assert libPrepKitSet.libraryPreparationKit != null
        DomainFactory.createSequenceDataFile(seqTrack: libPrepKitSet)

        expect:
        !MergingWorkPackage.getMergingProperties(libPrepKitNull).containsKey('libraryPreparationKit')
        !MergingWorkPackage.getMergingProperties(libPrepKitSet).containsKey('libraryPreparationKit')
        workPackage.satisfiesCriteria(libPrepKitNull)
        workPackage.satisfiesCriteria(libPrepKitSet)
        TestCase.assertContainSame([libPrepKitNull, libPrepKitSet], workPackage.findMergeableSeqTracks())
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder set, not withdrawn, FileOperationStatus PROCESSED, seqTracks match, returns bamFile'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile(randomProcessedBamFileProperties)

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        expect:
        bamFile == bamFile.workPackage.completeProcessableBamFileInProjectFolder
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder not set, not withdrawn, FileOperationStatus PROCESSED, seqTracks match, returns null'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile(randomProcessedBamFileProperties)

        expect:
        null == bamFile.workPackage.completeProcessableBamFileInProjectFolder
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder set, withdrawn, FileOperationStatus PROCESSED, seqTracks match, returns null'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile(randomProcessedBamFileProperties)
        bamFile.withdrawn = true
        bamFile.save(flush: true)

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        expect:
        null == bamFile.workPackage.completeProcessableBamFileInProjectFolder
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder set, not withdrawn, FileOperationStatus INPROGRESS, seqTracks match, returns null'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS, md5sum: null])

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        expect:
        null == bamFile.workPackage.completeProcessableBamFileInProjectFolder
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder set, not withdrawn, FileOperationStatus PROCESSED, seqTracks do not match, returns null'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile(randomProcessedBamFileProperties)
        DomainFactory.createSeqTrackWithDataFiles(bamFile.workPackage)

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        expect:
        null == bamFile.workPackage.completeProcessableBamFileInProjectFolder
    }
}
