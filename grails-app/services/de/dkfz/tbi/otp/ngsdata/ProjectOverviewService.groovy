package de.dkfz.tbi.otp.ngsdata

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
                countDistinct("mockFullName")
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

    public List centerNameRunId(Project project){
        List seq = Sequence.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("seqCenterName")
                count("runId")
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
                count("runId")
            }
            order("seqCenterName")
        }
        return seq
    }
}

