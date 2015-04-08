package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.testing.TestEndStateAwareJob
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.*

import de.dkfz.tbi.TestCase

/**
 */

@TestFor(RoddyWorkflowConfig)
@Build([Project, Workflow])
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
        configDir.deleteDir()
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


    RoddyWorkflowConfig createRoddyWorkflowConfig(Map properties = [:]) {
        return new RoddyWorkflowConfig([
            project: Project.build(),
            externalScriptVersion: "v1",
            workflow: Workflow.build(),
            configFilePath: configFile.path
        ] + properties)
    }

}