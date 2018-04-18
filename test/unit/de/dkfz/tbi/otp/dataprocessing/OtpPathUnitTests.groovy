package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.DomainFactory

import static groovy.util.GroovyTestCase.assertEquals

import org.junit.Before
import org.junit.Test
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Realm
import grails.buildtestdata.mixin.Build

@Build([
        Project,
        Realm,
])

class OtpPathUnitTests {

    TestConfigService configService

    String projectName
    OtpPath path

    @Before
    void before() {
        projectName = 'testProject' + Double.doubleToLongBits(Math.random())
        final Project project = Project.build([
                name : projectName,
                realm: DomainFactory.createRealm(),
        ])

        configService = new TestConfigService()

        final OtpPath path0 = new OtpPath(project, 'first', 'second', 'third')
        assertEquals project, path0.project
        assertEquals new File('first/second/third'), path0.relativePath

        path = new OtpPath(path0, 'fourth')
        assertEquals project, path.project
        assertEquals new File('first/second/third/fourth'), path.relativePath
    }

    @Test
    void testGetAbsoluteDataProcessingPath() {
        configService.setOtpProperty('otp.processing.root.path', 'processing_root')
        assertEquals "processing_root is not absolute.", shouldFail { path.absoluteDataProcessingPath }

        configService.setOtpProperty('otp.processing.root.path', '/processing_root')
        assertEquals "/processing_root/first/second/third/fourth", path.absoluteDataProcessingPath.path
    }

    @Test
    void testGetAbsoluteDataManagementPath() {
        configService.setOtpProperty('otp.root.path', 'root_path')
        assertEquals "root_path is not absolute.", shouldFail { path.absoluteDataManagementPath }

        configService.setOtpProperty('otp.root.path', '/root_path')
        assertEquals "/root_path/first/second/third/fourth", path.absoluteDataManagementPath.path
    }

    @Test
    void testGetAbsolutePath() {
        final String ABSOLUTE_PATH = "/absolute_path"
        final String RELATIVE_PATH = "relative_path"

        assertEquals "${RELATIVE_PATH} is not absolute.", shouldFail {
            path.getAbsolutePath(new File(RELATIVE_PATH))
        }

        assertEquals "/absolute_path/first/second/third/fourth", path.getAbsolutePath(new File(ABSOLUTE_PATH)).path
    }
}
