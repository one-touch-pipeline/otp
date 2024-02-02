/*
 * Copyright 2011-2024 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

/**
 * Show all used roddy analysis plugins per pipeline and project.
 *
 * Only the last calling for a sample is taken into account.
 */

String sql = """
select
    analysis
from
    BamFilePairAnalysis analysis
where
    analysis.withdrawn = false
    and analysis.config.class = 'de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig'
and
    analysis.id = (
        select
            max(id)
        from
            BamFilePairAnalysis analysis2
        where
            analysis2.samplePair = analysis.samplePair
            and analysis2.config.pipeline = analysis.config.pipeline
    )
"""

println IndelCallingInstance.executeQuery(sql.toString()).groupBy(
        [
                { BamFilePairAnalysis analysis ->
                    analysis.config.pipeline
                },
                { BamFilePairAnalysis analysis ->
                    analysis.project
                },
                { BamFilePairAnalysis analysis ->
                    analysis.seqType
                },
                { BamFilePairAnalysis analysis ->
                    analysis.config.programVersion
                }

        ]
).collect { Pipeline pipeline, Map map1 ->
    "${pipeline}\n" +
            map1.collect { Project project, Map map2 ->
                "    ${project}\n" +
                        map2.collect { SeqType seqType, Map map3 ->
                            "        ${seqType.displayNameWithLibraryLayout} (${RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)?.programVersion})\n" +
                                    map3.collect { String programVersion, List list ->
                                        "            ${programVersion}: ${list.size()}".toString()
                                    }.sort().join('\n')
                        }.sort().join('\n')
            }.sort().join('\n\n')
}.sort().join('\n\n\n')
