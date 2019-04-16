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

package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.text.DateFormat
import java.text.SimpleDateFormat

class ProjectServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ConfigPerProjectAndSeqType,
                Pipeline,
                Project,
                Realm,
                RunYapsaConfig,
                SeqType,
        ]
    }

    void "createProject: dirName shouldn't overlap with root path"() {
        given:
        ProjectService projectService = new ProjectService([
                configService: new TestConfigService([
                        (OtpProperty.PATH_PROJECT_ROOT): "/some/nested/root/path",
                ])
        ])

        when:
        projectService.createProject(
                'project',
                dirName,
                new Realm(),
                ['category'],
                QcThresholdHandling.CHECK_AND_NOTIFY
        )

        then:
        AssertionError err = thrown()
        err.message.contains("contains (partial) data processing root path")

        where:
        dirName                         | _
        'some/nested/root/path/dirName' | _
        'nested/root/path/dirName'      | _
        'root/path/dirName'             | _
        'path/dirName'                  | _
        'some/dirName'                  | _
        'nested/dirName'                | _
        'root/dirName'                  | _
    }

    void "test invalidateProjectConfig"() {
        given:
        ProjectService projectService = new ProjectService()
        projectService.workflowConfigService = new WorkflowConfigService()
        RunYapsaConfig config = DomainFactory.createRunYapsaConfig()

        when:
        projectService.invalidateProjectConfig(config.project, config.seqType, config.pipeline)

        then:
        config.refresh()
        config.obsoleteDate != null
    }

    void "test createOrUpdateRunYapsaConfig"() {
        given:
        ProjectService projectService = new ProjectService()
        projectService.workflowConfigService = new WorkflowConfigService()
        RunYapsaConfig config = DomainFactory.createRunYapsaConfig()

        when:
        projectService.createOrUpdateRunYapsaConfig(config.project, config.seqType, "yapsa 1.0")

        then:
        config.obsoleteDate != null
        RunYapsaConfig newConfig = CollectionUtils.exactlyOneElement(RunYapsaConfig.findAllByProjectAndSeqTypeAndObsoleteDateIsNull(
                config.project, config.seqType))
        newConfig != config
        newConfig.programVersion == "yapsa 1.0"
    }

    void "test addAdditionalValuesToProjectInfo"() {
        given:
        ProjectService projectService = new ProjectService()
        ProjectInfo projectInfo = DomainFactory.createProjectInfo()
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd", Locale.ENGLISH)
        User performingUser = DomainFactory.createUser()
        AddProjectInfoCommand addProjectInfoCommand = new AddProjectInfoCommand([
                recipient : "recipient",
                commissioningUser : DomainFactory.createUser().username,
                transferDate : dateFormat.format(new Date()),
                validityDate : dateFormat.format(new Date()),
                transferMode : "transferMode",
                legalBasis : "transferMode",
        ])

        when:
        projectService.addAdditionalValuesToProjectInfo(projectInfo, addProjectInfoCommand, performingUser)

        then:
        projectInfo.recipient == addProjectInfoCommand.recipient
        projectInfo.performingUser == performingUser
        projectInfo.commissioningUser.username == addProjectInfoCommand.commissioningUser
        dateFormat.format(projectInfo.transferDate) == addProjectInfoCommand.transferDate
        dateFormat.format(projectInfo.validityDate) == addProjectInfoCommand.validityDate
        projectInfo.transferMode == addProjectInfoCommand.transferMode
        projectInfo.legalBasis == addProjectInfoCommand.legalBasis
    }
}
