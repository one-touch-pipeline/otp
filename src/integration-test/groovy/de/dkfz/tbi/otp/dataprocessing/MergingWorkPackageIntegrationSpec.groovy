/*
 * Copyright 2011-2023 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Rollback
@Integration
class MergingWorkPackageIntegrationSpec extends Specification {

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
        TestCase.assertValidateError(workPackage2, 'sample', 'unique', workPackage.sample)
    }

    @Unroll
    void "constraint for sample, when seqType is chip seq and #text, then validate should not create errors"() {
        given:
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(
                seqType: DomainFactory.createChipSeqType()
        )

        when:
        MergingWorkPackage workPackage2 = DomainFactory.createMergingWorkPackage([
                seqType       : workPackage.seqType,
                sample        : sameSample ? workPackage.sample : DomainFactory.createSample(),
                antibodyTarget: sameAntibody ? workPackage.antibodyTarget : DomainFactory.createAntibodyTarget(),
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
        TestCase.assertValidateError(workPackage2, 'sample', 'unique', workPackage.sample)
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder set, not withdrawn, FileOperationStatus PROCESSED, seqTracks match, returns bamFile'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile(DomainFactory.randomBamFileProperties)

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        expect:
        bamFile == ((MergingWorkPackage)(bamFile.workPackage)).bamFileThatIsReadyForFurtherAnalysis
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder not set, not withdrawn, FileOperationStatus PROCESSED, seqTracks match, returns null'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile(DomainFactory.randomBamFileProperties)

        expect:
        null == ((MergingWorkPackage)(bamFile.workPackage)).bamFileThatIsReadyForFurtherAnalysis
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder set, withdrawn, FileOperationStatus PROCESSED, seqTracks match, returns null'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile(DomainFactory.randomBamFileProperties)
        bamFile.withdrawn = true
        bamFile.save(flush: true)

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        expect:
        null == ((MergingWorkPackage)(bamFile.workPackage)).bamFileThatIsReadyForFurtherAnalysis
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder set, not withdrawn, FileOperationStatus INPROGRESS, seqTracks match, returns null'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([fileOperationStatus: AbstractBamFile.FileOperationStatus.INPROGRESS, md5sum: null])

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        expect:
        null == ((MergingWorkPackage)(bamFile.workPackage)).bamFileThatIsReadyForFurtherAnalysis
    }

    void 'getCompleteProcessableBamFileInProjectFolder, when bamFileInProjectFolder set, not withdrawn, FileOperationStatus PROCESSED, seqTracks do not match, returns null'() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile(DomainFactory.randomBamFileProperties)
        DomainFactory.createSeqTrackWithDataFiles(bamFile.workPackage)

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)

        expect:
        null == ((MergingWorkPackage)(bamFile.workPackage)).bamFileThatIsReadyForFurtherAnalysis
    }
}
