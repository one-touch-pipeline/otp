package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.testing.*
import de.dkfz.tbi.otp.tracking.*
import grails.plugin.springsecurity.*
import org.junit.*
import org.springframework.beans.factory.annotation.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

public class AbstractSnvCallingStartJobTests extends GroovyScriptAwareTestCase implements UserAndRoles  {

    private static String PLAN_NAME = "SnvWorkflow"

    @Autowired
    private TestAbstractSnvCallingStartJob testAbstractSnvCallingStartJob

    @Autowired
    private SnvCallingService snvCallingService

    SnvCallingInstanceTestData snvTestData

    @Test
    public void testExecute() {
        DomainFactory.createAlignableSeqTypes()
        createUserAndRoles()
        SpringSecurityUtils.doWithAuth('admin') { runScript(new File('scripts/workflows/SnvWorkflow.groovy')) }
        snvTestData = new SnvCallingInstanceTestData()
        snvTestData.createSnvObjects()

        SamplePair mockSamplePair
        try {
            // arrange: create basic objects / mock required services
            ProcessedMergedBamFile mockBam1 = snvTestData.bamFileTumor
            mockBam1.workPackage.bamFileInProjectFolder = mockBam1
            assert mockBam1.workPackage.save(flush: true)
            ProcessedMergedBamFile mockBam2 = snvTestData.bamFileControl
            mockBam2.workPackage.bamFileInProjectFolder = mockBam2
            assert mockBam2.workPackage.save(flush: true)

            // sampletypes for sample pair
            SampleType sampleType1 = mockBam1.sampleType
            SampleType sampleType2 = mockBam2.sampleType

            // Sample pair + service-mocks to make sure it is found by the startJob
            mockSamplePair = snvTestData.samplePair
            mockSamplePair.snvProcessingStatus = ProcessingStatus.NEEDS_PROCESSING
            assert mockSamplePair.save(flush: true)
            mockSamplePair.metaClass.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn = { SampleType sampleType ->
                if (sampleType == sampleType1) {
                    return mockBam1
                } else if (sampleType == sampleType2) {
                    return mockBam2
                } else {
                    assert false
                }
            }

            snvCallingService.metaClass.samplePairForProcessing = { short minPriority, Class ConfigPerProject -> return mockSamplePair }
            SnvConfig snvConfig = DomainFactory.createSnvConfig(pipeline: DomainFactory.createOtpSnvPipelineLazy());
            String instanceName = "test"
            testAbstractSnvCallingStartJob.metaClass.getConfigClass = { -> return SnvConfig}
            testAbstractSnvCallingStartJob.metaClass.getConfig = { SamplePair samplePair -> return snvConfig}
            testAbstractSnvCallingStartJob.metaClass.getInstanceName = { ConfigPerProject config -> return instanceName}
            testAbstractSnvCallingStartJob.metaClass.getInstanceClass = { -> return SnvCallingInstance.class }


            SnvCallingInstanceTestData.createOrFindExternalScript()

            // validate starting condition: we shouldn't have done anything yet
            testAbstractSnvCallingStartJob.jobExecutionPlan = TestJobHelper.findJobExecutionPlan(PLAN_NAME)
            assert TestJobHelper.findProcessesForPlanName(PLAN_NAME).size() == 0
            assert SnvCallingInstance.findAll().size() == 0
            assert ProcessParameter.findAll().size() == 0

            // set runSegment to test setStartedForSeqTracks
            OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
            DataFile.findAll()*.runSegment = DomainFactory.createRunSegment(otrsTicket: otrsTicket)

            // act: run the startjob
            testAbstractSnvCallingStartJob.execute()

            // assertions: see if the startJob picked up the correct SamplePair and made an job-instance for it
            Process process = exactlyOneElement(TestJobHelper.findProcessesForPlanName(PLAN_NAME))

            SnvCallingInstance snvCallingInstance = exactlyOneElement(SnvCallingInstance.findAll())
            assert snvCallingInstance.sampleType1BamFile == mockBam1
            assert snvCallingInstance.sampleType2BamFile == mockBam2
            assert snvCallingInstance.samplePair == mockSamplePair
            assert snvCallingInstance.config == snvConfig
            assert snvCallingInstance.instanceName == instanceName

            ProcessParameter processParameter = exactlyOneElement(ProcessParameter.findAll())
            assert processParameter.className == SnvCallingInstance.class.name
            assert processParameter.value == snvCallingInstance.id.toString()
            assert processParameter.process == process

            assert mockSamplePair.snvProcessingStatus == ProcessingStatus.NO_PROCESSING_NEEDED

            assert otrsTicket.snvStarted != null
        } finally {
            TestCase.removeMetaClass(SnvCallingService.class, snvCallingService)
            TestCase.removeMetaClass(TestAbstractSnvCallingStartJob.class, testAbstractSnvCallingStartJob)
        }
    }
}
