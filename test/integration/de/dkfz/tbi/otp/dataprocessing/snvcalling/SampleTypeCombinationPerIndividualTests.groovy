package de.dkfz.tbi.otp.dataprocessing.snvcalling

import static org.junit.Assert.*
import grails.validation.ValidationException
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.Individual.Type

class SampleTypeCombinationPerIndividualTests {

    Individual individual
    SampleType sampleType1
    SampleType sampleType2

    @Before
    void setUp() {
        Project project = new Project(
                        name: "project",
                        dirName: "/dirName/",
                        realmName: "DKFZ",
                        )
        project.save()

        individual = new Individual(
                        project: project,
                        pid: "pid",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Type.REAL
                        )
        individual.save()

        sampleType1 = new SampleType(
                        name: "BLOOD"
                        )
        sampleType1.save()

        sampleType2 = new SampleType(
                        name: "CONTROL"
                        )
        sampleType2.save()
    }

    @After
    void tearDown() {
        individual = null
        sampleType1 = null
        sampleType2 = null
    }

    @Test
    void testAllCorrect() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
                        individual: individual,
                        sampleType1: sampleType1,
                        sampleType2: sampleType2
                        )
        sampleCombinationPerIndividual.save()
    }

    @Test(expected= ValidationException)
    void testPairsWithSameSampleTypes() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
                        individual: individual,
                        sampleType1: sampleType1,
                        sampleType2: sampleType1
                        )
        sampleCombinationPerIndividual.save()
    }

    @Test(expected= ValidationException)
    void testPairsUniquePerIndividual() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual1 = new SampleTypeCombinationPerIndividual(
                        individual: individual,
                        sampleType1: sampleType1,
                        sampleType2: sampleType2
                        )
        sampleCombinationPerIndividual1.save()
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual2 = new SampleTypeCombinationPerIndividual(
                        individual: individual,
                        sampleType1: sampleType1,
                        sampleType2: sampleType2
                        )
        sampleCombinationPerIndividual2.save()
    }

    @Test(expected= ValidationException)
    void testPairsUniquePerIndividualInBothDirections() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual1 = new SampleTypeCombinationPerIndividual(
                        individual: individual,
                        sampleType1: sampleType1,
                        sampleType2: sampleType2
                        )
        sampleCombinationPerIndividual1.save()
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual2 = new SampleTypeCombinationPerIndividual(
                        individual: individual,
                        sampleType1: sampleType2,
                        sampleType2: sampleType1
                        )
        sampleCombinationPerIndividual2.save()
    }

}
