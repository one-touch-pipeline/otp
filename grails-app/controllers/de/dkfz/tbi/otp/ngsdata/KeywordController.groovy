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
package de.dkfz.tbi.otp.ngsdata

import grails.validation.Validateable
import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ProjectSelection
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.searchability.Keyword
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

class KeywordController implements CheckAndCall {

    KeywordService keywordService
    ProjectService projectService
    ProjectSelectionService projectSelectionService

    Map index() {
        List<Project> projects = projectService.allProjects
        if (!projects) {
            return [
                    projects: projects,
            ]
        }

        ProjectSelection selection = projectSelectionService.selectedProject

        // we need to reload it to get proper access to all properties
        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)
        project = atMostOneElement(Project.findAllByName(project?.name, [fetch: [keywords: 'join']]))
         return [
                    keywords                       : Keyword.listOrderByName() ?: [],
                    projects                       : projects,
                    project                        : project,
            ]
        }

    def save(AddKeywordCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage(g.message(code: "keyword.index.failure") as String, cmd.getErrors())
        }
        else {
            keywordService.addKeywords(cmd.value, cmd.project)
            flash.message = new FlashMessage(g.message(code: "keyword.index.success") as String)
        }
        redirect(action: "index")
    }

    def remove(RemoveKeywordCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage(g.message(code: "keyword.index.removeKeywordFailure") as String, cmd.getErrors())
        }
        else {
            keywordService.removeKeywordFromProject(cmd.keyword, cmd.project)
            flash.message = new FlashMessage(g.message(code: "keyword.index.removeKeywordSuccess") as String)
        }
        redirect(action: "index")
    }
}

class AddKeywordCommand implements Validateable {
    String value
    Project project
}

class RemoveKeywordCommand implements Validateable {
    Keyword keyword
    Project project
}
