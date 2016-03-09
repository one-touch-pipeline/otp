package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.TestCase

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.Realm.OperationType
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest


class ExecutionServiceTests extends AbstractIntegrationTest {

    ExecutionService executionService

    Realm realm

    final static String SCRIPT_CONTENT = """#!/bin/bash
                       date
                       sleep 12
                       """

    @Before
    void setUp() {
        realm = new Realm(
                name: "DKFZ",
                env: "development",
                operationType: OperationType.DATA_MANAGEMENT,
                cluster: Realm.Cluster.DKFZ,
                rootPath: "/dev/null/otp-test/project/",
                processingRootPath: "/dev/null/otp-test/processing/",
                loggingRootPath: "/dev/null/otp-test/logging/",
                programsRootPath: "/testPrograms",
                webHost: "http://test.me",
                host: "invalid!host",
                port: 22,
                unixUser: "nobody",
                roddyUser: "nobody",
                timeout: -100,
                pbsOptions: "{-l: {nodes: '1', walltime: '00:00:30'}}"
                )
        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, String command, File script, String options -> return ['1234.example.pbs.server.invalid'] }
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecutionService, executionService)
    }

    @Test
    void testExecuteCommand_WhenRealmIsNull_ShouldFail() {
        assertNotNull(realm.save())
        shouldFail(AssertionError) {
            executionService.executeCommand(null, SCRIPT_CONTENT)
        }
    }

    @Test
    void testExecuteCommand_WhenCommandIsNull_ShouldFail() {
        assertNotNull(realm.save())
        shouldFail(AssertionError) {
            executionService.executeCommand(realm, null)
        }
    }

    @Test
    void testExecuteCommandConnectionFailed() {
        TestCase.removeMetaClass(ExecutionService, executionService)
        realm.host = "test.host.invalid"
        assertNotNull(realm.save())
        // Send command to pbs
        shouldFail(ProcessingException) {
            executionService.executeCommand(realm, SCRIPT_CONTENT)
        }
    }
}
