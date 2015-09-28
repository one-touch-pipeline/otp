package de.dkfz.tbi.otp.ngsdata

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.ngsdata.*


class SamplePairServiceTests extends AbstractIntegrationTest {

    SamplePairService samplePairService


    @Before
    void setUp() {
        createUserAndRoles()
    }

    @Test
    void "test samplePairsBySnvProcessingState noData"() {
        Individual individual = Individual.build()

        SpringSecurityUtils.doWithAuth("admin") {
            def map = samplePairService.samplePairsBySnvProcessingState(individual)
            def expected = [
                    finished  : [],
                    inprogress: [],
                    notStarted: [],
                    disabled  : []
            ]
            assert expected == map
        }
    }

    @Test
    void "test samplePairsBySnvProcessingState notStarted"() {
        SamplePair samplePair = DomainFactory.createSamplePair(processingStatus: SamplePair.ProcessingStatus.NEEDS_PROCESSING)

        SpringSecurityUtils.doWithAuth("admin") {
            Map map = samplePairService.samplePairsBySnvProcessingState(samplePair.individual)
            def expected = [
                    finished  : [],
                    inprogress: [],
                    notStarted: [samplePair],
                    disabled  : []
            ]
            assert expected == map
        }
    }

    @Test
    void "test samplePairsBySnvProcessingState disable"() {
        SamplePair samplePair = DomainFactory.createSamplePair(processingStatus: SamplePair.ProcessingStatus.DISABLED)
        SpringSecurityUtils.doWithAuth("admin") {
            Map map = samplePairService.samplePairsBySnvProcessingState(samplePair.individual)
            def expected = [
                    disabled  : [samplePair],
                    finished  : [],
                    inprogress: [],
                    notStarted: []
            ]
            assert expected == map
        }
    }

    @Test
    void "test samplePairsBySnvProcessingState finished"() {
        SnvCallingInstance snvCallingInstance1 = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        SpringSecurityUtils.doWithAuth("admin") {
            Map map = samplePairService.samplePairsBySnvProcessingState(snvCallingInstance1.individual)
            def expected = [
                    finished  : [snvCallingInstance1.samplePair],
                    inprogress: [],
                    notStarted: [snvCallingInstance1.samplePair],
                    disabled  : []

            ]
            assert expected == map
        }
    }

    @Test
    void "test samplePairsBySnvProcessingState inProgress"() {
        SnvCallingInstance snvCallingInstance2 = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.IN_PROGRESS)

        SpringSecurityUtils.doWithAuth("admin") {
            Map<String, List<SamplePair>> map = samplePairService.samplePairsBySnvProcessingState(snvCallingInstance2.individual)
            def expected = [
                    finished  : [],
                    inprogress: [snvCallingInstance2.samplePair],
                    notStarted: [snvCallingInstance2.samplePair],
                    disabled  : []

            ]
            assert expected == map
        }
    }
}