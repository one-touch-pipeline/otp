package de.dkfz.tbi.otp.job.jobs.snvcalling

import static org.junit.Assert.*

import org.apache.commons.logging.impl.NoOpLog
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils

class SnvJoiningJobTests {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ExecutionService executionService

    @Autowired
    SchedulerService schedulerService

    File testDirectory
    SnvCallingInstanceTestData testData
    SnvCallingInstance snvCallingInstance
    SnvJobResult jobResult
    SnvJoiningJob job

    @Before
    void setUp() {
        testDirectory = TestCase.createEmptyTestDirectory()
        testData = new SnvCallingInstanceTestData()
        testData.createSnvObjects(testDirectory)
        testData.createSnvConfig(SnvCallingJobTests.CONFIGURATION)
        snvCallingInstance = testData.createAndSaveSnvCallingInstance()
        jobResult = testData.createAndSaveSnvJobResult(snvCallingInstance, SnvCallingStep.CALLING, null, SnvProcessingStates.IN_PROGRESS)

        job = applicationContext.getBean('snvJoiningJob',
                DomainFactory.createAndSaveProcessingStep(SnvJoiningJob.toString()), [])
        job.log = new NoOpLog()
    }

    @After
    void tearDown() {
        testDirectory.deleteDir()
    }

    @Test
    void testMaybeSubmit() {
        testData.createProcessingOptions()

        boolean querySshCalled = false
        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, String command, File script, String options ->
            assert !querySshCalled
            querySshCalled = true
            File snvFile = new OtpPath(snvCallingInstance.snvInstancePath, SnvCallingStep.CALLING.getResultFileName(snvCallingInstance.individual, null)).absoluteDataManagementPath
            String scriptCommandPart = "${testData.externalScript_Joining.scriptFilePath}; " +
                    "md5sum ${snvFile} > ${snvFile}.md5sum"
            assert command.contains(scriptCommandPart)
            return [HelperUtils.uniqueString]
        }

        testData.createBamFile(snvCallingInstance.sampleType1BamFile)
        testData.createBamFile(snvCallingInstance.sampleType2BamFile)

        schedulerService.startingJobExecutionOnCurrentThread(job)
        try {
            TestCase.withMockedExecuteCommand(job.executionService, {
                assertEquals(NextAction.WAIT_FOR_CLUSTER_JOBS, job.maybeSubmit(snvCallingInstance))
            })
            assert querySshCalled
        } finally {
            schedulerService.finishedJobExecutionOnCurrentThread(job)
        }
    }

    @Test
    void testValidate() {
        testData.createBamFile(snvCallingInstance.sampleType1BamFile)
        testData.createBamFile(snvCallingInstance.sampleType2BamFile)
        File configFilePath = snvCallingInstance.configFilePath.absoluteDataManagementPath
        configFilePath.parentFile.mkdirs()
        snvCallingInstance.config.writeToFile(configFilePath)
        File resultFilePath = jobResult.getResultFilePath().absoluteDataManagementPath
        long resultFileSize = CreateFileHelper.createFile(resultFilePath).length()
        String md5sum = HelperUtils.randomMd5sum
        CreateFileHelper.createFile(new File("${resultFilePath}.md5sum"), "${md5sum} ${resultFilePath.name}")

        job.validate(snvCallingInstance)

        assert jobResult.processingState == SnvProcessingStates.FINISHED
        assert jobResult.fileSize == resultFileSize
        assert jobResult.md5sum == md5sum
    }
}
