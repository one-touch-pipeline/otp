/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.query.api.Criteria
import org.hibernate.criterion.CriteriaSpecification
import org.hibernate.sql.JoinType
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AbstractSnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.exceptions.NotSupportedException
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.util.TimeFormats

import java.nio.file.Files
import java.nio.file.Path

@CompileDynamic
@Transactional
@Slf4j
abstract class AbstractAnalysisResultsService<T extends BamFilePairAnalysis> {

    AceseqService aceseqService
    FileSystemService fileSystemService
    IndelCallingService indelCallingService
    ProjectService projectService
    SnvCallingService snvCallingService
    SophiaService sophiaService
    LinkGenerator grailsLinkGenerator

    List getCallingInstancesForProject(Project proj) {
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
            appendQcFetchingCriteria(delegate)
            resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
            projections {
                appendQcProjectionCriteria(delegate)
                workflowArtefact(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                    producedBy(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                        property('state', 'runState')
                    }
                }
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
                            property('name', 'libPrepKit1')
                        }
                    }
                    mergingWorkPackage2 {
                        sample {
                            sampleType {
                                property('name', 'sampleType2')
                            }
                        }
                        libraryPreparationKit(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                            property('name', 'libPrepKit2')
                        }
                    }
                }
                config {
                    property("programVersion", 'version')
                }
                property('processingState', 'processingState')
                property('id', "instanceId")
                property('dateCreated', 'dateCreated')
            }
        }

        return results.collect { Map properties ->
            properties.putAll(mapQcData(properties))

            Collection<String> libPrepKitNames
            if (SeqTypeNames.fromSeqTypeName(properties.seqTypeName)?.isWgbs()) {
                assert properties.libPrepKit1 == null && properties.libPrepKit2 == null
                T instance = instanceClass.get(properties.instanceId as Long)
                libPrepKitNames = instance.containedSeqTracks*.libraryPreparationKit*.name
            } else {
                libPrepKitNames = [(String) properties.libPrepKit1, (String) properties.libPrepKit2]
            }
            properties.with {
                configFile = getConfigLinks(properties.instanceId as long, properties.runState as WorkflowRun.State)
                libPrepKits = libPrepKitNames.unique().collect { it ?: 'unknown' }.join(", <br>")
                remove('libPrepKit1')
                remove('libPrepKit2')
                sampleTypes = "${properties.sampleType1} \u2013 ${properties.sampleType2}"
                remove('sampleType1')
                remove('sampleType2')
                remove('runState')
                dateCreated = TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(dateCreated)
                if (processingState != AnalysisProcessingStates.FINISHED) {
                    remove('instanceId')
                }
                return it
            }
            return properties
        }
    }

    abstract Class<T> getInstanceClass()

    abstract Map mapQcData(Map data)

    abstract void appendQcFetchingCriteria(Criteria criteria)

    abstract void appendQcProjectionCriteria(Criteria criteria)

    List<Map> getConfigLinks(long analysisId, WorkflowRun.State runState) {
        return (!analysisId || !runState || runState == WorkflowRun.State.LEGACY) ? [[value: 'N/A']] :
                [[
                         value     : 'View',
                         linkTarget: '_blank',
                         link      : grailsLinkGenerator.link(
                                 action: 'viewConfigFile',
                                 params: ['analysisInstance.id': analysisId],
                         ),
                 ], [
                         value: 'Download',
                         link : grailsLinkGenerator.link(
                                 action: 'viewConfigFile',
                                 params: ['analysisInstance.id': analysisId, 'to': 'DOWNLOAD'],
                         ),
                 ],
                ]
    }

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
                throw new NotSupportedException("${callingInstance.class.name} is not a valid calling instance")
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
