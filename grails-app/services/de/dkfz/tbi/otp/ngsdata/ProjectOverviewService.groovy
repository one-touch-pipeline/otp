package de.dkfz.tbi.otp.ngsdata

class ProjectOverviewService {

    List overviewProjectQuery(projectName) {
        Project project = Project.findByName(projectName)

        List sequenceProjections = Sequence.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("individualId")
                groupProperty("mockPid")
                groupProperty("sampleTypeId")
                groupProperty("seqTypeId")
                groupProperty("seqPlatformId")
                groupProperty("seqCenterName")
                sum("nBasePairs")
                groupProperty("projectName")
            }
            order ("mockPid")
            order ("sampleTypeId")
            order ("seqTypeId")
        }

        List queryList = []

        for (def track in sequenceProjections) {
            def queryListSingleRow = [
                track[1],
                SampleType.get(track[2]).name,
                SeqType.get(track[3]).name,
                SeqType.get(track[3]).libraryLayout,
                track[5],
                SeqPlatform.get(track[4]).toString(),
                Math.floor(track[6] / 1e9)
            ]
            queryList.add(queryListSingleRow)
        }
        return queryList
    }

    public List patientsAndSamplesGBCountPerProject(Project project) {
        List seq = Sequence.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("seqTypeName")
                countDistinct("mockFullName")
                count("sampleId")
                sum("nBasePairs")
            }
            order ("seqTypeName")
        }
        List queryList = []

        for (def track in seq) {
            def queryListSingl = [
                track[0],
                track[1],
                track[2],
                Math.floor(track[3] / 1e9)
            ]
            queryList.add(queryListSingl)
        }
        return queryList
    }
}

