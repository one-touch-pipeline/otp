package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifierImpl
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.CreateFileHelper
import org.junit.Before
import org.junit.After
import org.junit.Test

class AbstractRoddyJobTests {

    public static final ClusterJobIdentifier identifierA = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId1")
    public static final ClusterJobIdentifier identifierB = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId2")
    public static final ClusterJobIdentifier identifierC = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId3")
    public static final ClusterJobIdentifier identifierD = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId4")
    public static final ClusterJobIdentifier identifierE = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId5")
    public static final ClusterJobIdentifier identifierF = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId6")
    public static final ClusterJobIdentifier identifierG = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId7")

    def abstractMaybeSubmitValidateRoddyJobInst
    File testDirectory

    @Before
    void setUp() {
        testDirectory = TestCase.createEmptyTestDirectory()
    }

    @After
    void tearDown() {
        assert testDirectory.deleteDir()
    }

    @Test
    void testFailedOrNotFinishedClusterJobs() {
        abstractMaybeSubmitValidateRoddyJobInst = [
                getProcessingStep : { ->
                    return DomainFactory.createAndSaveProcessingStep()
                },
                getProcessParameterObject : { ->
                    RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
                    bamFile.metaClass.getPathToJobStateLogFiles { ->
                        return testDirectory.absolutePath
                    }
                    return bamFile
                }
        ] as AbstractRoddyJob

        Map<String, String> logFileMapA = createLogFileMap(identifierA, [exitCode: "1", timeStamp: "10"])
        Map<String, String> logFileMapB = createLogFileMap(identifierB, [exitCode: "4", timeStamp: "10"])
        Map<String, String> logFileMapC = createLogFileMap(identifierC, [exitCode: "3", timeStamp: "10"])
        Map<String, String> logFileMapE = createLogFileMap(identifierE, [exitCode: "0", timeStamp: "10"])
        Map<String, String> logFileMapF = createLogFileMap(identifierF, [exitCode: "1", timeStamp: "10"])
        Map<String, String> logFileMapG = createLogFileMap(identifierG, [exitCode: "1", timeStamp: "10"])

        // JOB A, 2 entries (start-entry + end-entry) in FILE 1, exitCode != 0 => failed
        // JOB B, 3 entries (start-entry + changed-entry + end-entry) in FILE 1, exitCode != 0 => failed
        // JOB C, 1 entry (start-entry) in FILE 1 => still in progress
        // JOB D, 0 enties => no information found
        // JOB E, 2 entries (start-entry + end-entry) in FILE 1, exitCode = 0 => sucessfully finished job, no output
        // JOB F, 2 entries (start-entry + end-entry) in FILE 2, exitCode != 0 => failed
        // JOB G, 2 entries (start-entry + end-entry) in FILE 1 + FILE 2, exitCode != 0 => failed

        String content = """
${logFileMapA.pbsId}.${logFileMapA.host}:57427:0:${logFileMapA.jobClass}
${logFileMapB.pbsId}.${logFileMapB.host}:57427:0:${logFileMapB.jobClass}
${logFileMapC.pbsId}.${logFileMapC.host}:57427:0:${logFileMapC.jobClass}
${logFileMapE.pbsId}.${logFileMapE.host}:57427:0:${logFileMapE.jobClass}
${logFileMapA.pbsId}.${logFileMapA.host}:${logFileMapA.exitCode}:${logFileMapA.timeStamp}:${logFileMapA.jobClass}
${logFileMapB.pbsId}.${logFileMapB.host}:0:1:${logFileMapB.jobClass}
${logFileMapB.pbsId}.${logFileMapB.host}:${logFileMapB.exitCode}:${logFileMapB.timeStamp}:${logFileMapB.jobClass}
${logFileMapE.pbsId}.${logFileMapE.host}:${logFileMapE.exitCode}:${logFileMapE.timeStamp}:${logFileMapE.jobClass}
${logFileMapG.pbsId}.${logFileMapG.host}:57427:0:${logFileMapG.jobClass}
"""
        String content2 = """
${logFileMapF.pbsId}.${logFileMapF.host}:57427:0:${logFileMapF.jobClass}
${logFileMapF.pbsId}.${logFileMapF.host}:${logFileMapF.exitCode}:${logFileMapF.timeStamp}:${logFileMapF.jobClass}
${logFileMapG.pbsId}.${logFileMapG.host}:${logFileMapG.exitCode}:${logFileMapG.timeStamp}:${logFileMapG.jobClass}
"""

        CreateFileHelper.createFile(new File(testDirectory.absolutePath, "testFile"), content)
        CreateFileHelper.createFile(new File(testDirectory.absolutePath, "testFile2"), content2)

        Collection<? extends ClusterJobIdentifier> finishedClusterJobs =
                [
                        identifierA,
                        identifierB,
                        identifierC,
                        identifierD,
                        identifierE,
                        identifierF,
                        identifierG
                ]

        assert [
                (identifierA): "${identifierA} failed processing. ExitCode: ${logFileMapA.exitCode}",
                (identifierB): "${identifierB} failed processing. ExitCode: ${logFileMapB.exitCode}",
                (identifierC): "${identifierC} is not finished.",
                (identifierD): "JobStateLogFile contains no information for ${identifierD}",
                (identifierF): "${identifierF} failed processing. ExitCode: ${logFileMapF.exitCode}",
                (identifierG): "${identifierG} failed processing. ExitCode: ${logFileMapG.exitCode}",
        ] == abstractMaybeSubmitValidateRoddyJobInst.failedOrNotFinishedClusterJobs(finishedClusterJobs)
    }

    private Map<String, String> createLogFileMap(ClusterJobIdentifier identifier, properties = [:]) {
        return [pbsId: identifier.clusterJobId,
                host: "testHost",
                exitCode: "0",
                timeStamp: "0",
                jobClass: "testJobClass"] + properties
    }
}
