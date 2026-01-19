/*
 * Copyright 2011-2026 The OTP authors
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
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Rollback
@Integration
class RoddyBamFileIntegrationSpec extends Specification implements RoddyPanCancerFactory {

    static final Long ARBITRARY_UNUSED_VALUE = 1

    static final Map ARBITRARY_QA_VALUES = [
            qcBasesMapped                  : ARBITRARY_UNUSED_VALUE,
            totalReadCounter               : ARBITRARY_UNUSED_VALUE,
            qcFailedReads                  : ARBITRARY_UNUSED_VALUE,
            duplicates                     : ARBITRARY_UNUSED_VALUE,
            totalMappedReadCounter         : ARBITRARY_UNUSED_VALUE,
            pairedInSequencing             : ARBITRARY_UNUSED_VALUE,
            pairedRead2                    : ARBITRARY_UNUSED_VALUE,
            pairedRead1                    : ARBITRARY_UNUSED_VALUE,
            properlyPaired                 : ARBITRARY_UNUSED_VALUE,
            withItselfAndMateMapped        : ARBITRARY_UNUSED_VALUE,
            withMateMappedToDifferentChr   : ARBITRARY_UNUSED_VALUE,
            withMateMappedToDifferentChrMaq: ARBITRARY_UNUSED_VALUE,
            singletons                     : ARBITRARY_UNUSED_VALUE,
            insertSizeMedian               : ARBITRARY_UNUSED_VALUE,
            insertSizeSD                   : ARBITRARY_UNUSED_VALUE,
            referenceLength                : ARBITRARY_UNUSED_VALUE,
    ].asImmutable()

    static final Map NULL_QA_VALUES = [
            qcBasesMapped                  : null,
            totalReadCounter               : null,
            qcFailedReads                  : null,
            duplicates                     : null,
            totalMappedReadCounter         : null,
            pairedInSequencing             : null,
            pairedRead2                    : null,
            pairedRead1                    : null,
            properlyPaired                 : null,
            withItselfAndMateMapped        : null,
            withMateMappedToDifferentChr   : null,
            withMateMappedToDifferentChrMaq: null,
            singletons                     : null,
            insertSizeMedian               : null,
            insertSizeSD                   : null,
            referenceLength                : null,
            percentageMatesOnDifferentChr  : null,
            insertSizeCV  : null,
    ].asImmutable()

    void "test getNumberOfReadsFromQa"() {
        given:
        long pairedRead = DomainFactory.counter++
        long numberOfReads = 2 * pairedRead
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        DomainFactory.createRoddyMergedBamQa([
                abstractBamFile: roddyBamFile,
                pairedRead1    : pairedRead,
                pairedRead2    : pairedRead,
                referenceLength: 0,
        ])

        expect:
        numberOfReads == roddyBamFile.numberOfReadsFromQa
    }

    void "test getQualityAssessment"() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        DomainFactory.createRoddyMergedBamQa(
                NULL_QA_VALUES + [
                        abstractBamFile              : bamFile,
                        chromosome                   : '12',
                        referenceLength              : 1,
                        genomeWithoutNCoverageQcBases: 1,
                ]
        )

        RoddyMergedBamQa mergedQa = DomainFactory.createRoddyMergedBamQa(
                ARBITRARY_QA_VALUES + [
                        abstractBamFile              : bamFile,
                        chromosome                   : RoddyQualityAssessment.ALL,
                        insertSizeCV                 : 123,
                        percentageMatesOnDifferentChr: 0.123,
                        genomeWithoutNCoverageQcBases: 1,
                ]
        )

        expect:
        mergedQa == bamFile.qualityAssessment
    }

    void "test constraints when all is fine"() {
        given:
        RoddyBamFile bamFile = createRBF()

        expect:
        bamFile.save(flush: true)
    }

    void "test that seqtracks connection is saved to database"() {
        when:
        createBamFile()

        then:
        RoddyBamFile.withCriteria {
            seqTracks {
                isNotNull('id')
            }
        }
    }

    void "test constraints with no seqTracks should fail"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        bamFile.seqTracks = [] as Set

        when:
        bamFile.validate()

        then:
        TestCase.assertAtLeastExpectedValidateError(bamFile, 'seqTracks', 'minSize.notmet', bamFile.seqTracks)
    }

    void "test constraints with not Roddy pipeline name should fail"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        bamFile.workPackage.pipeline.name = Pipeline.Name.DEFAULT_OTP
        bamFile.config.pipeline.name = Pipeline.Name.DEFAULT_OTP

        when:
        boolean isValid = bamFile.validate()

        then:
        !isValid
        Errors errors = bamFile.errors
        errors.errorCount == 2
        errors.fieldErrorCount == 2
        List<FieldError> fieldErrors = errors.fieldErrors
        fieldErrors*.field == ['config.pipeline', 'workPackage']
        fieldErrors*.rejectedValue == [bamFile.config.pipeline, bamFile.workPackage]
    }

    void "test constraints when pipeline in config and workPackage inconsistent should fail"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        bamFile.config.pipeline = DomainFactory.createDefaultOtpPipeline()

        expect:
        TestCase.assertValidateError(bamFile, 'config', 'validator.invalid', bamFile.config)
    }

    void "test constraints with not unique identifier for workPackage should fail"() {
        given:
        RoddyBamFile bamFile = createRBF()
        RoddyBamFile bamFile2 = createBamFile(workPackage: bamFile.workPackage)
        bamFile.identifier = bamFile2.identifier

        expect:
        TestCase.assertValidateError(bamFile, 'identifier', 'validator.invalid', bamFile.identifier)
    }

    void "test constraints when workPackage is null should fail"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        bamFile.workPackage = null

        expect:
        TestCase.assertAtLeastExpectedValidateError(bamFile, 'workPackage', 'nullable', bamFile.workPackage)
    }

    void "test isConsistentAndContainsNoWithdrawnData when seqTrack does not belong to bamFile workPackage is also valid"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        seqTrack.seqType = DomainFactory.createSeqType()

        expect:
        bamFile.isConsistentAndContainsNoWithdrawnData().empty
    }

    void "test isConsistentAndContainsNoWithdrawnData with withdrawn bamFile succeeds"() {
        given:
        RoddyBamFile bamFile = createRBF()
        bamFile.withdrawn = true

        expect:
        bamFile.isConsistentAndContainsNoWithdrawnData().empty
    }

    void "test isConsistentAndContainsNoWithdrawnData with withdrawn bamFile with withdrawn seqTracks succeeds"() {
        given:
        RoddyBamFile bamFile = createBamFile([withdrawn: true])
        List<RawSequenceFile> rawSequenceFiles = RawSequenceFile.findAll()
        rawSequenceFiles*.fileWithdrawn = true
        rawSequenceFiles*.save(flush: true)

        expect:
        [] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    void "test isConsistentAndContainsNoWithdrawnData with withdrawn bamFile with not withdrawn seqTracks succeeds"() {
        given:
        RoddyBamFile bamFile = createBamFile([withdrawn: true])

        expect:
        [] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    void "test isConsistentAndContainsNoWithdrawnData with not withdrawn bamFile with withdrawn seqTracks should return error message"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        List<RawSequenceFile> rawSequenceFiles = RawSequenceFile.findAll()
        rawSequenceFiles*.fileWithdrawn = true
        rawSequenceFiles*.save(flush: true)

        expect:
        ["not withdrawn bam file has withdrawn seq tracks"] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    void "test isConsistentAndContainsNoWithdrawnData when numberOfMergedLanes not equal to numberOfContainedLanes should return error message"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        bamFile.numberOfMergedLanes = 5

        expect:
        ["total number of merged lanes is not equal to number of contained seq tracks: 5 vs 1"] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    void "test isMostRecentBamFile"() {
        given:
        RoddyBamFile bamFile = createRBF()

        expect:
        bamFile.isMostRecentBamFile()
    }

    void "test maxIdentifier when no RoddyBamFile exists for workPackage"() {
        given:
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage()

        expect:
        RoddyBamFile.maxIdentifier(workPackage) == null
    }

    void "test maxIdentifier when RoddyBamFile exists for workPackage"() {
        given:
        RoddyBamFile bamFile = createRBF()

        expect:
        RoddyBamFile.maxIdentifier(bamFile.workPackage) == 0
    }

    void "test withdraw single file should set to withdrawn"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile()

        expect:
        !roddyBamFile.withdrawn

        when:
        LogThreadLocal.withThreadLog(System.out) {
            roddyBamFile.withdraw()
        }

        then:
        roddyBamFile.withdrawn
    }

    private RoddyBamFile createRBF() {
        return createBamFile([
                md5sum: null,
                fileOperationStatus: AbstractBamFile.FileOperationStatus.DECLARED,
                fileSize: -1,
        ])
    }
}
