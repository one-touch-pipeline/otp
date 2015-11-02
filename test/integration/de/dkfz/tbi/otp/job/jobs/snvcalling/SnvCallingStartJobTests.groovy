package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.job.processing.ProcessParameter

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

import grails.plugin.springsecurity.SpringSecurityUtils
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.joda.time.DateTimeUtils
import org.joda.time.format.DateTimeFormat

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.TestJobHelper
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.testing.GroovyScriptAwareTestCase

public class SnvCallingStartJobTests extends GroovyScriptAwareTestCase {

    private static String PLAN_NAME = "SnvWorkflow"
    private static long ARBITRARY_TIMESTAMP = 1337

    @Autowired
    private SnvCallingStartJob snvCallingStartJob

    @Autowired
    private SnvCallingService snvCallingService

    SnvCallingInstanceTestData snvTestData

    @Test
    public void testExecute() {
        createUserAndRoles()
        SpringSecurityUtils.doWithAuth('admin') { runScript('scripts/workflows/SnvWorkflow.groovy') }
        snvTestData = new SnvCallingInstanceTestData()
        snvTestData.createSnvObjects()

        SamplePair mockSamplePair
        try {
            // arrange: create basic objects / mock required services
            Project project = snvTestData.samplePair.project
            SeqType seqType = snvTestData.samplePair.seqType

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
            mockSamplePair.processingStatus = ProcessingStatus.NEEDS_PROCESSING
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

            snvCallingService.metaClass.samplePairForSnvProcessing = { return mockSamplePair }

            SnvCallingInstanceTestData.createOrFindExternalScript()

            // snv config
            SnvConfig config = snvTestData.createSnvConfig()

            // validate starting condition: we shouldn't have done anything yet
            snvCallingStartJob.jobExecutionPlan = TestJobHelper.findJobExecutionPlan(PLAN_NAME)
            assert TestJobHelper.findProcessesForPlanName(PLAN_NAME).size() == 0
            assert SnvCallingInstance.findAll().size() == 0
            assert ProcessParameter.findAll().size() == 0

            // set a fixed timestamp to test the instance name
            DateTimeUtils.setCurrentMillisFixed(ARBITRARY_TIMESTAMP)


            // act: run the startjob
            snvCallingStartJob.execute()

            // assertions: see if the startJob picked up the correct SamplePair and made an job-instance for it
            Process process = exactlyOneElement(TestJobHelper.findProcessesForPlanName(PLAN_NAME))

            SnvCallingInstance snvCallingInstance = exactlyOneElement(SnvCallingInstance.findAll())
            assert snvCallingInstance.sampleType1BamFile == mockBam1
            assert snvCallingInstance.sampleType2BamFile == mockBam2
            assert snvCallingInstance.samplePair == mockSamplePair
            assert snvCallingInstance.config == config
            assert snvCallingInstance.instanceName == DateTimeFormat.forPattern("yyyy-MM-dd_HH'h'mm_Z").print(ARBITRARY_TIMESTAMP)

            ProcessParameter processParameter = exactlyOneElement(ProcessParameter.findAll())
            assert processParameter.className == SnvCallingInstance.class.name
            assert processParameter.value == snvCallingInstance.id.toString()
            assert processParameter.process == process

            assert mockSamplePair.processingStatus == ProcessingStatus.NO_PROCESSING_NEEDED

        } finally {
            TestCase.removeMetaClass(SnvCallingService.class, snvCallingService)
            DateTimeUtils.setCurrentMillisSystem()
        }
    }
}
