package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import org.hibernate.criterion.*
import org.hibernate.sql.*

class AnalysisService {

    ProjectService projectService

    List getCallingInstancesForProject(Class<BamFilePairAnalysis> callingInstance, String projectName) {
        String callingInstanceType

        switch (callingInstance) {
            case SnvCallingInstance:
                callingInstanceType = "snv"
                break
            case IndelCallingInstance:
                callingInstanceType = "indel"
                break
            default:
                throw new RuntimeException("${callingInstance.name} is not a valid calling instance")
        }

        return callingInstance.withCriteria {
            eq('withdrawn', false)
            samplePair {
                mergingWorkPackage1 {
                    sample {
                        individual {
                            project {
                                eq('name', projectService.getProjectByName(projectName).name)
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
                property('processingState', "${callingInstanceType}ProcessingState")
                property('id', "${callingInstanceType}InstanceId")
                property('instanceName', "${callingInstanceType}InstanceName")
            }
        }
    }

    File checkFile(BamFilePairAnalysis callingInstance) {
        if (!callingInstance) {
            return null
        }
        if (callingInstance instanceof SnvCallingInstance) {
            if (!callingInstance.getAllSNVdiagnosticsPlots().absoluteDataManagementPath.exists()) {
                return null
            }
            return callingInstance.getAllSNVdiagnosticsPlots().absoluteDataManagementPath
        } else if (callingInstance instanceof IndelCallingInstance) {
            if (!callingInstance.getCombinedPlotPath().exists()) {
                return null
            }
            return callingInstance.getCombinedPlotPath()
        } else {
            throw new RuntimeException("${callingInstance.class.name} is not a valid calling instance")
        }
    }
}
