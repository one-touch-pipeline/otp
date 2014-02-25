package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize

class ProjectOverviewService {

    List overviewProjectQuery(projectName) {
        Project project = Project.findByName(projectName)
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            property("individualId")
            property("mockPid")
            property("sampleTypeName")
            property("seqTypeName")
            property("libraryLayout")
            property("seqPlatformId")
            property("seqCenterName")
            property("laneCount")
            property("sum_N_BasePairsGb")
            property("projectName")
            order ("mockPid")
            order ("sampleTypeName")
            order ("seqTypeName")
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
                track.seqTypeName,
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
                groupProperty("seqTypeName")
                groupProperty("libraryLayout")
                countDistinct("mockPid")
                count("sampleId")
                sum("sum_N_BasePairsGb")
            }
            order ("seqTypeName")
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
        List<SeqType> seqTypes = SeqType.withCriteria {
            'in'("id", seqTypeIds)
            order("name")
            order("libraryLayout")
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
    public List<List<String>> overviewSampleIdentifier(Project project){
        List<List<String>> sampleIdentifiers = SampleIdentifier.withCriteria {
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
     *[patient1, sampleType1, laneCount1],
     *[patient1, sampleType2, laneCount2],
     *[patient2, sampleType2, laneCount3],
     *...]
     * @param project the project for filtering the result
     * @param seqType the seqType for filtering the result
     * @return all combination of  name of {@link Individual}(mockPid) and sampleTypeName with with the number of lanes depend of {@link SeqType}  as list
     */
    public List laneCountPerPatientAndSampleType(Project project, SeqType seqType){
        List lanesPerSeqTypes = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            eq("seqTypeId", seqType.id)
            projections {
                groupProperty("mockPid")
                groupProperty("sampleTypeName")
                sum("laneCount")
            }
        }
        return lanesPerSeqTypes
    }


    @PreAuthorize("hasRole('ROLE_MMML_MAPPING')")
    public List tableForMMMLMapping(){
        def seq = Individual.withCriteria {
            project {
                eq("name", "MMML")
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
