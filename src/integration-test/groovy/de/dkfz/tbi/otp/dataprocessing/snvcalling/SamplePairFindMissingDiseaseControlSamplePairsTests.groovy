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

package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.otp.dataprocessing.AbstractMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Rollback
@Integration
class SamplePairFindMissingDiseaseControlSamplePairsTests {

    SeqType wholeGenome
    SeqType exome
    SeqType rna

    Project project
    SampleType diseaseSampleType
    SampleType controlSampleType
    SampleTypePerProject diseaseStpp
    SampleTypePerProject controlStpp
    Individual individual
    Sample diseaseSample
    Sample controlSample
    MergingWorkPackage diseaseMwp
    MergingWorkPackage controlMwp

    void setupData() {
        wholeGenome = DomainFactory.createWholeGenomeSeqType()
        exome = DomainFactory.createExomeSeqType()
        rna = DomainFactory.createRnaPairedSeqType()
        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit()

        project = DomainFactory.createProject()
        diseaseSampleType = DomainFactory.createSampleType()
        controlSampleType = DomainFactory.createSampleType()
        diseaseStpp = DomainFactory.createSampleTypePerProject(project: project, sampleType: diseaseSampleType, category: SampleType.Category.DISEASE)
        controlStpp = DomainFactory.createSampleTypePerProject(project: project, sampleType: controlSampleType, category: SampleType.Category.CONTROL)
        individual = DomainFactory.createIndividual(project: project)
        diseaseSample = DomainFactory.createSample(individual: individual, sampleType: diseaseSampleType)
        controlSample = DomainFactory.createSample(individual: individual, sampleType: controlSampleType)
        diseaseMwp = DomainFactory.createMergingWorkPackage(sample: diseaseSample, seqType: wholeGenome, libraryPreparationKit: libraryPreparationKit)
        controlMwp = DomainFactory.createMergingWorkPackage(diseaseMwp, controlSample)
    }

    @Test
    void testMatch() {
        setupData()
        assertFindsOne(diseaseMwp, controlMwp)
    }

    @Test
    void testDiseaseSeqTypeMismatch() {
        setupData()
        diseaseMwp.seqType = exome
        assert diseaseMwp.save(flush: true)
        assertFindsNothing()

        assertFindsOne(DomainFactory.createMergingWorkPackage(controlMwp, diseaseSample), controlMwp)
    }

    @Test
    void testControlSeqTypeMismatch() {
        setupData()
        controlMwp.seqType = exome
        assert controlMwp.save(flush: true)
        assertFindsNothing()

        assertFindsOne(diseaseMwp, DomainFactory.createMergingWorkPackage(diseaseMwp, controlSample))
    }

    @Test
    void testNotAnalysableSeqType() {
        setupData()
        diseaseMwp.seqType = rna
        assert diseaseMwp.save(flush: true)
        controlMwp.seqType = rna
        assert controlMwp.save(flush: true)

        assertFindsNothing()
    }

    @Test
    void testDiseaseIndividualMismatch() {
        setupData()
        diseaseSample.individual = DomainFactory.createIndividual(project: project)
        assert diseaseSample.save(flush: true)
        assertFindsNothing()

        Sample matchingDiseaseSample = DomainFactory.createSample(individual: individual, sampleType: diseaseSampleType)
        MergingWorkPackage matchingDiseaseMwp = DomainFactory.createMergingWorkPackage(controlMwp, matchingDiseaseSample)

        assertFindsOne(matchingDiseaseMwp, controlMwp)
    }

    @Test
    void testControlIndividualMismatch() {
        setupData()
        controlSample.individual = DomainFactory.createIndividual(project: project)
        assert controlSample.save(flush: true)
        assertFindsNothing()

        Sample matchingControlSample = DomainFactory.createSample(individual: individual, sampleType: controlSampleType)
        MergingWorkPackage matchingControlMwp = DomainFactory.createMergingWorkPackage(diseaseMwp, matchingControlSample)

        assertFindsOne(diseaseMwp, matchingControlMwp)
    }

    @Test
    void testDiseaseLibPrepKitMismatch_WGS() {
        setupData()
        diseaseMwp.libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        diseaseMwp.save(flush: true)

        assertFindsOne()
    }

    @Test
    void testDiseaseLibPrepKitMismatch_Exome() {
        setupData()
        diseaseMwp.libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        diseaseMwp.seqType = exome
        diseaseMwp.save(flush: true)

        controlMwp.seqType = exome
        controlMwp.save(flush: true)

        assertFindsOne()
    }

    @Test
    void testDiseaseLibPrepKitMismatch_LibPrepKitNull() {
        setupData()
        diseaseMwp.libraryPreparationKit = null
        diseaseMwp.save(flush: true)

        assertFindsOne()
    }

    @Test
    void testDiseaseStppMissing() {
        setupData()
        diseaseStpp.delete(flush: true)
        assertFindsNothing()
    }

    @Test
    void testControlStppMissing() {
        setupData()
        controlStpp.delete(flush: true)
        assertFindsNothing()
    }

    @Test
    void testDiseaseStppProjectMismatch() {
        setupData()
        diseaseStpp.project = DomainFactory.createProject()
        assert diseaseStpp.save(flush: true)
        assertFindsNothing()

        DomainFactory.createSampleTypePerProject(project: project, sampleType: diseaseSampleType, category: SampleType.Category.DISEASE)
        assertFindsOne()
    }

    @Test
    void testControlStppProjectMismatch() {
        setupData()
        controlStpp.project = DomainFactory.createProject()
        assert controlStpp.save(flush: true)
        assertFindsNothing()

        DomainFactory.createSampleTypePerProject(project: project, sampleType: controlSampleType, category: SampleType.Category.CONTROL)
        assertFindsOne()
    }

    @Test
    void testDiseaseStppSampleTypeMismatch() {
        setupData()
        diseaseStpp.sampleType = DomainFactory.createSampleType()
        assert diseaseStpp.save(flush: true)
        assertFindsNothing()

        DomainFactory.createSampleTypePerProject(project: project, sampleType: diseaseSampleType, category: SampleType.Category.DISEASE)
        assertFindsOne()
    }

    @Test
    void testControlStppSampleTypeMismatch() {
        setupData()
        controlStpp.sampleType = DomainFactory.createSampleType()
        assert controlStpp.save(flush: true)
        assertFindsNothing()

        DomainFactory.createSampleTypePerProject(project: project, sampleType: controlSampleType, category: SampleType.Category.CONTROL)
        assertFindsOne()
    }

    @Test
    void testDiseaseStppSampleTypeCategoryIgnored() {
        setupData()
        diseaseStpp.category = SampleType.Category.IGNORED
        assert diseaseStpp.save(flush: true)
        assertFindsNothing()
    }

    @Test
    void testControlStppSampleTypeCategoryIgnored() {
        setupData()
        controlStpp.category = SampleType.Category.IGNORED
        assert controlStpp.save(flush: true)
        assertFindsNothing()
    }

    @Test
    void testBothStppDisease() {
        setupData()
        controlStpp.category = SampleType.Category.DISEASE
        assert controlStpp.save(flush: true)
        assertFindsNothing()
    }

    @Test
    void testBothStppControl() {
        setupData()
        diseaseStpp.category = SampleType.Category.CONTROL
        assert diseaseStpp.save(flush: true)
        assertFindsNothing()
    }

    @Test
    void testMatchingSamplePairAlreadyExists() {
        setupData()
        DomainFactory.createSamplePair(
                diseaseMwp,
                controlMwp,
        )
        assertFindsNothing()
    }

    @Test
    void testSamplePairWithOtherIndividualExists() {
        setupData()
        Individual otherIndividual = DomainFactory.createIndividual(project: project)
        DomainFactory.createSamplePair(
                DomainFactory.createMergingWorkPackage(diseaseMwp,
                        DomainFactory.createSample(individual: otherIndividual, sampleType: diseaseSampleType)),
                DomainFactory.createMergingWorkPackage(controlMwp,
                        DomainFactory.createSample(individual: otherIndividual, sampleType: controlSampleType)),
        )
        assertFindsOne()
    }

    @Test
    void testSamplePairWithOtherSampleType1Exists() {
        setupData()
        final SampleType sampleType1 = DomainFactory.createSampleType()
        DomainFactory.createSampleTypePerProject(project: project, sampleType: sampleType1, category: SampleType.Category.DISEASE)
        DomainFactory.createSamplePair(
                DomainFactory.createMergingWorkPackage(diseaseMwp, sampleType1),
                controlMwp,
        )
        assertFindsOne()
    }

    @Test
    void testSamplePairWithOtherSampleType2Exists() {
        setupData()
        DomainFactory.createSamplePair(
                diseaseMwp,
                DomainFactory.createMergingWorkPackage(diseaseMwp, DomainFactory.createSampleType()),
        )
        assertFindsOne()
    }

    @Test
    void testSamplePairWithOtherSeqTypeExists() {
        setupData()
        DomainFactory.createSamplePair(
                DomainFactory.createMergingWorkPackage(diseaseMwp, [seqType: exome]),
                DomainFactory.createMergingWorkPackage(controlMwp, [seqType: exome]),
        )
        assertFindsOne()
    }

    @Test
    void testFindTwo() {
        setupData()
        MergingWorkPackage diseaseExomeMwp = DomainFactory.createMergingWorkPackage(diseaseMwp, [seqType: exome])
        MergingWorkPackage controlExomeMwp = DomainFactory.createMergingWorkPackage(controlMwp, [seqType: exome])
        final List<SamplePair> samplePairs =
                SamplePair.findMissingDiseaseControlSamplePairs().sort { it.seqType.name }
        assert samplePairs.size() == 2
        assertEqualsAndNotPersisted(samplePairs[0], diseaseExomeMwp, controlExomeMwp)
        assertEqualsAndNotPersisted(samplePairs[1], diseaseMwp, controlMwp)
    }

    @Test
    void testFindNoExternalMergingWorkPackage() {
        setupData()
        DomainFactory.createExternalMergingWorkPackage([sample: diseaseMwp.sample, seqType: diseaseMwp.seqType])
        DomainFactory.createExternalMergingWorkPackage([sample: controlMwp.sample, seqType: controlMwp.seqType])

        diseaseMwp.delete(flush: true)
        controlMwp.delete(flush: true)

        assert AbstractMergingWorkPackage.list().size() == 2

        assertFindsNothing()
    }

    void assertFindsNothing() {
        assert SamplePair.findMissingDiseaseControlSamplePairs().empty
    }

    void assertFindsOne(final MergingWorkPackage mwp1 = diseaseMwp, final MergingWorkPackage mwp2 = controlMwp) {
        assertEqualsAndNotPersisted(
                exactlyOneElement(SamplePair.findMissingDiseaseControlSamplePairs()),
                mwp1, mwp2
        )
    }

    void assertEqualsAndNotPersisted(final SamplePair samplePair, final MergingWorkPackage mwp1, final MergingWorkPackage mwp2) {
        assert samplePair.mergingWorkPackage1 == mwp1 &&
               samplePair.mergingWorkPackage2 == mwp2 &&
               samplePair.id                  == null
    }
}
