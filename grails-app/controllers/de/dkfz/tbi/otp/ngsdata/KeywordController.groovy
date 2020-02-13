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

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.utils.StringUtils

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

        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)
        project = atMostOneElement(Project.findAllByName(project?.name, [fetch: [keywords: 'join']]))

        Closure keywordSorting = { Keyword keyword ->
            keyword.name.toLowerCase()
        }

        List<Keyword> otherAvailableKeywords = Keyword.list() - project.keywords
        return [
                availableKeywords: otherAvailableKeywords.sort(keywordSorting) ?: [],
                projectKeywords  : project.keywords.sort(keywordSorting),
                projects         : projects,
                project          : project,
        ]
    }

    def createOrAdd(AddKeywordByNameCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage(g.message(code: "keyword.index.failure") as String, cmd.getErrors())
        } else {
            keywordService.createOrAddKeyword(cmd.value, cmd.project)
            flash.message = new FlashMessage(g.message(code: "keyword.index.success") as String)
        }
        redirect(action: "index")
    }

    def add(AddKeywordCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage(g.message(code: "keyword.index.addKeywordFailure") as String, cmd.getErrors())
        } else {
            keywordService.addKeywordToProject(cmd.keyword, cmd.project)
            flash.message = new FlashMessage(g.message(code: "keyword.index.addKeywordSuccess") as String)
        }
        redirect(action: "index")
    }

    def remove(KeywordCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage(g.message(code: "keyword.index.removeKeywordFailure") as String, cmd.getErrors())
        } else {
            keywordService.removeKeywordFromProject(cmd.keyword, cmd.project)
            flash.message = new FlashMessage(g.message(code: "keyword.index.removeKeywordSuccess") as String)
        }
        redirect(action: "index")
    }
}

class AddKeywordByNameCommand implements Validateable {
    String value
    Project project

    static constraints = {
        value blank: false, size: 1..255
    }

    void setValue(String value) {
        this.value = StringUtils.trimAndShortenWhitespace(value)
    }
}

class KeywordCommand implements Validateable {
    Keyword keyword
    Project project
}

class AddKeywordCommand extends KeywordCommand {

    static constraints = {
        keyword validator: { val, obj ->
            if (val in obj.project.keywords) {
                return "already.contained"
            }
        }
    }
}