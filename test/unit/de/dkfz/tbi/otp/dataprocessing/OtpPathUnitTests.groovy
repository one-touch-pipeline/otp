package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.TestCase.assertEquals

import org.junit.Before
import org.junit.Test
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.Realm.OperationType
import grails.buildtestdata.mixin.Build
import grails.util.Environment

@Build([
        Project,
        Realm,
])
class OtpPathUnitTests {

    String projectName
    String realmName
    OtpPath path

    @Before
    void before() {
        projectName = 'testProject' + Double.doubleToLongBits(Math.random())
        realmName = 'testRealm' + Double.doubleToLongBits(Math.random())
        final Project project = Project.build([
                name: projectName,
                realmName: realmName,
        ])

        final OtpPath path0 = new OtpPath(project, 'first', 'second', 'third')
        assertEquals project, path0.project
        assertEquals new File('first/second/third'), path0.relativePath

        path = new OtpPath(path0, 'fourth')
        assertEquals project, path.project
        assertEquals new File('first/second/third/fourth'), path.relativePath
    }

    private Realm buildRealm(final OperationType operationType) {
        return Realm.build([
                name: realmName,
                operationType: operationType,
                env: Environment.getCurrent().name,
                rootPath: '',
                processingRootPath: '',
                stagingRootPath: null,
        ])
    }

    @Test
    void testGetAbsoluteStagingPath() {

        assertEquals "No DATA_PROCESSING realm found for project ${projectName}.", shouldFail { path.absoluteStagingPath }

        final Realm realm = buildRealm(OperationType.DATA_PROCESSING)
        assertEquals "stagingRootPath is not set for ${realm}.", shouldFail { path.absoluteStagingPath }

        realm.stagingRootPath = 'staging_root'
        assert realm.save()
        assertEquals "stagingRootPath (${realm.stagingRootPath}) is not absolute for ${realm}.", shouldFail { path.absoluteStagingPath }

        realm.stagingRootPath = '/staging_root'
        assert realm.save()
        assertEquals new File('/staging_root/first/second/third/fourth'), path.absoluteStagingPath
    }

    @Test
    void testGetAbsoluteDataProcessingPath() {

        assertEquals "No DATA_PROCESSING realm found for project ${projectName}.", shouldFail { path.absoluteDataProcessingPath }

        final Realm realm = buildRealm(OperationType.DATA_PROCESSING)
        assertEquals "processingRootPath is not set for ${realm}.", shouldFail { path.absoluteDataProcessingPath }

        realm.processingRootPath = 'processing_root'
        assert realm.save()
        assertEquals "processingRootPath (${realm.processingRootPath}) is not absolute for ${realm}.", shouldFail { path.absoluteDataProcessingPath }

        realm.processingRootPath = '/processing_root'
        assert realm.save()
        assertEquals new File('/processing_root/first/second/third/fourth'), path.absoluteDataProcessingPath
    }

    @Test
    void testGetAbsoluteDataManagementPath() {

        assertEquals "No DATA_MANAGEMENT realm found for project ${projectName}.", shouldFail { path.absoluteDataManagementPath }

        final Realm realm = buildRealm(OperationType.DATA_MANAGEMENT)
        assertEquals "rootPath is not set for ${realm}.", shouldFail { path.absoluteDataManagementPath }

        realm.rootPath = 'project_root'
        assert realm.save()
        assertEquals "rootPath (${realm.rootPath}) is not absolute for ${realm}.", shouldFail { path.absoluteDataManagementPath }

        realm.rootPath = '/project_root'
        assert realm.save()
        assertEquals new File('/project_root/first/second/third/fourth'), path.absoluteDataManagementPath
    }
}
