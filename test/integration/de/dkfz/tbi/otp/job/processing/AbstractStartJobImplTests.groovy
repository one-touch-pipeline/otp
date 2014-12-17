package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.utils.HelperUtils
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.job.jobs.TestStartJob2
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import de.dkfz.tbi.otp.testing.TestStartJob

class AbstractStartJobImplTests extends AbstractIntegrationTest {

    @Autowired
    ProcessingOptionService processingOptionService

    @Autowired
    TestStartJob testStartJob

    @Autowired
    TestStartJob2 testStartJob2

    private static int SLOT_COUNT_1 = 123
    private static int SLOT_COUNT_2 = 456

    @Test
    void testGetConfiguredSlotCount_notConfigured() {
        final String optionName = HelperUtils.uniqueString
        final JobExecutionPlan plan = JobExecutionPlan.build()
        assert testStartJob.getConfiguredSlotCount(plan, optionName, SLOT_COUNT_1) == SLOT_COUNT_1
    }

    @Test
    void testGetConfiguredSlotCount_properlyConfigured() {
        final String optionName = "option${HelperUtils.uniqueString}"
        final JobExecutionPlan plan = JobExecutionPlan.build()
        createUserAndRoles()
        SpringSecurityUtils.doWithAuth('operator') {
            processingOptionService.createOrUpdate(optionName, plan.name, null, Integer.toString(SLOT_COUNT_2), '')
        }
        assert testStartJob.getConfiguredSlotCount(plan, optionName, SLOT_COUNT_1) == SLOT_COUNT_2
    }

    @Test
    void testGetConfiguredSlotCount_notANumber() {
        final String optionName = "option${HelperUtils.uniqueString}"
        final JobExecutionPlan plan = JobExecutionPlan.build()
        createUserAndRoles()
        SpringSecurityUtils.doWithAuth('operator') {
            processingOptionService.createOrUpdate(optionName, plan.name, null, 'twelve', '')
        }
        shouldFail NumberFormatException, {
            testStartJob.getConfiguredSlotCount(plan, optionName, SLOT_COUNT_1)
        }
    }

    @Test
    void testGetConfiguredSlotCount_negative() {
        final String optionName = "option${HelperUtils.uniqueString}"
        final JobExecutionPlan plan = JobExecutionPlan.build()
        createUserAndRoles()
        SpringSecurityUtils.doWithAuth('operator') {
            processingOptionService.createOrUpdate(optionName, plan.name, null, '-1', '')
        }
        shouldFail NumberFormatException, {
            testStartJob.getConfiguredSlotCount(plan, optionName, SLOT_COUNT_1)
        }
    }

    @Test
    void testGetMinimumProcessingPriorityForOccupyingASlot_noJep() {
        testStartJob2.jep = null
        assert testStartJob2.minimumProcessingPriorityForOccupyingASlot == ProcessingPriority.SUPREMUM_PRIORITY
    }

    @Test
    void testGetMinimumProcessingPriorityForOccupyingASlot_obsoleteJep() {
        testStartJob2.jep = JobExecutionPlan.build(obsoleted: true, enabled: true)
        assert testStartJob2.minimumProcessingPriorityForOccupyingASlot == ProcessingPriority.SUPREMUM_PRIORITY
    }

    @Test
    void testGetMinimumProcessingPriorityForOccupyingASlot_disabledJep() {
        testStartJob2.jep = JobExecutionPlan.build(obsoleted: false, enabled: false)
        assert testStartJob2.minimumProcessingPriorityForOccupyingASlot == ProcessingPriority.SUPREMUM_PRIORITY
    }

    @Test
    void testGetMinimumProcessingPriorityForOccupyingASlot_allSlotsFull() {
        prepareTestGetMinimumProcessingPriorityForOccupyingASlot(3)
        assert testStartJob2.minimumProcessingPriorityForOccupyingASlot == ProcessingPriority.SUPREMUM_PRIORITY
    }

    @Test
    void testGetMinimumProcessingPriorityForOccupyingASlot_onlyFastTrackSlotsFree() {
        prepareTestGetMinimumProcessingPriorityForOccupyingASlot(2)
        assert testStartJob2.minimumProcessingPriorityForOccupyingASlot == ProcessingPriority.FAST_TRACK_PRIORITY
    }

    @Test
    void testGetMinimumProcessingPriorityForOccupyingASlot_slotsFree() {
        prepareTestGetMinimumProcessingPriorityForOccupyingASlot(1)
        assert testStartJob2.minimumProcessingPriorityForOccupyingASlot == ProcessingPriority.NORMAL_PRIORITY
    }

    private void prepareTestGetMinimumProcessingPriorityForOccupyingASlot(final int runningProcesses) {
        final JobExecutionPlan jep = JobExecutionPlan.build(enabled: true)
        3.times {
            Process.build(jobExecutionPlan: jep, finished: true)
        }
        runningProcesses.times {
            Process.build(jobExecutionPlan: jep)
        }
        testStartJob2.jep = jep
        testStartJob2.totalSlots = 3
        testStartJob2.slotsReservedForFastTrack = 1
    }
}
