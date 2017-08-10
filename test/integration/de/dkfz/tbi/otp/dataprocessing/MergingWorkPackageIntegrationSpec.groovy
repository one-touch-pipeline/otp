package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*
import spock.lang.*


class MergingWorkPackageIntegrationSpec extends IntegrationSpec {

    @Unroll
    void "constraint for sample, when seqType is not chip seq and  #text, then validate should not create errors"() {
        given:
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage()

        when:
        MergingWorkPackage workPackage2 = DomainFactory.createMergingWorkPackage([
                seqType: sameSeqType ? workPackage.seqType : DomainFactory.createSeqType(),
                sample : sameSample ? workPackage.sample : DomainFactory.createSample(),
        ], false)
        workPackage2.validate()

        then:
        workPackage2.errors.errorCount == 0

        where:
        sameSeqType | sameSample
        false       | false
        false       | true
        true        | false

        text = "seqtype is ${sameSeqType ? '' : 'not '}same and sample is ${sameSample ? '' : 'not '}same"
    }

    void "constraint for sample, when seqType is not chip seq and  seqtype and sample are same, then validate should create an errors"() {
        given:
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage()

        when:
        MergingWorkPackage workPackage2 = DomainFactory.createMergingWorkPackage([
                seqType: workPackage.seqType,
                sample : workPackage.sample,
        ], false)

        then:
        TestCase.assertValidateError(workPackage2, 'sample', 'The mergingWorkPackage must be unique for one sample and seqType and antibodyTarget', workPackage.sample)
    }

    @Unroll
    void "constraint for sample, when seqType is chip seq and  #text, then validate should not create errors"() {
        given:
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(
                seqType: DomainFactory.createChipSeqType()
        )

        when:
        MergingWorkPackage workPackage2 = DomainFactory.createMergingWorkPackage([
                seqType       : workPackage.seqType,
                sample        : sameSample ? workPackage.sample : DomainFactory.createSample(),
                antibodyTarget: sameAntibody ? workPackage.antibodyTarget : DomainFactory.createAntibodyTarget()
        ], false)
        workPackage2.validate()

        then:
        workPackage2.errors.errorCount == 0

        where:
        sameSample | sameAntibody
        false      | false
        false      | true
        true       | false

        text = "sample is ${sameSample ? '' : 'not '}same and antibody target is ${sameAntibody ? '' : 'not '}same"
    }

    void "constraint for sample, when seqType is chip seq and  sample and antibody target are same, then validate should create an errors"() {
        given:
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(
                seqType: DomainFactory.createChipSeqType()
        )

        when:
        MergingWorkPackage workPackage2 = DomainFactory.createMergingWorkPackage([
                seqType       : workPackage.seqType,
                sample        : workPackage.sample,
                antibodyTarget: workPackage.antibodyTarget,
        ], false)

        then:
        TestCase.assertValidateError(workPackage2, 'sample', 'The mergingWorkPackage must be unique for one sample and seqType and antibodyTarget', workPackage.sample)
    }


    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder set, not withdrawn, FileOperationStatus PROCESSED, seqTracks match, returns bamFile'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile(DomainFactory.randomProcessedBamFileProperties)

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        expect:
        bamFile == ((MergingWorkPackage)(bamFile.workPackage)).completeProcessableBamFileInProjectFolder
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder not set, not withdrawn, FileOperationStatus PROCESSED, seqTracks match, returns null'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile(DomainFactory.randomProcessedBamFileProperties)

        expect:
        null == ((MergingWorkPackage)(bamFile.workPackage)).completeProcessableBamFileInProjectFolder
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder set, withdrawn, FileOperationStatus PROCESSED, seqTracks match, returns null'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile(DomainFactory.randomProcessedBamFileProperties)
        bamFile.withdrawn = true
        bamFile.save(flush: true)

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        expect:
        null == ((MergingWorkPackage)(bamFile.workPackage)).completeProcessableBamFileInProjectFolder
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder set, not withdrawn, FileOperationStatus INPROGRESS, seqTracks match, returns null'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS, md5sum: null])

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        expect:
        null == ((MergingWorkPackage)(bamFile.workPackage)).completeProcessableBamFileInProjectFolder
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder set, not withdrawn, FileOperationStatus PROCESSED, seqTracks do not match, returns null'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile(DomainFactory.randomProcessedBamFileProperties)
        DomainFactory.createSeqTrackWithDataFiles(bamFile.workPackage)

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        expect:
        null == ((MergingWorkPackage)(bamFile.workPackage)).completeProcessableBamFileInProjectFolder
    }
}
