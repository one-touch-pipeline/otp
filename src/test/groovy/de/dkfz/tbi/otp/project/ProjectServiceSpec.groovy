/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.project

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils

import java.nio.file.Path
import java.time.LocalDate

class ProjectServiceSpec extends Specification implements DataTest, DomainFactoryCore, UserAndRoles {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ConfigPerProjectAndSeqType,
                Pipeline,
                Project,
                RunYapsaConfig,
                SeqType,
        ]
    }

    // Codenarc fails on the string "contains (partial)" and interprets the contains string like a method
    @SuppressWarnings("SpaceAfterMethodCallName")
    void "createProject: dirName shouldn't overlap with root path"() {
        given:
        ProjectService projectService = new ProjectService([
                configService: new TestConfigService([
                        (OtpProperty.PATH_PROJECT_ROOT): "/some/nested/root/path",
                ])
        ])
        ProjectCreationCommand cmd = new ProjectCreationCommand(
                name: "project",
                individualPrefix: "individualPrefix",
                unixGroup: "unixGroup",
                directory: dirName,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )

        when:
        projectService.createProject(cmd)

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

    void "test getProjectDirectory all fine"() {
        given:
        ProjectService projectService = new ProjectService([
                configService    : new TestConfigService(),
                fileSystemService: new TestFileSystemService(),
        ])

        Project project = createProject()

        when:
        Path file = projectService.getProjectDirectory(project)

        then:
        file.absolute
        file.toString().contains(project.dirName)
    }

    void "test getProjectDirectory project directory contains slashes"() {
        given:
        ProjectService projectService = new ProjectService([
                configService    : new TestConfigService(),
                fileSystemService: new TestFileSystemService(),
        ])
        Project project = createProject(
                dirName: "${HelperUtils.uniqueString}/${HelperUtils.uniqueString}/${HelperUtils.uniqueString}"
        )

        when:
        Path file = projectService.getProjectDirectory(project)

        then:
        file.absolute
        file.toString().contains(project.dirName)
    }
}
