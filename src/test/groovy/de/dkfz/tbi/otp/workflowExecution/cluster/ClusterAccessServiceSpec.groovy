/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.cluster

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.processing.ClusterJobManagerFactoryService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class ClusterAccessServiceSpec extends Specification implements ServiceUnitTest<ClusterAccessService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ClusterJob,
                Realm,
                WorkflowStep,
        ]
    }

    void "executeJobs, when multiple scripts given, then all methods of preparation, sending and starting are called and for each a cluster id is returned"() {
        given:
        Realm realm = createRealm()
        WorkflowStep workflowStep = createWorkflowStep()

        BatchEuphoriaJobManager batchEuphoriaJobManager = Mock(BatchEuphoriaJobManager)

        List<String> scripts = (1..5).collect { "script ${nextId}" }
        List<BEJob> beJobs = scripts.collect { new BEJob(new BEJobID("${nextId}"), batchEuphoriaJobManager) }
        List<String> ids = beJobs*.jobID*.shortID

        service.clusterJobManagerFactoryService = Mock(ClusterJobManagerFactoryService) {
            1 * getJobManager(realm) >> batchEuphoriaJobManager
            0 * _
        }
        service.clusterJobHandlingService = Mock(ClusterJobHandlingService) {
            1 * createBeJobsToSend(batchEuphoriaJobManager, realm, workflowStep, scripts, [:]) >> beJobs
            1 * sendJobs(batchEuphoriaJobManager, workflowStep, beJobs)
            1 * startJob(batchEuphoriaJobManager, workflowStep, beJobs)
            1 * collectJobStatistics(realm, workflowStep, beJobs)
            0 * _
        }
        service.logService = Mock(LogService) {
            1 * addSimpleLogEntry(workflowStep, _)
        }

        when:
        List<String> result = service.executeJobs(realm, workflowStep, scripts)

        then:
        result == ids
    }

    void "executeJobs, when no scripts are given, then throw exception"() {
        given:
        Realm realm = createRealm()
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        service.executeJobs(realm, workflowStep, [])

        then:
        thrown(NoScriptsGivenWorkflowException)
    }
}
