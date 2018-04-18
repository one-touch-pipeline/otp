package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.*
import spock.lang.*

class SnvCallingStartJobIntegrationSpec extends Specification {

    RoddySnvCallingStartJob roddySnvCallingStartJob

    TestConfigService configService


    void setup() {
        configService = new TestConfigService()
    }

    void "test method restart, fail when process is null"() {
        when:
        roddySnvCallingStartJob = new RoddySnvCallingStartJob()
        roddySnvCallingStartJob.restart(null)

        then:
        AssertionError error = thrown()
        error.message.contains("assert process")
    }

    void "test method restart with RoddySnvCallingInstance"() {
        given:
        RoddySnvCallingInstance failedInstance =  DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()

        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)

        failedProcess.metaClass.getProcessParameterObject = { -> failedInstance }

        roddySnvCallingStartJob = Spy(RoddySnvCallingStartJob) {
            1 * getInstanceClass() >> RoddySnvCallingInstance
            1 * getInstanceName(_) >> "someOtherInstanceName"
        }
        roddySnvCallingStartJob.executionService = Mock(ExecutionService) {
            1 * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String cmd ->
                assert cmd == "rm -rf ${failedInstance.instancePath.absoluteDataManagementPath}"
            }
        }
        roddySnvCallingStartJob.schedulerService = Mock(SchedulerService) {
            1 * createProcess(_, _, _) >> { StartJob startJob, List<Parameter> input, ProcessParameter processParameter2 ->
                Process process2 = DomainFactory.createProcess(
                        jobExecutionPlan: failedProcess.jobExecutionPlan
                )
                processParameter2.process = process2
                assert processParameter2.save(flush: true)
                return process2
            }
        }

        when:
        Process process
        LogThreadLocal.withThreadLog(System.out) {
            process = roddySnvCallingStartJob.restart(failedProcess)
        }
        RoddySnvCallingInstance restartedInstance = RoddySnvCallingInstance.get(ProcessParameter.findByProcess(process).value)

        then:
        RoddySnvCallingInstance.list().size() == 2
        restartedInstance.config == failedInstance.config

        failedInstance.withdrawn == true
    }
}
