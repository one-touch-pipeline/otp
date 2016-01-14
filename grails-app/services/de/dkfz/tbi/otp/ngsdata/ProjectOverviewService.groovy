package de.dkfz.tbi.otp.ngsdata

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import de.dkfz.tbi.otp.dataprocessing.*

class ProjectOverviewService {

    List overviewProjectQuery(projectName) {
        Project project = Project.findByName(projectName)
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            property("individualId")
            property("mockPid")
            property("sampleTypeName")
            property("seqTypeDisplayName")
            property("libraryLayout")
            property("seqPlatformId")
            property("seqCenterName")
            property("laneCount")
            property("sum_N_BasePairsGb")
            property("projectName")
            order ("mockPid")
            order ("sampleTypeName")
            order ("seqTypeDisplayName")
            order ("libraryLayout")
            order ("seqPlatformId")
            order ("seqCenterName")
            order ("laneCount")
        }
        List queryList = []
        for (def track in seq) {
            def queryListSingleRow = [
                track.mockPid,
                track.sampleTypeName,
                track.seqTypeDisplayName,
                track.libraryLayout,
                track.seqCenterName,
                SeqPlatform.get(track.seqPlatformId).toString(),
                track.laneCount,
                track.sum_N_BasePairsGb
            ]
            queryList.add(queryListSingleRow)
        }
        return queryList
    }

    public List patientsAndSamplesGBCountPerProject(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("seqTypeDisplayName")
                groupProperty("libraryLayout")
                countDistinct("mockPid")
                count("sampleId")
                sum("sum_N_BasePairsGb")
            }
            order ("seqTypeDisplayName")
        }
        return seq
    }

    public Long individualCountByProject(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections { countDistinct("mockPid") }
        }
        return seq[0]
    }

    public List sampleTypeNameCountBySample(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("sampleTypeName")
                countDistinct("sampleId")
            }
        }
        return seq
    }

    public List centerNameRunId(Project project){
        List seq = Sequence.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("seqCenterName")
                countDistinct("runId")
            }
            order ("seqCenterName")
        }
        return seq
    }

    public List centerNameRunIdLastMonth(Project project){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -6)
        Date date = cal.getTime()
        List seq = Sequence.withCriteria {
            eq("projectId", project.id)
            gt("dateExecuted", date)
            projections {
                groupProperty("seqCenterName")
                countDistinct("runId")
            }
            order("seqCenterName")
        }
        return seq
    }
    /**
     * @param project the project for filtering the result
     *  @return all SeqTypes used in the project
     */
    public List<SeqType> seqTypeByProject(Project project){
        List<Long> seqTypeIds = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("seqTypeId")
            }
        }
        List<SeqType> seqTypes = []
        if(seqTypeIds) {
            seqTypes = SeqType.withCriteria {
                'in'("id", seqTypeIds)
                order("name")
                order("libraryLayout")
            }
        }
        return seqTypes
    }
    /**
     * @param project the project for filtering the result
     *  @return all MockPids used in the project
     */
    public List<String> mockPidByProject(Project project){
        List<String> mockPids = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("mockPid")
            }
        }
        return mockPids
    }
    public List<String> sampleTypeByProject(Project project){
        List<String> sampleTypes = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("sampleTypeName")
            }
            order("sampleTypeName", "asc")
        }
        return sampleTypes
    }


    /**
     * fetch and return all combination of individual(mockPid) and sampleTypeName as list.
     * <br> Example: [[patient1, sampleType1],[patient1, sampleType2]...]
     *
     * @param project the project for filtering the result
     * @return all combination of individual(mockPid) and sampleTypeName as list
     */
    public List<List<String>> overviewMockPidSampleType(Project project){
        List<List<String>> mockPidSampleTypes = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("mockPid")
                groupProperty("sampleTypeName")
            }
        }
        return mockPidSampleTypes
    }

    /**
     * fetch and return all combination of {@link Individual} (as mockpid) and {@link SampleType} with the first {@link SampleIdentifier}
     * as list.
     *<br> Example:[[patient1, sampleType1, SampleIdentifier1],[patient1, sampleType2, SampleIdentifier2],[patient1, sampleType3, SampleIdentifier3]...]
     * @param project the project for filtering the result
     * @return all combination of name of individual(mockPid) and sampleTypeName with the first SampleIdentifier as list
     *
     */
    public List<Object> overviewSampleIdentifier(Project project){
        List<Object> sampleIdentifiers = SampleIdentifier.withCriteria {
            projections {
                sample {
                    individual {
                        eq("project", project)
                        groupProperty("mockPid")
                    }
                    sampleType {
                        groupProperty("name")
                    }
                }
                min("name")
            }
        }
        return sampleIdentifiers
    }

    /**
     * fetch and return all combination of {@link Individual} (as mockpid) and of Sample type name with the number of lanes depend of {@link SeqType}
     *as list.
     *<br> Example:[
     *[mockPid: patient1, sampleTypeName: sampleTypeName1, seqType: sampleType1, laneCount: laneCount1],
     *[mockPid: patient1, sampleTypeName: sampleTypeName1, seqType: sampleType2, laneCount: laneCount2],
     *[mockPid: patient1, sampleTypeName: sampleTypeName2, seqType: sampleType1, laneCount: laneCount3],
     *[mockPid: patient2, sampleTypeName: sampleTypeName1, seqType: sampleType1, laneCount: laneCount4],
     *...]
     * @param project the project for filtering the result
     * @return all combination of  name of {@link Individual}(mockPid) and sampleTypeName with with the number of lanes depend of {@link SeqType}  as list
     */
    public List<Map> laneCountForSeqtypesPerPatientAndSampleType(Project project){
        List lanes = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("mockPid")
                groupProperty("sampleTypeName")
                groupProperty("seqTypeId")
                sum("laneCount")
            }
        }

        Map<Long, SeqType> seqTypes = [:]

        List<Map> ret = lanes.collect {
            SeqType seqType = seqTypes[it[2]]
            if (!seqType) {
                seqType = SeqType.get(it[2])
                seqTypes.put(it[2], seqType)
            }
            [
                mockPid: it[0],
                sampleTypeName: it[1],
                seqType: seqType,
                laneCount: it[3],
            ]
        }
        return ret
    }

    public Collection<AbstractMergedBamFile> abstractMergedBamFilesInProjectFolder(Project project) {
        return AbstractMergedBamFile.executeQuery("""
from
        AbstractMergedBamFile abstractMergedBamFile
where
        workPackage.sample.individual.project = :project
        and workPackage.bamFileInProjectFolder = abstractMergedBamFile
        and fileOperationStatus = :fileOperationStatus
""", [project: project, fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED])
    }

    public List listReferenceGenome(Project project) {
        return ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
    }

    @PreAuthorize("hasRole('ROLE_MMML_MAPPING')")
    public List tableForMMMLMapping(){
        def seq = Individual.withCriteria {
            project {
                'in'("name", ["MMML", "MMML_XP"])
            }
            projections {
                property("id")
                property("mockFullName")
                property("internIdentifier")
            }
            order("id", "desc")
        }
        return seq
    }
}

