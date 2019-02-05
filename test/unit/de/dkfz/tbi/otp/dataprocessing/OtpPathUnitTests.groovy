/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.ngsdata.*

import static groovy.util.GroovyTestCase.assertEquals

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
        configService.setOtpProperty((OtpProperty.PATH_PROCESSING_ROOT), 'processing_root')
        assertEquals "processing_root is not absolute.", shouldFail { path.absoluteDataProcessingPath }

        configService.setOtpProperty((OtpProperty.PATH_PROCESSING_ROOT), '/processing_root')
        assertEquals "/processing_root/first/second/third/fourth", path.absoluteDataProcessingPath.path
    }

    @Test
    void testGetAbsoluteDataManagementPath() {
        configService.setOtpProperty((OtpProperty.PATH_PROJECT_ROOT), 'root_path')
        assertEquals "root_path is not absolute.", shouldFail { path.absoluteDataManagementPath }

        configService.setOtpProperty((OtpProperty.PATH_PROJECT_ROOT), '/root_path')
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
