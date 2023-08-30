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

import grails.artefact.Artefact
import grails.artefact.DomainClass
import grails.testing.gorm.DataTest
import grails.validation.Validateable
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.GormEntity
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class AbstractMergingWorkPackageSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                AbstractMergingWorkPackage,
                AntibodyTarget,
                Individual,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                Project,
                ReferenceGenome,
                Sample,
                SampleType,
                SeqPlatformGroup,
                SeqType,
                TestAbstractBamFile,
                TestAbstractMergingWorkPackage,
        ]
    }

    TestAbstractMergingWorkPackage testAMWP
    TestAbstractMergingWorkPackage testAMWP2
    TestAbstractBamFile testAMBF

    @Artefact(DomainClassArtefactHandler.TYPE)
    class TestAbstractMergingWorkPackage extends AbstractMergingWorkPackage implements DomainClass, GormEntity<TestAbstractMergingWorkPackage>, Validateable {

        final AbstractBamFile bamFileThatIsReadyForFurtherAnalysis = null
    }

    @Artefact(DomainClassArtefactHandler.TYPE)
    class TestAbstractBamFile extends AbstractBamFile implements DomainClass, GormEntity<TestAbstractBamFile>, Validateable {

        final String bamFileName = null

        final String baiFileName = null

        final AlignmentConfig alignmentConfig = null

        final Set<SeqTrack> containedSeqTracks = []

        final AbstractQualityAssessment qualityAssessment = null

        final File finalInsertSizeFile = null

        final Integer maximalReadLength = null

        @Override
        boolean isMostRecentBamFile() {
            return false
        }

        @Override
        void withdraw() {
        }

        @Override
        protected File getPathForFurtherProcessingNoCheck() {
            return null
        }

        @Override
        AbstractMergingWorkPackage getMergingWorkPackage() {
            return TestAbstractMergingWorkPackage.get(workPackage.id)
        }
    }

    def setup() {
        testAMWP = new TestAbstractMergingWorkPackage()
        testAMWP2 = new TestAbstractMergingWorkPackage()
        testAMBF = new TestAbstractBamFile()
    }

    void "test constraint bamFileInProjectFolder, when workpackage invalid then validate fails"() {
        given: "Given an AbstractMergingWorkPackage, you assign a different one to an AbstractBamFile"
        testAMBF.workPackage = testAMWP2
        testAMWP.bamFileInProjectFolder = testAMBF
        testAMBF.fileOperationStatus = AbstractBamFile.FileOperationStatus.PROCESSED

        expect:
        TestCase.assertAtLeastExpectedValidateError(testAMWP, "bamFileInProjectFolder", "validator.invalid", testAMBF)
    }

    void "test constraint bamFileInProjectFolder, when fileOperationStatus invalid then validate fails"() {
        given:
        testAMBF.workPackage = testAMWP
        testAMWP.bamFileInProjectFolder = testAMBF
        testAMBF.fileOperationStatus = AbstractBamFile.FileOperationStatus.NEEDS_PROCESSING

        expect:
        TestCase.assertAtLeastExpectedValidateError(testAMWP, "bamFileInProjectFolder", "validator.invalid", testAMBF)
    }

    void "test constraint bamFileInProjectFolder, when valid"() {
        given:
        testAMBF.workPackage = testAMWP
        testAMWP.bamFileInProjectFolder = testAMBF
        testAMBF.fileOperationStatus = AbstractBamFile.FileOperationStatus.PROCESSED

        when:
        testAMWP.validate()

        then:
        noExceptionThrown()
    }

    void "constraint for antibodyTarget, when seqType is not chip seq and antibodyTarget is not set, then validate should not create errors"() {
        when:
        AbstractMergingWorkPackage workPackage = createTestAbstractMergingWorkPackage([
                seqType       : DomainFactory.createSeqType(),
                antibodyTarget: null,
        ])
        workPackage.validate()

        then:
        workPackage.errors.errorCount == 0
    }

    void "constraint for antibodyTarget, when seqType is chip seq and antibodyTarget is set, then validate should not create errors"() {
        when:
        AbstractMergingWorkPackage workPackage = createTestAbstractMergingWorkPackage([
                seqType       : DomainFactory.createChipSeqType(),
                antibodyTarget: DomainFactory.createAntibodyTarget(),
        ])
        workPackage.validate()

        then:
        workPackage.errors.errorCount == 0
    }

    void "constraint for antibodyTarget, when seqType is chip seq and antibodyTarget is not set, then validate should create errors"() {
        when:
        SeqType seqtype = DomainFactory.createChipSeqType()
        AbstractMergingWorkPackage workPackage = createTestAbstractMergingWorkPackage([
                seqType       : seqtype,
                antibodyTarget: null,
        ])

        then:
        TestCase.assertValidateError(workPackage, 'antibodyTarget', "required", workPackage.antibodyTarget)
    }

    void "constraint for antibodyTarget, when seqType is not chip seq and antibodyTarget is set, then validate should create errors"() {
        when:
        SeqType seqType = DomainFactory.createSeqType()
        AbstractMergingWorkPackage workPackage = createTestAbstractMergingWorkPackage([
                seqType       : seqType,
                antibodyTarget: DomainFactory.createAntibodyTarget(),
        ])

        then:
        TestCase.assertValidateError(workPackage, 'antibodyTarget', "not.allowed", workPackage.antibodyTarget)
    }

    TestAbstractMergingWorkPackage createTestAbstractMergingWorkPackage(Map properties) {
        return new TestAbstractMergingWorkPackage([
                sample:                new Sample(),
                referenceGenome:       new ReferenceGenome(),
                pipeline:              DomainFactory.createPanCanPipeline(),
        ] + properties)
    }
}
