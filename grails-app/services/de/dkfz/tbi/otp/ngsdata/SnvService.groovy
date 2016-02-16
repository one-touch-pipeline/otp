package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates


class SnvService {

    ProjectService projectService

    List getSnvCallingInstanceForProject(String projectName) {
        return SnvCallingInstance.createCriteria().list {
            samplePair {
                mergingWorkPackage1 {
                    sample {
                        individual {
                            project {
                                eq ('name', projectService.getProjectByName(projectName).name)
                            }
                        }
                    }
                }
            }
            projections {
                samplePair {
                    mergingWorkPackage1 {
                        sample {
                            individual {
                                property('id')
                                property('pid')
                            }
                            sampleType {
                                property('name')
                            }
                        }
                        seqType {
                            property('displayName')
                        }
                    }
                    mergingWorkPackage2 {
                        sample {
                            sampleType {
                                property('name')
                            }
                        }
                    }
                }
                property('processingState')
                property('id')
                property('instanceName')
            }
        }
    }
}
