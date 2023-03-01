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
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.utils.StringUtils

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class KeywordController implements CheckAndCall {

    KeywordService keywordService
    ProjectSelectionService projectSelectionService
    ProjectService projectService

    static allowedMethods = [
            index      : "GET",
            createOrAdd: "POST",
            add        : "POST",
            remove     : "POST",
    ]

    Map index() {
        Project project = projectSelectionService.selectedProject
        project = projectService.findByProjectWithFetchedKeywords(project)

        Closure keywordSorting = { Keyword keyword ->
            keyword.name.toLowerCase()
        }

        List<Keyword> otherAvailableKeywords = keywordService.list() - project.keywords
        return [
                availableKeywords: otherAvailableKeywords.sort(keywordSorting) ?: [],
                projectKeywords  : project.keywords.sort(keywordSorting),
        ]
    }

    def createOrAdd(AddKeywordByNameCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage(g.message(code: "keyword.index.failure") as String, cmd.errors)
        } else {
            keywordService.createOrAddKeyword(cmd.value, projectSelectionService.requestedProject)
            flash.message = new FlashMessage(g.message(code: "keyword.index.success") as String)
        }
        redirect(action: "index")
    }

    def add(AddKeywordCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage(g.message(code: "keyword.index.addKeywordFailure") as String, cmd.errors)
        } else {
            keywordService.addKeywordToProject(cmd.keyword, projectSelectionService.requestedProject)
            flash.message = new FlashMessage(g.message(code: "keyword.index.addKeywordSuccess") as String)
        }
        redirect(action: "index")
    }

    def remove(KeywordCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage(g.message(code: "keyword.index.removeKeywordFailure") as String, cmd.errors)
        } else {
            keywordService.removeKeywordFromProject(cmd.keyword, projectSelectionService.requestedProject)
            flash.message = new FlashMessage(g.message(code: "keyword.index.removeKeywordSuccess") as String)
        }
        redirect(action: "index")
    }
}

class AddKeywordByNameCommand implements Validateable {
    String value

    static constraints = {
        value blank: false, size: 1..255
    }

    void setValue(String value) {
        this.value = StringUtils.trimAndShortenWhitespace(value)
    }
}

class KeywordCommand implements Validateable {
    Keyword keyword
}

class AddKeywordCommand extends KeywordCommand {
    ProjectSelectionService projectSelectionService

    static constraints = {
        keyword validator: { val, obj ->
            if (val in obj.projectSelectionService.requestedProject?.keywords) {
                return "already.contained"
            }
        }
    }
}
