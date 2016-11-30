package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import org.hibernate.criterion.*
import org.hibernate.sql.*
import org.springframework.security.access.prepost.*

class SnvService {

    ProjectService projectService

    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or hasPermission(returnObject.project, read)")
    SnvCallingInstance getSnvCallingInstance(long id) {
        return SnvCallingInstance.get(id)
    }

    List getSnvCallingInstancesForProject(String projectName) {
        return SnvCallingInstance.withCriteria {
            eq ('withdrawn', false)
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
            resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
            projections {
                samplePair {
                    mergingWorkPackage1 {
                        sample {
                            individual {
                                property('id', 'individualId')
                                property('pid', 'individualPid')
                            }
                            sampleType {
                                property('name', 'sampleType1')
                            }
                        }
                        seqType {
                            property('displayName', 'seqType')
                            property('name', 'seqTypeName')
                        }
                        libraryPreparationKit(JoinType.LEFT_OUTER_JOIN.getJoinTypeValue()) {
                            property('shortDisplayName', 'libPrepKit1')
                        }
                    }
                    mergingWorkPackage2 {
                        sample {
                            sampleType {
                                property('name', 'sampleType2')
                            }
                        }
                        libraryPreparationKit(JoinType.LEFT_OUTER_JOIN.getJoinTypeValue()) {
                            property('shortDisplayName', 'libPrepKit2')
                        }
                    }
                }
                property('processingState', 'snvProcessingState')
                property('id', 'snvInstanceId')
                property('instanceName', 'snvInstanceName')
            }
        }
    }
}
