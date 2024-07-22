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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.project.Project

@Transactional
class ProjectOverviewService {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    @CompileDynamic
    List overviewProjectQuery(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            property("individualId")
            property("pid")
            property("sampleTypeName")
            property("seqTypeDisplayName")
            property("libraryLayout")
            property("singleCell")
            property("seqPlatformId")
            property("seqCenterName")
            property("laneCount")
            property("sum_N_BasePairsGb")
            property("projectName")
            order("pid")
            order("sampleTypeName")
            order("seqTypeDisplayName")
            order("libraryLayout")
            order("seqPlatformId")
            order("seqCenterName")
            order("laneCount")
        }
        List queryList = []
        for (def track in seq) {
            def queryListSingleRow = [
                    track.pid,
                    track.sampleTypeName,
                    track.seqTypeDisplayName,
                    track.libraryLayout,
                    track.singleCell,
                    track.seqCenterName,
                    SeqPlatform.get(track.seqPlatformId).toString(),
                    track.laneCount,
                    track.sum_N_BasePairsGb,
            ]
            queryList.add(queryListSingleRow)
        }
        return queryList
    }

    @CompileDynamic
    List patientsAndSamplesGBCountPerProject(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqTypeDisplayName")
                groupProperty("libraryLayout")
                groupProperty("singleCell")
                countDistinct("pid")
                count("sampleId")
                sum("sum_N_BasePairsGb")
            }
            order("seqTypeDisplayName")
        }
        return seq
    }

    @CompileDynamic
    Long individualCountByProject(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections { countDistinct("pid") }
        }
        return seq[0]
    }

    @CompileDynamic
    List sampleTypeNameCountBySample(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("sampleTypeName")
                countDistinct("sampleId")
            }
        }
        return seq
    }

    @CompileDynamic
    List centerNameRunId(Project project) {
        List seq = Sequence.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqCenterName")
                countDistinct("runId")
            }
            order("seqCenterName")
        }
        return seq
    }

    @CompileDynamic
    List centerNameRunIdLastMonth(Project project) {
        Calendar cal = Calendar.instance
        cal.add(Calendar.MONTH, -6)
        Date date = cal.time
        List seq = Sequence.withCriteria {
            eq("projectId", project?.id)
            gt("dateExecuted", date)
            projections {
                groupProperty("seqCenterName")
                countDistinct("runId")
            }
            order("seqCenterName")
        }
        return seq
    }
}
