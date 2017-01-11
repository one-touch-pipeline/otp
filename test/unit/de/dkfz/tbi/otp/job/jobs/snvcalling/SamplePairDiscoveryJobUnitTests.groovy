package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.*
import org.apache.commons.logging.impl.*
import org.junit.*


@Build([SamplePair])
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
        } finally {
            SampleTypePerProject.metaClass = null
        }
    }

    @Test
    void testCreateMissingDiseaseControlSamplePairs() {
        Collection<SamplePair> missingSamplePairs = [new SamplePair()]
        testMethodWhichCallsSetProcessingStatus(missingSamplePairs, {
            SamplePair.metaClass.static.findMissingDiseaseControlSamplePairs = {
                return missingSamplePairs
            }
            job.createMissingDiseaseControlSamplePairs()
        })
    }

    private void testMethodWhichCallsSetProcessingStatus(final Collection expectedSamplePairs, final Closure call) {
        try {
            boolean setProcessingStatusCalled = false
            SamplePair.metaClass.static.setSnvProcessingStatus = {
                final Collection<SamplePair> samplePairs, final ProcessingStatus processingStatus ->
                    assert samplePairs.is(expectedSamplePairs)
                    assert processingStatus == ProcessingStatus.NEEDS_PROCESSING
                    setProcessingStatusCalled = true
            }
            call()
            assert setProcessingStatusCalled
        } finally {
            SamplePair.metaClass = null
        }
    }

    @Test
    void testExecute_sampleTypesNeedCategorization() {
        boolean logUncategorizedSampleTypesCalled = false
        testExecute {
            job.metaClass.logUncategorizedSampleTypes = { logUncategorizedSampleTypesCalled = true; return true }

            job.execute()
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
        boolean createMissingDiseaseControlSamplePairsCalled = false
        job.metaClass.createMissingDiseaseControlSamplePairs = { createMissingDiseaseControlSamplePairsCalled = true }

        code()

        assert createMissingDiseaseControlSamplePairsCalled == true
    }
}
