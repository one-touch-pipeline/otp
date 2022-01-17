/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import org.hibernate.criterion.CriteriaSpecification
import org.hibernate.sql.JoinType
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AbstractSnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.util.TimeFormats

import java.nio.file.Files
import java.nio.file.Path

@Transactional
abstract class AbstractAnalysisResultsService<T extends BamFilePairAnalysis> {

    AceseqService aceseqService
    FileSystemService fileSystemService
    IndelCallingService indelCallingService
    ProjectService projectService
    SnvCallingService snvCallingService
    SophiaService sophiaService

    List getCallingInstancesForProject(String projectName) {
        Project proj = projectService.getProjectByName(projectName)
        if (!proj) {
            return []
        }
        List results = instanceClass.withCriteria {
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
                        libraryPreparationKit(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                            property('shortDisplayName', 'libPrepKit1')
                        }
                    }
                    mergingWorkPackage2 {
                        sample {
                            sampleType {
                                property('name', 'sampleType2')
                            }
                        }
                        libraryPreparationKit(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                            property('shortDisplayName', 'libPrepKit2')
                        }
                    }
                }
                config {
                    property("programVersion", 'version')
                }
                property('processingState', "processingState")
                property('id', "instanceId")
                property('dateCreated', "dateCreated")
            }
        }

        return results.collect { Map properties ->
            T instance = instanceClass.get(properties.instanceId)
            properties.putAll(getQcData(instance))

            Collection<String> libPrepKitShortNames
            if (SeqTypeNames.fromSeqTypeName(properties.seqTypeName)?.isWgbs()) {
                assert properties.libPrepKit1 == null && properties.libPrepKit2 == null
                libPrepKitShortNames = instance.containedSeqTracks*.
                        libraryPreparationKit*.shortDisplayName
            } else {
                libPrepKitShortNames = [(String) properties.libPrepKit1, (String) properties.libPrepKit2]
            }
            properties.libPrepKits = libPrepKitShortNames.unique().collect { it ?: 'unknown' }.join(", <br>")
            properties.remove('libPrepKit1')
            properties.remove('libPrepKit2')
            properties.sampleTypes = "${properties.sampleType1} \u2013 ${properties.sampleType2}"
            properties.remove('sampleType1')
            properties.remove('sampleType2')
            properties.dateCreated = TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(properties.dateCreated)
            if (properties.processingState != AnalysisProcessingStates.FINISHED) {
                properties.remove('instanceId')
            }
            return properties
        }
    }

    abstract Class<T> getInstanceClass()

    abstract Map getQcData(T analysis)

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#callingInstance.project, 'OTP_READ_ACCESS')")
    List<Path> getFiles(BamFilePairAnalysis callingInstance, PlotType plotType) {
        if (!callingInstance) {
            return []
        }

        List<Path> filePaths = []

        switch (plotType) {
            case PlotType.ACESEQ_GC_CORRECTED:
            case PlotType.ACESEQ_QC_GC_CORRECTED:
            case PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR:
            case PlotType.ACESEQ_WG_COVERAGE:
                filePaths.add(aceseqService.getPlot(callingInstance as AceseqInstance, plotType))
                break
            case PlotType.ACESEQ_ALL:
            case PlotType.ACESEQ_EXTRA:
                filePaths.addAll(aceseqService.getPlots(callingInstance as AceseqInstance, plotType))
                break
            case PlotType.SOPHIA:
                filePaths.add(sophiaService.getCombinedPlotPath(callingInstance as SophiaInstance))
                break
            case PlotType.SNV:
                filePaths.add(snvCallingService.getCombinedPlotPath(callingInstance as AbstractSnvCallingInstance))
                break
            case PlotType.INDEL:
                filePaths.add(indelCallingService.getCombinedPlotPath(callingInstance as IndelCallingInstance))
                break
            case PlotType.INDEL_TINDA:
                filePaths.add(indelCallingService.getCombinedPlotPathTiNDA(callingInstance as IndelCallingInstance))
                break
            default:
                throw new RuntimeException("${callingInstance.class.name} is not a valid calling instance")
        }
        return filePaths.findAll { Path file ->
            Files.exists(file) && Files.isReadable(file)
        }
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
