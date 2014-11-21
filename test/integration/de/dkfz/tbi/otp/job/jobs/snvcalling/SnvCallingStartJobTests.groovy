package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.job.processing.ProcessParameter

import static de.dkfz.tbi.TestCase.assertEquals
import static de.dkfz.tbi.TestCase.removeMetaClass
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static org.junit.Assert.*

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.joda.time.DateTimeUtils
import org.joda.time.format.DateTimeFormat

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstanceTestData
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.job.jobs.TestJobHelper
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SampleType.Category
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest

public class SnvCallingStartJobTests extends GroovyScriptAwareIntegrationTest {

    private static String PLAN_NAME = "SnvWorkflow"
    private static long ARBITRARY_TIME = 1337

    @Autowired
    private SnvCallingStartJob snvCallingStartJob

    @Autowired
    private SnvCallingService snvCallingService

    SnvCallingInstanceTestData snvTestData

    @Test
    public void testExecute() {
        createUserAndRoles()
        SpringSecurityUtils.doWithAuth('admin') { run('scripts/workflows/SnvWorkflow.groovy') }
        snvTestData = new SnvCallingInstanceTestData()

        SampleTypeCombinationPerIndividual mockSamplePair
        try {
            // arrange: create basic objects / mock required services
            Individual individual = Individual.build()
            SeqType seqType = SeqType.build()

            ProcessedMergedBamFile mockBam1 = snvTestData.createProcessedMergedBamFile(individual, seqType, "1")
            ProcessedMergedBamFile mockBam2 = snvTestData.createProcessedMergedBamFile(individual, seqType, "2")

            // sampletypes for sample pair
            SampleType sampleType1 = mockBam1.sampleType
            SampleTypePerProject.build(project: individual.project, sampleType: sampleType1, category: Category.DISEASE)
            SampleType sampleType2 = mockBam2.sampleType

            // Sample pair + service-mocks to make sure it is found by the startJob
            mockSamplePair = SampleTypeCombinationPerIndividual.build(
                    needsProcessing: true,
                    individual: individual,
                    sampleType1: sampleType1,
                    sampleType2: sampleType2,
                    seqType: seqType,
                    )
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

            // snv config
            SnvConfig config = SnvConfig.build(
                    project: individual.project,
                    seqType: mockSamplePair.seqType
                    )


            // validate starting condition: we haven't done anything yet
            snvCallingStartJob.jobExecutionPlan = TestJobHelper.findJobExecutionPlan(PLAN_NAME)
            assert TestJobHelper.findProcessesForPlanName(PLAN_NAME).size() == 0
            assert SnvCallingInstance.findAll().size() == 0
            assert ProcessParameter.findAll().size() == 0

            // set a fixed timestamp to test the instance name
            DateTimeUtils.setCurrentMillisFixed(ARBITRARY_TIME)


            // act: run the startjob
            snvCallingStartJob.execute()

            // assertions
            Process process = exactlyOneElement(TestJobHelper.findProcessesForPlanName(PLAN_NAME))

            SnvCallingInstance snvCallingInstance = exactlyOneElement(SnvCallingInstance.findAll())
            assert snvCallingInstance.sampleType1BamFile == mockBam1
            assert snvCallingInstance.sampleType2BamFile == mockBam2
            assert snvCallingInstance.sampleTypeCombination == mockSamplePair
            assert snvCallingInstance.config == config
            assert snvCallingInstance.instanceName == DateTimeFormat.forPattern("yyyy-MM-dd_HH'h'mm_Z").print(ARBITRARY_TIME)

            ProcessParameter processParameter = exactlyOneElement(ProcessParameter.findAll())
            assert processParameter.className == SnvCallingInstance.class.name
            assert processParameter.value == snvCallingInstance.id.toString()
            assert processParameter.process == process

        } finally {
            TestCase.removeMetaClass(SnvCallingService.class, snvCallingService)
            DateTimeUtils.setCurrentMillisSystem()
        }
    }
}
