package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.*
import spock.lang.*

class SnvCallingStartJobIntegrationSpec extends Specification {

    TestAbstractSnvCallingStartJob snvCallingStartJob
    TestAbstractSnvCallingStartJob roddySnvCallingStartJob

    TestConfigService configService


    void setup() {
        configService = new TestConfigService()
    }

    void "test method restart, fail when process is null"() {
        when:
        snvCallingStartJob = new TestAbstractSnvCallingStartJob()
        snvCallingStartJob.restart(null)

        then:
        AssertionError error = thrown()
        error.message.contains("assert process")
    }

    void "test method restart with SnvCallingInstance"() {
        given:
        SnvJobResult snvJobResult = DomainFactory.createSnvJobResultWithRoddyBamFiles()
        SnvCallingInstance failedInstance = snvJobResult.snvCallingInstance

        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)

        failedProcess.metaClass.getProcessParameterObject = { -> failedInstance }

        snvCallingStartJob = Spy(TestAbstractSnvCallingStartJob) {
            1 * getInstanceClass() >> SnvCallingInstance
            1 * getInstanceName(_) >> "someInstanceName"
            1 * withdrawSnvJobResultsIfAvailable(_) >> { BamFilePairAnalysis bamFilePairAnalysis ->
                SnvJobResult.findAllBySnvCallingInstance(bamFilePairAnalysis).each {
                    it.withdraw()
                }
            }
        }
        snvCallingStartJob.executionService = Mock(ExecutionService) {
            1 * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String cmd ->
                assert cmd == "rm -rf ${failedInstance.instancePath.absoluteDataManagementPath} ${failedInstance.instancePath.absoluteStagingPath}"
            }
        }
        snvCallingStartJob.schedulerService = Mock(SchedulerService) {
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
            process = snvCallingStartJob.restart(failedProcess)
        }
        SnvCallingInstance restartedInstance = SnvCallingInstance.get(ProcessParameter.findByProcess(process).value)

        then:
        SnvCallingInstance.list().size() == 2
        restartedInstance.config == failedInstance.config
        restartedInstance.samplePair == failedInstance.samplePair
        restartedInstance.sampleType1BamFile == failedInstance.sampleType1BamFile
        restartedInstance.sampleType2BamFile == failedInstance.sampleType2BamFile

        failedInstance.withdrawn == true
        snvJobResult.withdrawn == true
    }

    void "test method restart with RoddySnvCallingInstance"() {
        given:
        RoddySnvCallingInstance failedInstance =  DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()

        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)

        failedProcess.metaClass.getProcessParameterObject = { -> failedInstance }

        roddySnvCallingStartJob = Spy(TestAbstractSnvCallingStartJob) {
            1 * getInstanceClass() >> RoddySnvCallingInstance
            1 * getInstanceName(_) >> "someOtherInstanceName"
        }
        roddySnvCallingStartJob.executionService = Mock(ExecutionService) {
            1 * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String cmd ->
                assert cmd == "rm -rf ${failedInstance.instancePath.absoluteDataManagementPath} ${failedInstance.instancePath.absoluteStagingPath}"
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
