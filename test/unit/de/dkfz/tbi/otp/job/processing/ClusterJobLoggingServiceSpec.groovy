package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.Mock
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.*

@Mock([
        JobDefinition,
        JobExecutionPlan,
        Process,
        ProcessingStep,
        ProcessingStepUpdate,
        Realm,
])
class ClusterJobLoggingServiceSpec extends Specification {

    TestConfigService configService

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    Realm realm
    ProcessingStepUpdate processingStepUpdate
    ClusterJobLoggingService service


    void setup() {
        configService = new TestConfigService([(OtpProperty.PATH_CLUSTER_LOGS_OTP): temporaryFolder.newFolder().path])
        realm = DomainFactory.createRealm()
        processingStepUpdate = DomainFactory.createProcessingStepUpdate()
        service = new ClusterJobLoggingService()
        service.lsdfFilesService = Stub(LsdfFilesService) {
            createDirectory(_, _) >> { File dir, Realm _ ->
                assert dir.mkdirs()
            }
        }
    }

    void cleanup() {
        configService.clean()
    }

    void "test logDirectory, when all fine return correct path"() {
        when:
        File logDir = service.logDirectory(processingStepUpdate.processingStep)

        then:
        logDir.path ==~ /${configService.getLoggingRootPath().path}\/${ClusterJobLoggingService.CLUSTER_LOG_BASE_DIR}\/\d{4}-\d\d-\d\d/
    }

    void "test logDirectory, when processingStep is null throw exception"() {
        when:
        service.logDirectory(null)

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
