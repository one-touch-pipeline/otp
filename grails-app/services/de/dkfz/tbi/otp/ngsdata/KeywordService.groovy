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

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.StringUtils

import grails.gorm.transactions.Transactional

@Transactional
class KeywordService {

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void createOrAddKeyword(String value, Project project) {
        Keyword keyword = findOrSaveByName(StringUtils.trimAndShortenWhitespace(value))
        addKeywordToProject(keyword, project)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void addKeywordToProject(Keyword keyword, Project project) {
        project.addToKeywords(keyword)
        project.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void removeKeywordFromProject(Keyword keyword, Project project) {
        keyword.removeFromProjects(project)
        keyword.save(flush: true)
    }

    List<Keyword> list() {
        return Keyword.list()
    }

    Keyword findOrSaveByName(String name) {
        return CollectionUtils.atMostOneElement(Keyword.findAllByName(name)) ?: new Keyword(name: name).save(flush: true)
    }
}
