package de.dkfz.tbi.otp.ngsdata

import grails.validation.Validateable

class StatisticService {

    public List projectDateSortAfterDate() {
        List seq = Sequence.withCriteria {
            projections {
                groupProperty("projectName")
                min("dateCreated", "minDate")
            }
            order ("minDate")
        }
        return seq
    }

    public List laneCountPerDay() {
        List seq = Sequence.executeQuery("\
            select year(s.dateCreated) as year, month(s.dateCreated) as month, day(s.dateCreated) as day, count(laneId) as laneCount \
            from Sequence s \
            group by year(s.dateCreated), month(s.dateCreated), day(s.dateCreated) \
            order by year, month, day")
        return seq
    }

    public List projectCountPerSequenceType() {
        List seq = Sequence.withCriteria {
            projections {
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
            projections {
                groupProperty("seqTypeId")
            }
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
                groupProperty("mockFullName")
                countDistinct("sampleId")
            }
            order ("mockFullName")
        }
        return seq
    }

    public List laneNumberByProject(Project project, SeqType seqType) {
        List seq = Sequence.withCriteria {
            eq("projectId", project.id)
            eq("seqTypeId", seqType.id)
            projections {
                groupProperty("mockFullName")
                groupProperty("sampleTypeName")
                count("laneId")
            }
            order ("mockFullName")
            order ("sampleTypeName")
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

