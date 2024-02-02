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

/**
 * Show the used plugin version per cluster, project and pipeline.
 *
 * Only the last calling for a sample is taken into account.
 */

String sql = """
select
    pipeline.name,
    project.name,
    config.programVersion,
    count(analysis.id) as count
from
    BamFilePairAnalysis analysis
    join analysis.samplePair.mergingWorkPackage1.sample.individual.project project
    join analysis.config config
    join config.pipeline pipeline,
    ClusterJob job
    ProcessParameter processParameter
where
    cast(processParameter.value as long) = analysis.id
    and processParameter.process = job.processingStep.process
    and analysis.withdrawn = false
    and pipeline.name != 'OTP_SNV'
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
group by
    pipeline.name,
    project.name,
    config.programVersion

"""

println IndelCallingInstance.executeQuery(sql.toString()).groupBy(
        [
                { it[0] },
                { it[1] },
                { it[2] },
                { it[3] },
        ]
).collect { key1, map1 ->
    "${key1}\n" +
            map1.collect { key2, map2 ->
                " ${key2}\n" +
                        map2.collect { key3, map3 ->
                            "   ${key3}\n" +
                                    map3.collect { key4, list4 ->
                                        "     ${key4} : ${list4[0][4]}"
                                    }.sort().join('\n')
                        }.sort().join('\n')
            }.sort().join('\n')
}.sort().join('\n\n')
