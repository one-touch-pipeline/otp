package de.dkfz.tbi.otp.job.jobs.snvcalling

import org.apache.commons.logging.impl.NoOpLog
import org.joda.time.LocalDate
import org.junit.Before
import org.junit.Test
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual.ProcessingStatus
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
    void testSetExistingCombinationsToNeedsProcessing() {
        Collection<SampleTypeCombinationPerIndividual> theCombinations = [new SampleTypeCombinationPerIndividual()]
        testMethodWhichCallsSetProcessingStatus(theCombinations, {
            SampleTypeCombinationPerIndividual.metaClass.static.findCombinationsForSettingNeedsProcessing = {
                return theCombinations
            }
            job.setExistingCombinationsToNeedsProcessing()
        })
    }

    @Test
    void testCreateMissingDiseaseControlCombinations() {
        Collection<SampleTypeCombinationPerIndividual> missingCombinations = [new SampleTypeCombinationPerIndividual()]
        testMethodWhichCallsSetProcessingStatus(missingCombinations, {
            SampleTypeCombinationPerIndividual.metaClass.static.findMissingDiseaseControlCombinations = { final Date minDate ->
                assert minDate == new LocalDate(2014, 12, 1).toDate()
                return missingCombinations
            }
            job.createMissingDiseaseControlCombinations()
        })
    }

    private void testMethodWhichCallsSetProcessingStatus(final Collection expectedCombinations, final Closure call) {
        try {
            boolean setProcessingStatusCalled = false
            SampleTypeCombinationPerIndividual.metaClass.static.setProcessingStatus = {
                final Collection<SampleTypeCombinationPerIndividual> combinations, final ProcessingStatus processingStatus ->
                    assert combinations.is(expectedCombinations)
                    assert processingStatus == ProcessingStatus.NEEDS_PROCESSING
                    setProcessingStatusCalled = true
            }
            call()
            assert setProcessingStatusCalled
        } finally {
            SampleTypeCombinationPerIndividual.metaClass = null
        }
    }

    @Test
    void testExecute_sampleTypesNeedCategorization() {
        boolean logUncategorizedSampleTypesCalled = false
        testExecute {
            job.metaClass.logUncategorizedSampleTypes = { logUncategorizedSampleTypesCalled = true; return true }

            assert shouldFail { job.execute() } == 'Some sample types are not categorized. See the job log for details.'
        }
        assert logUncategorizedSampleTypesCalled == true
    }

    @Test
    void testExecute_noSampleTypeNeedsCategorization() {
        boolean logUncategorizedSampleTypesCalled = false
        testExecute {
            job.metaClass.logUncategorizedSampleTypes = { logUncategorizedSampleTypesCalled = true; return false }

            job.start()
            job.execute()
            job.end()

            assert job.endState == ExecutionState.SUCCESS
        }
        assert logUncategorizedSampleTypesCalled == true
    }

    private void testExecute(final Closure code) {
        boolean setExistingCombinationsToNeedsProcessingCalled = false
        job.metaClass.setExistingCombinationsToNeedsProcessing = { setExistingCombinationsToNeedsProcessingCalled = true }
        boolean createMissingDiseaseControlCombinationsCalled = false
        job.metaClass.createMissingDiseaseControlCombinations = { createMissingDiseaseControlCombinationsCalled = true }

        code()

        assert setExistingCombinationsToNeedsProcessingCalled == true
        assert createMissingDiseaseControlCombinationsCalled == true
    }
}
