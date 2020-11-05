/*
 * Copyright 2011-2019 The OTP authors
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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*

class MergingWorkPackageSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalMergingWorkPackage,
                ReferenceGenome,
                Sample,
                SeqPlatformGroup,
                SeqType,
                SeqPlatform,
                SeqTrack,
                MergingCriteria,
                MergingWorkPackage,
                ProcessedBamFile,
                AlignmentPass,
        ]
    }


    void "validate&save, when all fine, then object is saved"() {
        given:
        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                sample: createSample(),
                seqType: createSeqType(),
                seqPlatformGroup: createSeqPlatformGroup(),
                referenceGenome: createReferenceGenome(),
                pipeline: DomainFactory.createDefaultOtpPipeline(),
        )

        when:
        mergingWorkPackage.validate()

        then:
        !mergingWorkPackage.errors.hasErrors()

        expect:
        mergingWorkPackage.save(flush: true)
    }

    @Unroll
    void "getMergingProperties, when withLibPrepKit=#withLibPrepKit and withAntibodyTarget=#withAntibodyTarget, then return expected map"() {
        given:
        LibraryPreparationKit libraryPreparationKit = withLibPrepKit ? createLibraryPreparationKit() : null
        SeqTrack seqTrack = createSeqTrack([
                antibodyTarget       : withAntibodyTarget ? createAntibodyTarget() : null,
                libraryPreparationKit: libraryPreparationKit,
                seqType              : createSeqType([hasAntibodyTarget: withAntibodyTarget]),
        ])
        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        Map expectedResult = [
                sample               : seqTrack.sample,
                seqType              : seqTrack.seqType,
                seqPlatformGroup     : seqTrack.seqPlatformGroup,
                libraryPreparationKit: libraryPreparationKit,
        ]
        if (withAntibodyTarget) {
            expectedResult << [antibodyTarget: seqTrack.antibodyTarget]
        }

        when:
        Map result = MergingWorkPackage.getMergingProperties(seqTrack)

        then:
        result == expectedResult

        where:
        withLibPrepKit | withAntibodyTarget
        false          | false
        true           | false
        false          | true
        true           | true
    }

    @Unroll
    void "satisfiesCriteriaSeqTrack, when correct (withLibPrepKit=#withLibPrepKit), then return true"() {
        given:
        SeqTrack seqTrack = createSeqTrack(libraryPreparationKit: withLibPrepKit ? createLibraryPreparationKit() : null)
        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage([
                sample               : seqTrack.sample,
                seqType              : seqTrack.seqType,
                seqPlatformGroup     : seqTrack.seqPlatformGroup,
                libraryPreparationKit: seqTrack.libraryPreparationKit,
        ])

        expect:
        workPackage.satisfiesCriteria(seqTrack)

        where:
        withLibPrepKit << [
                false,
                true,
        ]
    }


    @Unroll
    void "satisfiesCriteriaSeqTrack, when ignoreLibraryPreparationKit is set, then ignore libPrepKit return true  (#seqTrackHasLibPrepKit, #workPackageHasLibPrepKit)"() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                libraryPreparationKit: seqTrackHasLibPrepKit ? createLibraryPreparationKit() : null,
        )
        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType, useLibPrepKit: false)

        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage([
                sample               : seqTrack.sample,
                seqType              : seqTrack.seqType,
                seqPlatformGroup     : seqTrack.seqPlatformGroup,
                libraryPreparationKit: workPackageHasLibPrepKit ? createLibraryPreparationKit() : null,
        ])

        expect:
        workPackage.satisfiesCriteria(seqTrack)

        where:
        seqTrackHasLibPrepKit | workPackageHasLibPrepKit
        false                 | false
        true                  | false
        false                 | true
        true                  | true
    }

    @Unroll
    void "satisfiesCriteriaSeqTrack, when #name is incorrect, then return false"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack([
                libraryPreparationKit: DomainFactory.createLibraryPreparationKit(),
        ])
        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage([
                sample               : incorrectSample ? createSample() : seqTrack.sample,
                seqType              : incorrectSeqType ? createSeqType() : seqTrack.seqType,
                seqPlatformGroup     : incorrectSeqPlatformGroup ? createSeqPlatformGroup() : seqTrack.seqPlatformGroup,
                libraryPreparationKit: incorrectLibraryPreperationKit ? createLibraryPreparationKit() : seqTrack.libraryPreparationKit,
        ])

        expect:
        !workPackage.satisfiesCriteria(seqTrack)

        where:
        name                      | incorrectSample | incorrectSeqType | incorrectSeqPlatformGroup | incorrectLibraryPreperationKit
        'sample'                  | true            | false            | false                     | false
        'seqType'                 | false           | true             | false                     | false
        'seqplatform group'       | false           | false            | true                      | false
        'library preperation kit' | false           | false            | false                     | true
    }

    void "satisfiesCriteria with BamFile, when valid, then return true"() {
        given:
        SeqPlatformGroup seqPlatformGroup = createSeqPlatformGroup()
        SeqPlatform seqPlatform = createSeqPlatformWithSeqPlatformGroup(seqPlatformGroups: [seqPlatformGroup])
        SeqTrack seqTrack = createSeqTrack(run: DomainFactory.createRun(seqPlatform: seqPlatform))
        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage([
                sample               : seqTrack.sample,
                seqType              : seqTrack.seqType,
                seqPlatformGroup     : seqPlatformGroup,
                libraryPreparationKit: seqTrack.libraryPreparationKit,
                pipeline             : DomainFactory.createDefaultOtpPipeline(),
        ])
        AlignmentPass alignmentPass = DomainFactory.createAlignmentPass([
                seqTrack   : seqTrack,
                workPackage: workPackage,
        ])
        ProcessedBamFile processedBamFile = DomainFactory.createProcessedBamFile(alignmentPass: alignmentPass)

        expect:
        workPackage.satisfiesCriteria(processedBamFile)
    }

    void "satisfiesCriteria with BamFile, when invalid, then return false"() {
        given:
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME)

        ProcessedBamFile processedBamFile = DomainFactory.createProcessedBamFile()

        expect:
        !workPackage.satisfiesCriteria(processedBamFile)
    }

    void "valid for on statSizeFileName and OTP pipeline, when name is null for, should be valid"() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: null,
                pipeline        : DomainFactory.createDefaultOtpPipeline(),
        ], false)

        when:
        mergingWorkPackage.validate()

        then:
        !mergingWorkPackage.errors.hasErrors()
    }

    void "valid on constraint on statSizeFileName and OTP pipeline, with name, should be invalid"() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
                pipeline        : DomainFactory.createDefaultOtpPipeline(),
        ], false)

        expect:
        TestCase.assertValidateError(mergingWorkPackage, 'statSizeFileName', 'validator.invalid', DomainFactory.DEFAULT_TAB_FILE_NAME)
    }

    void "valid on constraint on statSizeFileName and PANCAN pipeline, with valid name, should be valid"() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
                pipeline        : DomainFactory.createPanCanPipeline(),
        ], false)

        when:
        mergingWorkPackage.validate()

        then:
        !mergingWorkPackage.errors.hasErrors()
    }

    void "valid on constraint on statSizeFileName and PANCAN pipeline, when name is name, should be invalid"() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: null,
                pipeline        : DomainFactory.createPanCanPipeline(),
        ], false)

        expect:
        TestCase.assertValidateError(mergingWorkPackage, 'statSizeFileName', 'validator.invalid', null)
    }

    void "valid on constraint on statSizeFileName and PANCAN pipeline, when name is blank, should be invalid"() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: null,
                pipeline        : DomainFactory.createPanCanPipeline(),
        ], false)
        mergingWorkPackage.statSizeFileName = '' //setting empty string does not work via map

        expect:
        TestCase.assertValidateError(mergingWorkPackage, 'statSizeFileName', 'blank', '')
    }

    @Unroll
    void "valid on constraint on statSizeFileName and PANCAN pipeline, when name contains valid char '#c', should be valid"() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: "File${c}.tab",
                pipeline        : DomainFactory.createPanCanPipeline(),
        ], false)

        when:
        mergingWorkPackage.validate()

        then:
        !mergingWorkPackage.errors.hasErrors()

        where:
        c << "-_.".collect()
    }

    @Unroll
    void "valid on constraint on statSizeFileName and PANCAN pipeline, when name contains invalid char '#c', should be invalid"() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: "File${c}.tab",
                pipeline        : DomainFactory.createPanCanPipeline(),
        ], false)

        expect:
        TestCase.assertAtLeastExpectedValidateError(mergingWorkPackage, 'statSizeFileName', 'matches.invalid', mergingWorkPackage.statSizeFileName)

        where:
        c << "\"',:;%\$§&<>|^§!?=äöüÄÖÜß´`".collect()
    }

    @Unroll
    void "valid for constraint libraryPreparationKit, when seqType do not care about libPrepKit and workpackage #name libPrepKit, then should be valid"() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                libraryPreparationKit: withLibPrepKit ? createLibraryPreparationKit() : null,
                seqType              : createSeqType(),
        ], false)

        when:
        mergingWorkPackage.validate()

        then:
        !mergingWorkPackage.errors.hasErrors()

        where:
        name          | withLibPrepKit
        'do not have' | false
        'has'         | true
    }

    void "valid for constraint libraryPreparationKit, when exome seq type and workpackage has libraryPreparationKit, then should be valid"() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                libraryPreparationKit: createLibraryPreparationKit(),
                seqType              : DomainFactory.createExomeSeqType(),
        ], false)

        when:
        mergingWorkPackage.validate()

        then:
        !mergingWorkPackage.errors.hasErrors()
    }

    void "valid for constraint libraryPreparationKit, when exome seq type and workpackage has no libraryPreparationKit, then should be invalid"() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                libraryPreparationKit: null,
                seqType              : DomainFactory.createExomeSeqType(),
        ], false)

        expect:
        TestCase.assertValidateError(mergingWorkPackage, 'libraryPreparationKit', 'validator.invalid', null)
    }

    void "valid for constraint libraryPreparationKit, when WGBS seq type and workpackage has no libraryPreparationKit, then should be valid"() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                libraryPreparationKit: null,
                seqType              : DomainFactory.createWholeGenomeSeqType(),
        ], false)

        when:
        mergingWorkPackage.validate()

        then:
        !mergingWorkPackage.errors.hasErrors()
    }

    void "valid for constraint libraryPreparationKit, when WGBS seq type and workpackage has libraryPreparationKit, then should be invalid"() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                libraryPreparationKit: createLibraryPreparationKit(),
                seqType              : DomainFactory.createWholeGenomeBisulfiteSeqType(),
        ], false)

        expect:
        TestCase.assertValidateError(mergingWorkPackage, 'libraryPreparationKit', 'validator.invalid', mergingWorkPackage.libraryPreparationKit)
    }

    void "save, when ExternalMergingWorkPackage exist, then it should be possible to save a MeringWorkPackage with same sample and seqType"() {
        given:
        ExternalMergingWorkPackage externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage()

        expect:
        DomainFactory.createMergingWorkPackage([
                sample : externalMergingWorkPackage.sample,
                seqType: externalMergingWorkPackage.seqType,
        ])
    }

    void "save, when MergingWorkPackage exist, then it should not be possible to save a MeringWorkPackage with same sample and seqType"() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage(pipeline: DomainFactory.createDefaultOtpPipeline())

        when:
        MergingWorkPackage mergingWorkPackage1 = new MergingWorkPackage(
                sample: mergingWorkPackage.sample,
                seqType: mergingWorkPackage.seqType,
                referenceGenome: mergingWorkPackage.referenceGenome,
                seqPlatformGroup: mergingWorkPackage.seqPlatformGroup,
                pipeline: mergingWorkPackage.pipeline,
        )

        then:
        TestCase.assertValidateError(mergingWorkPackage1, 'sample', 'unique', mergingWorkPackage.sample)
    }
}
