package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.roddy.execution.jobs.BatchEuphoriaJobManager
import de.dkfz.roddy.execution.jobs.cluster.lsf.LSFJobManager
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSJobManager
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm

@Mock([
        ProcessingOption,
        Realm
])
class ClusterJobManagerFactoryServiceWithoutAutowiredSpec extends Specification {

    final static String SSH_USER = "user"

    def "test getJobManager, get correct manager"(Realm.JobScheduler type, Class managerClass) {
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

    def "test getJobManager, get the different manager for the different realm"() {
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

    def "test getJobManager, get the same manager for the same realm"() {
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
