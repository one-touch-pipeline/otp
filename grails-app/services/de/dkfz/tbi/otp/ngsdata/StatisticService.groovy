package de.dkfz.tbi.otp.ngsdata


class StatisticService {

    ProjectGroupService projectGroupService

    ProjectService projectService

    public List projectDateSortAfterDate(ProjectGroup projectGroup) {
        List seq = Sequence.withCriteria {
            projections {
                if (projectGroup) {
                    List<Project> projects = projectService.projectByProjectGroup(projectGroup)
                    'in'("projectId", projects*.id)
                }
                groupProperty("projectName")
                min("dateCreated", "minDate")
            }
            order ("minDate")
        }
        return seq
    }

    public List sampleCountPerSequenceType(ProjectGroup projectGroup) {
        List seq = AggregateSequences.withCriteria {
            projections {
                if (projectGroup) {
                    List<Project> projects = projectService.projectByProjectGroup(projectGroup)
                    'in'("projectId", projects*.id)
                }
                groupProperty("seqTypeName")
                count("sampleId")
            }
            order("seqTypeName")
        }
    }

    public List patientsCountPerSequenceType(ProjectGroup projectGroup) {
        List seq = AggregateSequences.withCriteria {
            projections {
                if (projectGroup) {
                    List<Project> projects = projectService.projectByProjectGroup(projectGroup)
                    'in'("projectId", projects*.id)
                }
                groupProperty("seqTypeName")
                countDistinct("mockPid")
            }
            order("seqTypeName")
        }
        return seq
    }

    public List laneCountPerDay(ProjectGroup projectGroup) {
        List<Project> projects
        if (projectGroup) {
            projects = projectService.projectByProjectGroup(projectGroup)
        } else {
            projects = Project.list()
        }

        List seq = Sequence.executeQuery("\
            select year(s.dateCreated) as year, month(s.dateCreated) as month, day(s.dateCreated) as day, count(laneId) as laneCount \
            from Sequence s \
            where s.projectId in :projectIds \
            group by year(s.dateCreated), month(s.dateCreated), day(s.dateCreated) \
            order by year, month, day", [projectIds: projects*.id])
        return seq
    }

    public List projectCountPerSequenceType(ProjectGroup projectGroup) {
        List seq = Sequence.withCriteria {
            projections {
                if (projectGroup) {
                    List<Project> projects = projectService.projectByProjectGroup(projectGroup)
                    'in'("projectId", projects*.id)
                }
                groupProperty("seqTypeName")
                countDistinct("projectId")
            }
            order ("seqTypeName")
        }
        return seq
    }

    public List seqTypeByProject(Project project) {
        List seq = Sequence.withCriteria {
            eq("projectId", project.id)
            projections { groupProperty("seqTypeId") }
            order ("seqTypeId")
        }
        return seq
    }

    public List sampleTypeCountBySeqType(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("seqTypeName")
                count("sampleId")
            }
            order ("seqTypeName")
        }
        return seq
    }

    public List sampleTypeCountByPatient(Project project) {
        List seq = Sequence.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("mockPid")
                countDistinct("sampleId")
            }
            order ("mockPid")
        }
        return seq
    }

    public List laneCountPerDateByProject(Project project) {
        List seq = Sequence.executeQuery("\
            select year(s.dateCreated) as year, month(s.dateCreated) as month, day(s.dateCreated) as day, count(s.laneId) as laneCount \
            from Sequence s \
            WHERE s.projectId = ${project.id} \
            group by year(s.dateCreated), month(s.dateCreated), day(s.dateCreated) \
            order by year, month, day")
        return seq
    }
}
