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
package de.dkfz.tbi.otp.cron

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.SessionUtils

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/**
 * Scheduled job to find and report inconsistencies in the user management.
 */
@SuppressWarnings("AnnotationsForJobs")
@Scope("singleton")
@Component
@Slf4j
class UserManagementInconsistenciesJob {

    @Autowired
    ProjectService projectService

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    ProcessingOptionService processingOptionService

    static String mailSubject = "Detected divergence in user management"

    static List<String> propertiesToDisplay = UserProjectRole.accessRelatedProperties

    @Scheduled(cron="0 0 5 * * *")
    void execute() {
        List<String> output = []
        SessionUtils.withNewSession {
            Map<String, List<Project>> projectsWithSharedUnixGroup = projectService.allProjectsWithSharedUnixGroup
            projectsWithSharedUnixGroup.each { String unixGroup, List<Project> projects ->
                List<User> allUsersOfUnixGroup = UserProjectRole.findAllByProjectInList(projects)*.user.unique()

                String unixGroupHeader = """\
                    |Unix Group: ${unixGroup}
                    |Affected Projects: ${projects*.name}
                    |Affected Users:    ${allUsersOfUnixGroup*.toString()}""".stripMargin()

                List<String> userDifferences = []
                allUsersOfUnixGroup.each { User user ->
                    List<List<String>> information = []
                    List<UserProjectRole> userProjectRoles = []
                    projects.each { Project project ->
                        UserProjectRole userProjectRole = atMostOneElement(UserProjectRole.findAllByUserAndProject(user, project))
                        userProjectRoles << userProjectRole
                        information << [project.name] + collectUserProjectRoleInformation(userProjectRole, propertiesToDisplay)
                    }
                    if (!allEqual(userProjectRoles)) {
                        userDifferences << """\
                            |${user.username} (${user.realName})
                            |${getAsTableString(["project"] + propertiesToDisplay, information)}""".stripMargin()
                    }
                }
                if (userDifferences) {
                    output << unixGroupHeader + "\n"
                    userDifferences.each {
                        output << it + "\n"
                    }
                }
            }
        }
        String mailContent = output.join("\n")
        if (mailContent) {
            notify(mailContent)
        }
    }

    private void notify(String content) {
        mailHelperService.sendEmail(mailSubject, content, recipients)
    }

    private List<String> getRecipients() {
        return [processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_OTP_MAINTENANCE)]
    }

    private static boolean allEqual(List<UserProjectRole> userProjectRoles) {
        if (null in userProjectRoles) {
            return false
        }
        UserProjectRole userProjectRole = userProjectRoles.first()
        return userProjectRoles.every {
            userProjectRole.equalByAccessRelatedProperties(it)
        }
    }

    private static List<String> collectUserProjectRoleInformation(UserProjectRole userProjectRole, List<String> expectedProperties) {
        return expectedProperties.collect {
            return userProjectRole?.getProperty(it)?.toString() ?: "-"
        }
    }

    private static String getAsTableString(List<String> header, List<List<String>> content) {
        String format = getFormatString(content + [header])
        return sprintf(format, header) + "\n" + content.collect { sprintf(format, it) }.join("\n")
    }

    private static String getFormatString(List<List<String>> content) {
        return getMaxColumnWidths(content).collect { int i -> "%-${i}s" }.join(" | ")
    }

    private static List<Integer> getMaxColumnWidths(List<List<String>> content) {
        List<Integer> widths = []
        content.each { List<String> row ->
            row.eachWithIndex { String column, int index ->
                int width = column.size()
                if (widths[index] < width) {
                    widths[index] = width
                }
            }
        }
        return widths
    }
}
