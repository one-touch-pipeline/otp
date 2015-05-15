package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.job.processing.ProcessParameter

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
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
import de.dkfz.tbi.otp.ngsdata.SampleType.Category
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

        SamplePair mockSamplePair
        try {
            // arrange: create basic objects / mock required services
            Project project = snvTestData.createProject()
            assert project.save(flush: true)
            Individual individual = snvTestData.createIndividual([project: project])
            assert individual.save(flush: true)
            SeqType seqType = snvTestData.createSeqType()
            assert seqType.save(flush: true)

            ProcessedMergedBamFile mockBam1 = snvTestData.createProcessedMergedBamFile(individual, seqType, "1")
            ProcessedMergedBamFile mockBam2 = snvTestData.createProcessedMergedBamFile(individual, seqType, "2")

            // sampletypes for sample pair
            SampleType sampleType1 = mockBam1.sampleType
            SampleTypePerProject sampleTypePerProject = new SampleTypePerProject(
                    project: project,
                    sampleType: sampleType1,
                    category: Category.DISEASE,
            )
            assert sampleTypePerProject.save(flush: true)
            SampleType sampleType2 = mockBam2.sampleType

            // Sample pair + service-mocks to make sure it is found by the startJob
            mockSamplePair = new SamplePair(
                    processingStatus: ProcessingStatus.NEEDS_PROCESSING,
                    individual: individual,
                    sampleType1: sampleType1,
                    sampleType2: sampleType2,
                    seqType: seqType,
                    )
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
            SnvConfig config = new SnvConfig (
                    project: project,
                    seqType: seqType,
                    configuration: "testConfig",
                    externalScriptVersion: "v1"
                    )
            assert config.save(flush: true)

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
