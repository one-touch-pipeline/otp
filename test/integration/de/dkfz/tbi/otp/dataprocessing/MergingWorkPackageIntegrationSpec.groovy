package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*

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
        DomainFactory.buildSequenceDataFile(seqTrack: libPrepKitNull)

        SeqTrack libPrepKitSet = DomainFactory.createSeqTrack(workPackage,
                [libraryPreparationKit: DomainFactory.createLibraryPreparationKit()])
        assert libPrepKitSet.libraryPreparationKit != null
        DomainFactory.buildSequenceDataFile(seqTrack: libPrepKitSet)

        expect:
        !MergingWorkPackage.getMergingProperties(libPrepKitNull).containsKey('libraryPreparationKit')
        !MergingWorkPackage.getMergingProperties(libPrepKitSet).containsKey('libraryPreparationKit')
        workPackage.satisfiesCriteria(libPrepKitNull)
        workPackage.satisfiesCriteria(libPrepKitSet)
        TestCase.assertContainSame([libPrepKitNull, libPrepKitSet], workPackage.findMergeableSeqTracks())
    }
}
