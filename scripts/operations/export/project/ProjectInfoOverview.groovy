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

import de.dkfz.tbi.otp.project.ProjectInfo
import de.dkfz.tbi.otp.project.Project

/**
 * Creates an overview over all ProjectInfos with their comments.
 *
 * Creates an tab separated table with:
 * - project name
 * - type of project info: dta or document
 * - fileName of projectInfo
 * - comment of projectInfo, replacing all newlines and tabs with space
 *
 * This script has no input
 */

//work area
//-----------------------------------------
println "Project\tType\tFileName\tComment"
println Project.list().sort {
    it.name
}.collect {
    ProjectInfo.findAllByProject(it).sort {
        it.fileName
    }.collect { ProjectInfo projectInfo ->
        [
                projectInfo.project.name,
                projectInfo.dta ? 'DTA' : 'document',
                projectInfo.fileName,
                projectInfo.comment?.replaceAll("[\n\r\t]", " ") ?: '-',
        ].join('\t')
    }.join('\n') ?: "${it.name}\t-\t-\t-"
}.join('\n')

''
