/*
 * Copyright 2011-2026 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.project.Project

@Transactional(readOnly = true)
class SampleOverviewService {

    /**
     * @param project the project for filtering the result
     * @return all SeqTypes used in the project
     */
    @CompileDynamic
    List<SeqType> seqTypeByProject(Project project) {
        List<Long> seqTypeIds = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqTypeId")
            }
        }
        List<SeqType> seqTypes = []
        if (seqTypeIds) {
            seqTypes = SeqType.withCriteria {
                'in'("id", seqTypeIds)
                order("name")
                order("libraryLayout")
            }
        }
        return seqTypes
    }

    @CompileDynamic
    List<String> sampleTypeByProject(Project project) {
        List<String> sampleTypes = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("sampleTypeName")
            }
            order("sampleTypeName", "asc")
        }
        return sampleTypes
    }

    /**
     * Projects, per {@link Individual} (pid) and sample type name, the number of registered (not withdrawn) lanes
     * grouped by {@link SeqType}.
     * @param project the project for filtering the result
     * @return one {@link SampleOverviewRegisteredLaneCountRow} per (pid, sampleTypeName, seqTypeId) combination
     */
    @CompileDynamic
    List<SampleOverviewRegisteredLaneCountRow> laneCountForSeqtypesPerPatientAndSampleType(Project project) {
        List<Object[]> lanes = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("pid")
                groupProperty("sampleTypeName")
                groupProperty("seqTypeId")
                sum("laneCount")
            }
        }
        return lanes.collect { Object[] it ->
            new SampleOverviewRegisteredLaneCountRow(it[0] as String, it[1] as String, it[2] as Long, it[3] as Long)
        }
    }

    /**
     * Projects, per {@link Individual} (pid) and sample type name, the number of withdrawn lanes grouped by
     * {@link SeqType}.
     * @param project the project for filtering the result
     * @return one {@link SampleOverviewWithdrawnLaneCountRow} per (pid, sampleTypeName, seqTypeId) combination
     */
    @CompileDynamic
    List<SampleOverviewWithdrawnLaneCountRow> withdrawnLaneCountForSeqTypesPerPatientAndSampleType(Project project) {
        List<Object[]> lanes = Sequence.withCriteria {
            eq("projectId", project?.id)
            eq("fileWithdrawn", true)
            projections {
                groupProperty("pid")
                groupProperty("sampleTypeName")
                groupProperty("seqTypeId")
                count()
            }
        }
        return lanes.collect { Object[] it ->
            new SampleOverviewWithdrawnLaneCountRow(it[0] as String, it[1] as String, it[2] as Long, it[3] as Long)
        }
    }

    /**
     * Projects the scalar fields the sample overview table needs for every processed BAM file that is the
     * in-project-folder file of its work package. Neither the {@link AbstractBamFile} nor its lazy associations
     * are hydrated.
     * @param project the project for filtering the result
     * @return one {@link SampleOverviewBamFileRow} per matching BAM file
     */
    @CompileDynamic
    List<SampleOverviewBamFileRow> abstractBamFilesInProjectFolder(Project project) {
        if (!project) {
            return []
        }
        return AbstractBamFile.executeQuery("""
                select
                    individual.pid,
                    sampleType.name,
                    seqType.id,
                    pipeline.id,
                    bamFile.numberOfMergedLanes,
                    bamFile.coverage,
                    bamFile.withdrawn
                from AbstractBamFile bamFile
                    join bamFile.workPackage workPackage
                    join workPackage.sample sample
                    join sample.individual individual
                    join sample.sampleType sampleType
                    join individual.project project
                    join workPackage.seqType seqType
                    join workPackage.pipeline pipeline
                where
                    project = :project
                    and workPackage.bamFileInProjectFolder = bamFile
                    and bamFile.fileOperationStatus = :fileOperationStatus
                """, [project: project, fileOperationStatus: AbstractBamFile.FileOperationStatus.PROCESSED]).collect { Object[] it ->
            new SampleOverviewBamFileRow(
                    it[0] as String,
                    it[1] as String,
                    it[2] as Long,
                    it[3] as Long,
                    it[4] as Integer,
                    it[5] as Double,
                    it[6] as boolean,
            )
        }
    }

    /**
     * Projects the (pid, sampleTypeName) of every {@link Sample} of the project so that samples without any
     * sequencing data still appear as a row in the sample overview. The {@link Sample} entity is not hydrated.
     * @param project the project for filtering the result
     * @return one {@link SampleOverviewSampleRow} per sample of the project
     */
    @CompileDynamic
    List<SampleOverviewSampleRow> samplesOfProject(Project project) {
        if (!project) {
            return []
        }
        return Sample.executeQuery("""
                select individual.pid, sampleType.name
                from Sample sample
                    join sample.individual individual
                    join sample.sampleType sampleType
                where individual.project = :project
                """, [project: project]).collect { Object[] it ->
            new SampleOverviewSampleRow(it[0] as String, it[1] as String)
        }
    }
}
