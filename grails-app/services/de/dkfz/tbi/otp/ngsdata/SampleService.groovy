package de.dkfz.tbi.otp.ngsdata

class SampleService {

    /**
     * returns the number of samples for specified period if given
     */
    int getCountOfSamplesForSpecifiedPeriodAndProjects(Date startDate = null, Date endDate = null, List<Project> projects) {
        return DataFile.createCriteria().get {
            projections {
                seqTrack {
                    sample {
                        countDistinct('id')
                        individual {
                            'in'('project', projects)
                        }
                    }
                }
                if (startDate && endDate) {
                    between('dateCreated', startDate, endDate)
                }
            }
        } as int
    }
}