package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

@Mock([
        JobDefinition,
        JobExecutionPlan,
        Process,
        ProcessingStep,
        ProcessingStepUpdate,
        Realm,
])
class ClusterJobLoggingServiceSpec extends Specification {



    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()



    Realm realm
    ProcessingStepUpdate processingStepUpdate
    ClusterJobLoggingService service


    void setup() {
        realm = DomainFactory.createRealmDataProcessing(temporaryFolder.newFolder())
        processingStepUpdate = DomainFactory.createProcessingStepUpdate()
        service = new ClusterJobLoggingService()
        service.lsdfFilesService = Stub(LsdfFilesService) {
            createDirectory(_, _) >> { File dir, Realm _ ->
                assert dir.mkdirs()
            }
        }
    }



    void "test logDirectory, when all fine return correct path"() {
        when:
        File logDir = service.logDirectory(realm, processingStepUpdate.processingStep)

        then:
        logDir.path ==~ /${realm.loggingRootPath}\/${ClusterJobLoggingService.CLUSTER_LOG_BASE_DIR}\/\d{4}-\d\d-\d\d/
    }

    void "test logDirectory, when realm is null throw exception"() {
        when:
        ClusterJobLoggingService.logDirectory(null, processingStepUpdate.processingStep)

        then:
        AssertionError e = thrown()
        e.message.contains('realm')
    }

    void "test logDirectory, when processingStep is null throw exception"() {
        when:
        ClusterJobLoggingService.logDirectory(realm, null)

        then:
        AssertionError e = thrown()
        e.message.contains('processingStep')
    }



    void "test createAndGetLogDirectory, when all fine the log directory is created"() {
        when:
        File logDir = service.createAndGetLogDirectory(realm, processingStepUpdate.processingStep)

        then:
        logDir.exists()
    }

    void "test createAndGetLogDirectory can called multiple times for same directory without error"() {
        when:
        service.createAndGetLogDirectory(realm, processingStepUpdate.processingStep)
        service.createAndGetLogDirectory(realm, processingStepUpdate.processingStep)
        service.createAndGetLogDirectory(realm, processingStepUpdate.processingStep)

        then:
        noExceptionThrown()
    }

    void "test createAndGetLogDirectory, when realm is null throw exception"() {
        when:
        service.createAndGetLogDirectory(null, processingStepUpdate.processingStep)

        then:
        AssertionError e = thrown()
        e.message.contains('realm')
    }

    void "test createAndGetLogDirectory, when processingStep is null throw exception"() {
        when:
        service.createAndGetLogDirectory(realm, null)

        then:
        AssertionError e = thrown()
        e.message.contains('processingStep')
    }
}
