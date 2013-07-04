package de.dkfz.tbi.otp.ngsdata

class ProjectOverviewService {

    List overviewProjectQuery(projectName) {
        Project project = Project.findByName(projectName)
        List seq = AggregateSequences.withCriteria {
           eq("projectId", project.id)
               property("individualId")
               property("mockPid")
               property("sampleTypeId")
               property("seqTypeId")
               property("seqPlatformId")
               property("seqCenterName")
               property("laneCount")
               property("sum_N_BasePairsGb")
               property("projectName")
               order ("mockPid")
               order ("sampleTypeId")
               order ("seqTypeId")
               order ("seqPlatformId")
               order ("seqCenterName")
               order ("laneCount")
            }
        List queryList=[]
        for (def track in seq) {
            def queryListSingleRow = [
                track.mockPid,
                SampleType.get(track.sampleTypeId).name,
                SeqType.get(track.seqTypeId).name,
                SeqType.get(track.seqTypeId).libraryLayout,
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
                countDistinct("mockFullName")
                count("sampleId")
                sum("sum_N_BasePairsGb")
            }
            order ("seqTypeName")
        }
        List queryList = []

        for (def track in seq) {
            def queryListSingl = [
                track[0],
                track[1],
                track[2],
                track[3]
            ]
            queryList.add(queryListSingl)
        }
        return queryList
    }

    public Long individualCountByProject(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections { countDistinct("mockFullName") }
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

}

