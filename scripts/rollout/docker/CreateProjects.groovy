/*
 * Copyright 2011-2020 The OTP authors
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

package rollout.docker

import de.dkfz.tbi.otp.ngsdata.QcThresholdHandling
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.Project

/**
 * Calls ../CreateProjects.groovy with configuration for Docker setup.
 */

GroovyShell shell = new GroovyShell()
def rollout = shell.parse(new File('scripts/rollout/CreateProjects.groovy'))

String ANALYSIS_BASE_PATH = "/otp/projects/analysis/"
Set<String> names = ["testProject1", "testProject2"]

Map<String, Object> projectCreationParameter = [
        name             : names,
        individualPrefix : ["DEV1", "DEV2"],
        dirName          : names,
        dirAnalysis      : names.collect{ANALYSIS_BASE_PATH+it},
        unixGroup        : ["test_project_01"],
        sampleIdentifierParserBeanName: [SampleIdentifierParserBeanName.NO_PARSER],
        qcThresholdHandling: [QcThresholdHandling.CHECK_NOTIFY_AND_BLOCK],
        projectType: [Project.ProjectType.SEQUENCING],
        processingPriority: [ctx.processingPriorityService.defaultPriority()]
]


rollout.createProjects(ctx, projectCreationParameter)
