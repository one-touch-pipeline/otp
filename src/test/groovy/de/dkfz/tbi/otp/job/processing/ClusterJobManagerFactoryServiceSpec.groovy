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
package de.dkfz.tbi.otp.job.processing

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.roddy.execution.jobs.BatchEuphoriaJobManager
import de.dkfz.roddy.execution.jobs.cluster.lsf.LSFJobManager
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSJobManager
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm

class ClusterJobManagerFactoryServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ProcessingOption,
                Realm,
        ]
    }

    final static String SSH_USER = "user"

    void "test getJobManager, get correct manager"(Realm.JobScheduler type, Class managerClass) {
        given:
        ClusterJobManagerFactoryService service = new ClusterJobManagerFactoryService()
        service.configService = Mock(ConfigService) {
            getSshUser() >> SSH_USER
        }
        Realm realm = DomainFactory.createRealm(
                jobScheduler: type,
        )

        when:
        BatchEuphoriaJobManager manager = service.getJobManager(realm)

        then:
        manager.class == managerClass

        where:
        type                   || managerClass
        Realm.JobScheduler.LSF || LSFJobManager
        Realm.JobScheduler.PBS || PBSJobManager
    }

    void "test getJobManager, get the different manager for the different realm"() {
        given:
        ClusterJobManagerFactoryService service = new ClusterJobManagerFactoryService()
        service.configService = Mock(ConfigService) {
            getSshUser() >> SSH_USER
        }
        Realm realm = DomainFactory.createRealm(
                jobScheduler: Realm.JobScheduler.LSF,
        )
        BatchEuphoriaJobManager manager = service.getJobManager(realm)

        Realm realm2 = DomainFactory.createRealm(
                jobScheduler: Realm.JobScheduler.LSF,
        )

        expect:
        manager != service.getJobManager(realm2)
    }

    void "test getJobManager, get the same manager for the same realm"() {
        given:
        ClusterJobManagerFactoryService service = new ClusterJobManagerFactoryService()
        service.configService = Mock(ConfigService) {
            getSshUser() >> SSH_USER
        }
        Realm realm = DomainFactory.createRealm(
                jobScheduler: Realm.JobScheduler.LSF,
        )
        BatchEuphoriaJobManager manager = service.getJobManager(realm)

        expect:
        manager == service.getJobManager(realm)
    }
}
