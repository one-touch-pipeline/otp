package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstanceTestData
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.ExternalScript
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 */

@TestFor(RoddyWorkflowConfig)
@Build([ExternalScript, Project, Workflow])
public class RoddyWorkflowConfigUnitTests {

    File configDir
    File configFile

    @Before
    void setUp() {
        configDir = TestCase.createEmptyTestDirectory()
        configFile = new File(configDir, 'tempConfigFile.txt')
    }


    @After
    void tearDown() {
        configFile.delete()
        TestCase.cleanTestDirectory()
    }

    @Test
    void testSaveRoddyWorkflowConfig_allFine() {
        configFile << "configuration"
        RoddyWorkflowConfig roddyWorkflowConfig = createRoddyWorkflowConfig()
        assert roddyWorkflowConfig.save(flush: true)
    }

    @Test
    void testSaveRoddyWorkflowConfig_NoWorkflow_ShouldFail() {
        configFile << "configuration"
        RoddyWorkflowConfig roddyWorkflowConfig = createRoddyWorkflowConfig()
        Workflow workflow = roddyWorkflowConfig.workflow
        roddyWorkflowConfig.workflow = null

        assertFalse roddyWorkflowConfig.validate()

        roddyWorkflowConfig.workflow = workflow
        assert roddyWorkflowConfig.save(flush: true)
    }

    @Test
    void testSaveRoddyWorkflowConfig_ConfigFilePathIsBlank_ShouldFail() {
        configFile << "configuration"
        RoddyWorkflowConfig roddyWorkflowConfig = createRoddyWorkflowConfig([configFilePath: ""])

        shouldFail AssertionError, {
            roddyWorkflowConfig.save(flush: true)
        }

        roddyWorkflowConfig.configFilePath = configFile.path
        assert roddyWorkflowConfig.save(flush: true)
    }

    @Test
    void testSaveRoddyWorkflowConfig_ConfigFileIsEmpty_ShouldFail() {
        RoddyWorkflowConfig roddyWorkflowConfig = createRoddyWorkflowConfig()

        shouldFail AssertionError, {
            roddyWorkflowConfig.save(flush: true)
        }

        configFile << "configuration"
        assert roddyWorkflowConfig.save(flush: true)
    }

    @Test
    void testSaveRoddyWorkflowConfig_ConfigFileIsNotAbsolute_ShouldFail() {
        configFile << "configuration"
        RoddyWorkflowConfig roddyWorkflowConfig = createRoddyWorkflowConfig([configFilePath: "tmp/"])

        shouldFail AssertionError, {
            roddyWorkflowConfig.save(flush: true)
        }
    }

    @Test
    void testSaveRoddyWorkflowConfig_ConfigFileDoesNotExist_ShouldFail() {
        configFile.delete()
        RoddyWorkflowConfig roddyWorkflowConfig = createRoddyWorkflowConfig()

        shouldFail AssertionError, {
            roddyWorkflowConfig.save(flush: true)
        }
    }


    @Test
    void testGetWorkflowVersion() {
        RoddyWorkflowConfig roddyWorkflowConfig = createRoddyWorkflowConfig()
        assert "v1" == roddyWorkflowConfig.workflowVersion
    }


    @Test
    void testUniqueConstraint() {
        CreateFileHelper.createFile(configFile)
        RoddyWorkflowConfig roddyWorkflowConfig1 = createRoddyWorkflowConfig()
        assert roddyWorkflowConfig1.save(flush: true)

        RoddyWorkflowConfig roddyWorkflowConfig2 = createRoddyWorkflowConfig([
                project: roddyWorkflowConfig1.project,
                workflow: roddyWorkflowConfig1.workflow,
                configFilePath: roddyWorkflowConfig1.configFilePath,
                ])

         assert !roddyWorkflowConfig2.validate()

    }



    RoddyWorkflowConfig createRoddyWorkflowConfig(Map properties = [:]) {
        SnvCallingInstanceTestData.createOrFindExternalScript()
        return new RoddyWorkflowConfig([
            project: Project.build(),
            externalScriptVersion: "v1",
            workflow: Workflow.buildLazy(name: Workflow.Name.PANCAN_ALIGNMENT, type: Workflow.Type.ALIGNMENT),
            configFilePath: configFile.path
        ] + properties)
    }

}
