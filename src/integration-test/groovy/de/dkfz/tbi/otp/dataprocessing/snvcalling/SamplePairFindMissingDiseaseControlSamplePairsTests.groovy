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

    @Before
    void before() {
        wholeGenome = DomainFactory.createWholeGenomeSeqType()
        exome = DomainFactory.createExomeSeqType()
        rna = DomainFactory.createRnaPairedSeqType()
        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit()

        project = Project.build()
        diseaseSampleType = SampleType.build()
        controlSampleType = SampleType.build()
        diseaseStpp = SampleTypePerProject.build(project: project, sampleType: diseaseSampleType, category: SampleType.Category.DISEASE)
        controlStpp = SampleTypePerProject.build(project: project, sampleType: controlSampleType, category: SampleType.Category.CONTROL)
        individual = Individual.build(project: project)
        diseaseSample = Sample.build(individual: individual, sampleType: diseaseSampleType)
        controlSample = Sample.build(individual: individual, sampleType: controlSampleType)
        diseaseMwp = DomainFactory.createMergingWorkPackage(sample: diseaseSample, seqType: wholeGenome, libraryPreparationKit: libraryPreparationKit)
        controlMwp = DomainFactory.createMergingWorkPackage(diseaseMwp, controlSample)
    }

    @Test
    void testMatch() {
        assertFindsOne(diseaseMwp, controlMwp)
    }

    @Test
    void testDiseaseSeqTypeMismatch() {
        diseaseMwp.seqType = exome
        assert diseaseMwp.save(flush: true)
        assertFindsNothing()

        assertFindsOne(DomainFactory.createMergingWorkPackage(controlMwp, diseaseSample), controlMwp)
    }

    @Test
    void testControlSeqTypeMismatch() {
        controlMwp.seqType = exome
        assert controlMwp.save(flush: true)
        assertFindsNothing()

        assertFindsOne(diseaseMwp, DomainFactory.createMergingWorkPackage(diseaseMwp, controlSample))
    }

    @Test
    void testNotAnalysableSeqType() {
        diseaseMwp.seqType = rna
        assert diseaseMwp.save(flush: true)
        controlMwp.seqType = rna
        assert controlMwp.save(flush: true)

        assertFindsNothing()
    }

    @Test
    void testDiseaseIndividualMismatch() {
        diseaseSample.individual = Individual.build(project: project)
        assert diseaseSample.save(flush: true)
        assertFindsNothing()

        Sample matchingDiseaseSample = DomainFactory.createSample(individual: individual, sampleType: diseaseSampleType)
        MergingWorkPackage matchingDiseaseMwp = DomainFactory.createMergingWorkPackage(controlMwp, matchingDiseaseSample)

        assertFindsOne(matchingDiseaseMwp, controlMwp)
    }

    @Test
    void testControlIndividualMismatch() {
        controlSample.individual = Individual.build(project: project)
        assert controlSample.save(flush: true)
        assertFindsNothing()

        Sample matchingControlSample = DomainFactory.createSample(individual: individual, sampleType: controlSampleType)
        MergingWorkPackage matchingControlMwp = DomainFactory.createMergingWorkPackage(diseaseMwp, matchingControlSample)

        assertFindsOne(diseaseMwp, matchingControlMwp)
    }

    @Test
    void testDiseaseLibPrepKitMismatch_WGS() {
        diseaseMwp.libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        diseaseMwp.save(flush: true)

        assertFindsOne()
    }

    @Test
    void testDiseaseLibPrepKitMismatch_Exome() {
        diseaseMwp.libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        diseaseMwp.seqType = exome
        diseaseMwp.save(flush: true)

        controlMwp.seqType = exome
        controlMwp.save(flush: true)

        assertFindsOne()
    }

    @Test
    void testDiseaseLibPrepKitMismatch_LibPrepKitNull() {
        diseaseMwp.libraryPreparationKit = null
        diseaseMwp.save(flush: true)

        assertFindsOne()
    }

    @Test
    void testDiseaseStppMissing() {
        diseaseStpp.delete(flush: true)
        assertFindsNothing()
    }

    @Test
    void testControlStppMissing() {
        controlStpp.delete(flush: true)
        assertFindsNothing()
    }

    @Test
    void testDiseaseStppProjectMismatch() {
        diseaseStpp.project = Project.build()
        assert diseaseStpp.save(flush: true)
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: diseaseSampleType, category: SampleType.Category.DISEASE)
        assertFindsOne()
    }

    @Test
    void testControlStppProjectMismatch() {
        controlStpp.project = Project.build()
        assert controlStpp.save(flush: true)
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: controlSampleType, category: SampleType.Category.CONTROL)
        assertFindsOne()
    }

    @Test
    void testDiseaseStppSampleTypeMismatch() {
        diseaseStpp.sampleType = SampleType.build()
        assert diseaseStpp.save(flush: true)
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: diseaseSampleType, category: SampleType.Category.DISEASE)
        assertFindsOne()
    }

    @Test
    void testControlStppSampleTypeMismatch() {
        controlStpp.sampleType = SampleType.build()
        assert controlStpp.save(flush: true)
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: controlSampleType, category: SampleType.Category.CONTROL)
        assertFindsOne()
    }

    @Test
    void testDiseaseStppSampleTypeCategoryIgnored() {
        diseaseStpp.category = SampleType.Category.IGNORED
        assert diseaseStpp.save(flush: true)
        assertFindsNothing()
    }

    @Test
    void testControlStppSampleTypeCategoryIgnored() {
        controlStpp.category = SampleType.Category.IGNORED
        assert controlStpp.save(flush: true)
        assertFindsNothing()
    }

    @Test
    void testBothStppDisease() {
        controlStpp.category = SampleType.Category.DISEASE
        assert controlStpp.save(flush: true)
        assertFindsNothing()
    }

    @Test
    void testBothStppControl() {
        diseaseStpp.category = SampleType.Category.CONTROL
        assert diseaseStpp.save(flush: true)
        assertFindsNothing()
    }

    @Test
    void testMatchingSamplePairAlreadyExists() {
        DomainFactory.createSamplePair(
                diseaseMwp,
                controlMwp,
        )
        assertFindsNothing()
    }

    @Test
    void testSamplePairWithOtherIndividualExists() {
        Individual otherIndividual = Individual.build(project: project)
        DomainFactory.createSamplePair(
                DomainFactory.createMergingWorkPackage(diseaseMwp,
                        Sample.build(individual: otherIndividual, sampleType: diseaseSampleType)),
                DomainFactory.createMergingWorkPackage(controlMwp,
                        Sample.build(individual: otherIndividual, sampleType: controlSampleType)),
        )
        assertFindsOne()
    }

    @Test
    void testSamplePairWithOtherSampleType1Exists() {
        final SampleType sampleType1 = SampleType.build()
        SampleTypePerProject.build(project: project, sampleType: sampleType1, category: SampleType.Category.DISEASE)
        DomainFactory.createSamplePair(
                DomainFactory.createMergingWorkPackage(diseaseMwp, sampleType1),
                controlMwp,
        )
        assertFindsOne()
    }

    @Test
    void testSamplePairWithOtherSampleType2Exists() {
        DomainFactory.createSamplePair(
                diseaseMwp,
                DomainFactory.createMergingWorkPackage(diseaseMwp, SampleType.build()),
        )
        assertFindsOne()
    }

    @Test
    void testSamplePairWithOtherSeqTypeExists() {
        DomainFactory.createSamplePair(
                DomainFactory.createMergingWorkPackage(diseaseMwp, [seqType: exome]),
                DomainFactory.createMergingWorkPackage(controlMwp, [seqType: exome]),
        )
        assertFindsOne()
    }

    @Test
    void testFindTwo() {
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

    void assertFindsOne(final MergingWorkPackage mergingWorkPackage1 = diseaseMwp,
                        final MergingWorkPackage mergingWorkPackage2 = controlMwp) {
        assertEqualsAndNotPersisted(
                exactlyOneElement(SamplePair.findMissingDiseaseControlSamplePairs()),
                mergingWorkPackage1, mergingWorkPackage2)
    }

    void assertEqualsAndNotPersisted(final SamplePair samplePair,
                                     final MergingWorkPackage mergingWorkPackage1,
                                     final MergingWorkPackage mergingWorkPackage2) {
        assert samplePair.mergingWorkPackage1 == mergingWorkPackage1 &&
               samplePair.mergingWorkPackage2 == mergingWorkPackage2 &&
               samplePair.id                  == null
    }
}
