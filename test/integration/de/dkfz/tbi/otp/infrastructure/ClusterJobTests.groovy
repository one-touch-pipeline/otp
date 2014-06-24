package de.dkfz.tbi.otp.infrastructure

import static org.junit.Assert.*

import org.junit.*

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm

class ClusterJobTests {

    @Test
    public void testFormula () {

        JobExecutionPlan plan = new JobExecutionPlan(name: "testFormula", obsoleted: true, planVersion: 0)

        assertNotNull(plan.save(flush: true))

        Process process = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")

        assertNotNull(process.save(flush: true))

        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "foo", plan: plan)

        assertNotNull(jobDefinition.save(flush: true))

        ProcessingStep step = new ProcessingStep(id: 1, process: process, jobDefinition: jobDefinition)

        assertNotNull(step.save(flush: true))

        Realm realm = DomainFactory.createRealmDataManagementBioQuant()

        assertNotNull(realm.save(flush: true))

        Date queued = new Date()
        Date started = new Date(queued.getTime() + 1 * 24 * 60 * 60 * 1000)
        Date ended = new Date(started.getTime() + 1 * 24 * 60 * 60 * 1000)
        ClusterJob clusterJob = new ClusterJob(
                                                    processingStep: step,
                                                    realm: realm,
                                                    clusterJobId: "testID",
                                                    clusterJobName: "testName",
                                                    queued: queued,
                                                    started: started,
                                                    ended: ended,
                                                    requestedWalltime: 24*60*60*1000,
                                                    requestedCores: 10,
                                                    cpuTime: 12*60*60*1000,
                                                    requestedMemory: 1000,
                                                    usedMemory: 800
                                              )

        assertNotNull(clusterJob.save(flush: true))

        clusterJob.refresh()

        assertEquals(clusterJob.memoryEfficiency, (800 / 1000), 0)
        assertEquals(clusterJob.cpuTimePerCore, ((12 * 60 * 60 * 1000) / 10), 0)
        assertEquals(clusterJob.cpuAvgUtilised, ((12 * 60 * 60 * 1000) / (24 * 60 * 60 * 1000)), 0)
        assertEquals(clusterJob.elapsedWalltime, ((24 * 60 * 60) * 1000), 0)
        assertEquals(clusterJob.walltimeDiff, ((24 * 60 * 60 * 1000) - (24 * 60 * 60 * 1000)), 0)
    }

    public void testNullable () {

        JobExecutionPlan plan = new JobExecutionPlan(name: "testFormula", obsoleted: true, planVersion: 0)

        assertNotNull(plan.save(flush: true))

        Process process = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")

        assertNotNull(process.save(flush: true))

        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "foo", plan: plan)

        assertNotNull(jobDefinition.save(flush: true))

        ProcessingStep step = new ProcessingStep(id: 1, process: process, jobDefinition: jobDefinition)

        assertNotNull(step.save(flush: true))

        Realm realm = DomainFactory.createRealmDataManagementBioQuant()

        assertNotNull(realm.save(flush: true))

        ClusterJob clusterJob = new ClusterJob(
                                                    processingStep: null,
                                                    realm: null,
                                                    clusterJobId: null,
                                                    clusterJobName: null,
                                                    queued: null,
                                                    started: null,
                                                    ended: null,
                                                    requestedWalltime: null,
                                                    requestedCores: null,
                                                    cpuTime: null,
                                                    requestedMemory: null,
                                                    usedMemory: null
                                              )

        assertFalse(clusterJob.validate())

        ClusterJob clusterJob2 = new ClusterJob(
                                                    processingStep: step,
                                                    realm: realm,
                                                    clusterJobId: "testID",
                                                    clusterJobName: "testName",
                                                    queued: new Date(),
                                                    requestedWalltime: 24*60*60,
                                                    requestedCores: 10,
                                                    requestedMemory: 1000,
                                                    exitStatus: null,
                                                    exitCode: null,
                                                    started: null,
                                                    ended: null,
                                                    elapsedWalltime: null,
                                                    walltimeDiff: null,
                                                    cpuTime: null,
                                                    cpuAvgUtilised: null,
                                                    usedMemory: null,
                                                    memoryEfficiency: null
                                              )

        assertTrue(clusterJob2.validate())
    }

}
