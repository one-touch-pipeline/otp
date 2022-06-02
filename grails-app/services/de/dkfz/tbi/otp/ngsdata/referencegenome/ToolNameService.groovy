/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata.referencegenome

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.ngsdata.ToolName
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils

@Transactional
class ToolNameService {

    List<ToolName> findAllToolNamesByType(ToolName.Type type) {
        return ToolName.findAllByType(type)
    }

    List<String> findAllToolNamesForRNA() {
        List<String> toolNames = ToolName.findAllByTypeAndNameNotIlike(ToolName.Type.RNA, "GENOME_STAR_INDEX%")*.name
        toolNames.add(ProjectService.GENOME_STAR_INDEX)
        return toolNames.sort()
    }

    ToolName findToolNameByNameAndType(String name, ToolName.Type type) {
        return CollectionUtils.atMostOneElement(ToolName.findAllByNameAndType(name, type))
    }
}
