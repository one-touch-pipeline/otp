package de.dkfz.tbi.otp.job.jobs.snvcalling

import org.apache.commons.logging.impl.NoOpLog
import org.junit.Before
import org.junit.Test
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject
import grails.buildtestdata.mixin.Build

@Build([SampleTypeCombinationPerIndividual])
class SamplePairDiscoveryJobUnitTests {

    SamplePairDiscoveryJob job

    @Before
    void before() {
        job = new SamplePairDiscoveryJob()
        job.log = new NoOpLog()
    }

    @Test
    void testUncategorizedSampleTypes_noResults() {
        try {
            SampleTypePerProject.metaClass.static.findMissingCombinations = { return Collections.emptyList() }

            assert SamplePairDiscoveryJob.findUncategorizedSampleTypes() == null
            assert !job.logUncategorizedSampleTypes()
        } finally {
            SampleTypePerProject.metaClass = null
        }
    }

    @Test
    void testUncategorizedSampleTypes_someResults() {
        final Project projectB = Project.build(name: 'ProjectB')
        final Project projectA = Project.build(name: 'ProjectA')
        final SampleType tumor = SampleType.build(name: 'TUMOR')
        final SampleType control = SampleType.build(name: 'CONTROL')
        try {
            SampleTypePerProject.metaClass.static.findMissingCombinations = {
                return [[projectB, tumor], [projectA, tumor], [projectB, control]]
            }
            final String expected =
                    "${projectA.name}\n" +
                    "  ${tumor.name}\n" +
                    "${projectB.name}\n" +
                    "  ${control.name}\n" +
                    "  ${tumor.name}\n"

            assert SamplePairDiscoveryJob.findUncategorizedSampleTypes().toString() == expected
            assert job.logUncategorizedSampleTypes()
        } finally {
            SampleTypePerProject.metaClass = null
        }
    }

    @Test
    void testCreateMissingDiseaseControlCombinations() {

        Collection<SampleTypeCombinationPerIndividual> missingCombinations = [new SampleTypeCombinationPerIndividual()]
        try {
            SampleTypeCombinationPerIndividual.metaClass.static.findMissingDiseaseControlCombinations = {
                return missingCombinations
            }
            boolean setNeedsProcessingCalled = false
            SampleTypeCombinationPerIndividual.metaClass.static.setNeedsProcessing = {
                final Collection<SampleTypeCombinationPerIndividual> combinations, final boolean needsProcessing ->
                    assert combinations.is(missingCombinations)
                    assert needsProcessing
                    setNeedsProcessingCalled = true
            }

            job.createMissingDiseaseControlCombinations()

            assert setNeedsProcessingCalled
        } finally {
            SampleTypeCombinationPerIndividual.metaClass = null
        }
    }

    @Test
    void testExecute_sampleTypesNeedCategorization() {
        boolean logUncategorizedSampleTypesCalled = false
        boolean createMissingDiseaseControlCombinationsCalled = false
        job.metaClass.logUncategorizedSampleTypes = { logUncategorizedSampleTypesCalled = true; return true }
        job.metaClass.createMissingDiseaseControlCombinations = { createMissingDiseaseControlCombinationsCalled = true }

        assert shouldFail { job.execute() } == 'Some sample types are not categorized. See the job log for details.'

        assert logUncategorizedSampleTypesCalled == true
        assert createMissingDiseaseControlCombinationsCalled == true
    }

    @Test
    void testExecute_noSampleTypeNeedsCategorization() {
        boolean logUncategorizedSampleTypesCalled = false
        boolean createMissingDiseaseControlCombinationsCalled = false
        job.metaClass.logUncategorizedSampleTypes = { logUncategorizedSampleTypesCalled = true; return false }
        job.metaClass.createMissingDiseaseControlCombinations = { createMissingDiseaseControlCombinationsCalled = true }

        job.start()
        job.execute()
        job.end()

        assert job.endState == ExecutionState.SUCCESS
        assert logUncategorizedSampleTypesCalled == true
        assert createMissingDiseaseControlCombinationsCalled == true
    }
}
