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

package rollout

import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext
import de.dkfz.tbi.otp.ngsdata.ProjectCreationCommand
import de.dkfz.tbi.otp.ngsdata.QcThresholdHandling
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.Project


/**
 * @see Project
 * @see ProjectCreationCommand
 *
 * Basic script for creating projects.
 *
 * It provides a simple configuration of complex scenarios. The script interacts with the
 * ProjectService to create the projects. This means that database entries are created as
 * well as the necessary files on the file system. The parameters are defined in the form
 * of a {@code Map<String, Collection<Object>>}. The ProjectService requires a
 * ProjectCreationCommand which serves as the basis for the parameters.
 *
 * The required parameters are:
 *
 *      name, individualPrefix, dirname, analysisDirPath, unixGroups,
 *      parserBeanNames, qcThresholdHandlings, projectTypes, processingPriorities
 *
 * Where name, individualPrefix, dirname, analysisDirPath must be unique within themselves
 * and system wide. The unique parameters are set in the form of a set with the same size
 * as the desired number of projects. The algorithm is project name centric and determines
 * the number of projects from the number of names. Optionally, the number can also be set
 * with amount. The other parameters are set as lists. The algorithm iterates circularly, i.e.
 * if the number of projects to be created exceeds the number of denoted parameter elements,
 * the algorithm starts again from the first element.
 *
 * In addition to the required parameters, the following can be optionally set:
 *
 *     nameInMetadataFiles, projectGroup, usersToCopyFromBaseProject,
 *     tumorEntity, speciesWithStrain, projectInfoFile, projectInfoToCopy,
 *     description, forceCopyFiles, fingerPrinting, keywords, endDate, storageUntil,
 *     relatedProjects, internalNotes,
 *     ignoreUsersFromBaseObjects, publiclyAvailable, projectRequestAvailable,
 *     sendProjectCreationNotification
 *
 */

/*
* Example usage
* ---------------------------------------------------------
*/

// This Example creates two projects with shared unixGroup, sampleIdentifierParserBeanName,
// projectType and processingPriority
def ANALYSIS_BASE_PATH = "/path-to-analysis-base/"
def names = ["name1", "name2"]

Map<String, Object> projectCreationParameter = [
        name             : names,
        individualPrefix : ["dev1", "dev2"],
        dirName          : names,
        dirAnalysis      : names.collect{ANALYSIS_BASE_PATH+it},
        unixGroup        : ["unixGroup"],
        sampleIdentifierParserBeanName: [SampleIdentifierParserBeanName.NO_PARSER],
        qcThresholdHandling: [QcThresholdHandling.CHECK_NOTIFY_AND_BLOCK, QcThresholdHandling.CHECK_AND_NOTIFY],
        projectType: [Project.ProjectType.SEQUENCING],
        processingPriority: [ctx.processingPriorityService.defaultPriority()]
]

/*
* Parameter validation and Project creation
* ---------------------------------------------------------
*/

/**
 * Validates all required parameters.
 *
 * @param projectCreationParameter Map of all parameters except amount and
 * sendProjectCreationNotification.
 * @param amount The amount of Projects to create.
 * @return true if no assertion happens.
 */
static boolean validateProjectParams(Map<String, Object> projectCreationParameter, int amount) {

    projectCreationParameter.each {
        assert it.value in Collection: "$it.key has to be a collection"
    }

    assert amount > 0: "The amount of projects to be created must be a positive integer"

    assert projectCreationParameter['name'] &&
            projectCreationParameter['name'].size() ==
            amount: "The amount of projects to be created must as least cover the number of unique project names"

    assert projectCreationParameter['individualPrefix'] &&
            projectCreationParameter['individualPrefix'].size() ==
            amount: "The amount of projects to be created must as least cover the number of unique individual prefixes"

    assert projectCreationParameter['dirName'] &&
            projectCreationParameter['dirName'].size() ==
            amount: "The amount of projects to be created must as least cover the number of unique project directory names"

    assert projectCreationParameter['dirAnalysis'] &&
            projectCreationParameter['dirAnalysis'].size() ==
            amount: "The amount of projects to be created must as least cover the number of unique analysis directory paths"

    assert projectCreationParameter['unixGroup']: "At least one unix groups has to be set"

    assert projectCreationParameter['sampleIdentifierParserBeanName']: "At least one parser name has to be set"

    assert projectCreationParameter['qcThresholdHandling'] : "At least one threshold handling has to be set"

    assert projectCreationParameter['projectType'] : "At least one project type has to be set"

    assert projectCreationParameter['processingPriority'] : "At least one processing priority has to be set"

    return true
}

/**
 * Create Project and corresponding files on the file system for a given configuration.
 *
 * @param ctx The AnnotationConfigEmbeddedWebApplicationContext, necessary if the function
 * is called from another script.
 * @param projectCreationParameter The parameter map containing the project configuration.
 * @return a String indicating a successfully creation or an validation error.
 */
static String createProjects(AnnotationConfigEmbeddedWebApplicationContext ctx, Map<String, Object> projectCreationParameter) {


    int amount
    boolean sendNotifications

    if (projectCreationParameter.containsKey('amount')){
        amount = projectCreationParameter['amount'] as int
        projectCreationParameter = projectCreationParameter.findAll({ it.key != "amount" })
    } else {
        assert projectCreationParameter['name']: "name is a required field"
        amount = projectCreationParameter['name'].size()
    }
    if(projectCreationParameter.containsKey('sendProjectCreationNotification')){
        sendNotifications = projectCreationParameter['sendProjectCreationNotification']
        projectCreationParameter = projectCreationParameter.findAll({ it.key != "sendProjectCreationNotification" })
    } else {
        sendNotifications = false
    }

    if (validateProjectParams(projectCreationParameter, amount)) {

        List<ProjectCreationCommand> projects = []
        (0..(amount - 1)).each {
            projects.add(new ProjectCreationCommand())
        }

        for (int ki = 0; ki < projectCreationParameter.values().size(); ki++) {
            for (int vi = 0; vi < amount; vi++) {
                projects[vi][projectCreationParameter.keySet()[ki]] =
                        (projectCreationParameter.values()[ki] as List).get((vi % (projectCreationParameter.values()[ki] as List).size()) as int)
            }
        }

        Project.withTransaction {
            projects.each {
                def project = ctx.projectService.createProject(it)
                if (sendNotifications) {
                    ctx.projectService.sendProjectCreationMailToUserAndTicketSystem(project)
                }
            }
        }

        return "Projects ${projects} created"
    } else {
        return "Project parameters invalid"
    }
}

createProjects(ctx, projectCreationParameter)
