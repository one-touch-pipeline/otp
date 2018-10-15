package de.dkfz.tbi.otp.job.processing

import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.execution.jobs.cluster.lsf.*
import de.dkfz.roddy.execution.jobs.cluster.pbs.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

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
