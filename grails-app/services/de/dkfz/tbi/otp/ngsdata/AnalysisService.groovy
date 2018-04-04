package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import org.hibernate.criterion.*
import org.hibernate.sql.*
import org.springframework.security.access.prepost.*

class AnalysisService {

    ProjectService projectService

    List getCallingInstancesForProject(Class<BamFilePairAnalysis> callingInstance, String projectName) {
        Project proj = projectService.getProjectByName(projectName)
        if (!proj) {
            return []
        }
        return callingInstance.withCriteria {
            eq('withdrawn', false)
            samplePair {
                mergingWorkPackage1 {
                    sample {
                        individual {
                            project {
                                eq('name', proj.name)
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
                config {
                    property('pluginVersion', "pluginVersion")
                }
                property('processingState', "processingState")
                property('id', "instanceId")
                property('dateCreated', "dateCreated")
            }
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#callingInstance.project, read)")
    List<File> getFiles(BamFilePairAnalysis callingInstance, PlotType plotType) {
        if (!callingInstance) {
            return null
        }

        List<File> files = []

        switch (plotType) {
            case PlotType.ACESEQ_GC_CORRECTED:
            case PlotType.ACESEQ_QC_GC_CORRECTED:
            case PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR:
            case PlotType.ACESEQ_WG_COVERAGE:
                if ((callingInstance as AceseqInstance).getPlot(plotType).exists()) {
                    files.add((callingInstance as AceseqInstance).getPlot(plotType))
                }
                break
            case PlotType.ACESEQ_ALL:
            case PlotType.ACESEQ_EXTRA:
                if ((callingInstance as AceseqInstance).getPlots(plotType)) {
                    files = (callingInstance as AceseqInstance).getPlots(plotType)
                }
                break
            case PlotType.SOPHIA:
            case PlotType.SNV:
            case PlotType.INDEL:
                if (callingInstance.getCombinedPlotPath().exists()) {
                    files.add(callingInstance.getCombinedPlotPath())
                }
                break
            case PlotType.INDEL_TINDA:
                if (callingInstance.getCombinedPlotPathTiNDA().exists()) {
                    files.add(callingInstance.getCombinedPlotPathTiNDA())
                }
                break
            default:
                throw new RuntimeException("${callingInstance.class.name} is not a valid calling instance")
        }

        if (files.isEmpty()) {
            return null
        }

        return files
    }
}

enum PlotType {
    SNV,
    INDEL,
    INDEL_TINDA,
    SOPHIA,
    ACESEQ_GC_CORRECTED,
    ACESEQ_QC_GC_CORRECTED,
    ACESEQ_WG_COVERAGE,
    ACESEQ_TCN_DISTANCE_COMBINED_STAR,
    ACESEQ_ALL,
    ACESEQ_EXTRA,
}
