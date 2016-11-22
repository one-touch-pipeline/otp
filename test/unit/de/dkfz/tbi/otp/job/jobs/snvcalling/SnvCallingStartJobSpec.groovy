package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import grails.test.mixin.*
import spock.lang.*

@Mock([
        DataFile,
        ExternalScript,
        FileType,
        Individual,
        JobDefinition,
        JobExecutionPlan,
        LibraryPreparationKit,
        MergingWorkPackage,
        Pipeline,
        Process,
        ProcessingStep,
        ProcessParameter,
        Project,
        ProjectCategory,
        Realm,
        ReferenceGenome,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SamplePair,
        SampleType,
        SampleTypePerProject,
        SeqCenter,
        SeqPlatformGroup,
        SeqPlatform,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SnvCallingInstance,
        SnvConfig,
        SnvJobResult,
        SoftwareTool,
        SoftwareToolIdentifier,
])
class SnvCallingStartJobSpec extends Specification {

    TestAbstractSnvCallingStartJob snvCallingStartJob


    void "test method restart, fail when process is null"() {
        when:
        snvCallingStartJob = new TestAbstractSnvCallingStartJob()
        snvCallingStartJob.restart(null)

        then:
        AssertionError error = thrown()
        error.message.contains("assert process")
    }


    void "test method restart"() {
        given:
        SnvJobResult snvJobResult = DomainFactory.createSnvJobResultWithRoddyBamFiles()
        SnvCallingInstance failedInstance = snvJobResult.snvCallingInstance
        DomainFactory.createRealmDataManagement(name: failedInstance.project.realmName)
        DomainFactory.createRealmDataProcessing(name: failedInstance.project.realmName)

        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)

        failedProcess.metaClass.getProcessParameterObject = { -> failedInstance }

        snvCallingStartJob = Spy(TestAbstractSnvCallingStartJob) {
            1 * getInstanceClass() >> SnvCallingInstance
            1 * getInstanceName(_) >> "someInstanceName"
        }
        snvCallingStartJob.executionService = Mock(ExecutionService) {
            1 * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String cmd ->
                assert cmd == "rm -rf ${failedInstance.snvInstancePath.absoluteDataManagementPath} ${failedInstance.snvInstancePath.absoluteStagingPath}"
            }
        }
        snvCallingStartJob.configService = new ConfigService()
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
        restartedInstance.latestDataFileCreationDate == failedInstance.latestDataFileCreationDate

        failedInstance.withdrawn == true
        snvJobResult.withdrawn == true
    }
}
