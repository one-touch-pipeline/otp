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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*

/**
 * Create Statistic about SeqType and Projects for project groups
 */

"""
""".split('\n')*.trim().findAll().each {
    ProjectGroup projectGroup = CollectionUtils.exactlyOneElement(ProjectGroup.findAllByName(it))
    List list = SeqTrack.createCriteria().list {
        projections {
            groupProperty('seqType')
            sample {
                individual {
                    project {
                        eq('projectGroup', projectGroup)
                        countDistinct('id')
                    }
                    countDistinct('id')
                }
                countDistinct('id')
            }
        }
    }.sort {
        it[0].alias
    }.collect {
        "    ${it[0]}: Projecte: ${it[1]}, Individuals: ${it[2]}, Samples: ${it[3]} "
    }
    println "${projectGroup}: ${Project.countByProjectGroup(projectGroup)} Projekte"
    println list.join('\n')
}
''
