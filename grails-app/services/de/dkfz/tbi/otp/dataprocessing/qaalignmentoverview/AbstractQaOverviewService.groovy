/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.qaalignmentoverview

import asset.pipeline.grails.LinkGenerator
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.util.TimeFormats

@CompileStatic
@Transactional(readOnly = true)
abstract class AbstractQaOverviewService {

    final static List<String> BASE_JOIN_DOMAINS = [
            "join qa.qualityAssessmentMergedPass qaPass",
            "join qaPass.abstractMergedBamFile bamFile",
            "join bamFile.workPackage mergingWorkPackage",
            "join mergingWorkPackage.sample sample",
            "join mergingWorkPackage.pipeline pipeline",
            "join sample.sampleType sampleType",
            "join sample.individual individual",
            "join individual.project project",
            "left outer join bamFile.comment comment",
            "left outer join mergingWorkPackage.config config",
            "left outer join bamFile.config config2",
            "left outer join bamFile.workflowArtefact artefact",
            "left outer join artefact.producedBy run",
            "left outer join run.workflowVersion version",
    ].asImmutable()

    final static List<String> BASE_RESTRICTION = [
            "project = :project",
            "bamFile.fileOperationStatus = 'PROCESSED'",
            "bamFile.qualityAssessmentStatus = 'FINISHED'",
            "bamFile.withdrawn = false",
            "mergingWorkPackage.seqType = :seqType",
            "mergingWorkPackage.bamFileInProjectFolder = bamFile",
    ].asImmutable()

    final static List<? extends ColumnDefinition> BASE_COLUMN_DEFINITIONS = [
            new PropertyColumnDefinition('bamFile', 'id', 'bamId'),
            new PropertyColumnDefinition('bamFile', 'version', 'dbVersion'),
            new PropertyColumnDefinition('bamFile', 'dateFromFileSystem', 'dateFromFileSystem'),
            new PropertyColumnDefinition('bamFile', 'withdrawn', 'withdrawn'),
            new PropertyColumnDefinition('bamFile', 'qcTrafficLightStatus', 'qcStatus'),
            new PropertyColumnDefinition('mergingWorkPackage', 'id', 'mergingWorkPackageId'),
            new PropertyColumnDefinition('pipeline', 'name', 'pipelineName'),
            new PropertyColumnDefinition('sampleType', 'name', 'sampleType'),
            new PropertyColumnDefinition('individual', 'pid', 'pid'),
            new PropertyColumnDefinition('individual', 'id', 'individualId'),
            new PropertyColumnDefinition('comment', 'comment', 'qcComment'),
            new PropertyColumnDefinition('comment', 'author', 'qcAuthor'),
            new PropertyColumnDefinition('qa', 'totalMappedReadCounter', 'totalMappedReadCounter'),
            new PropertyColumnDefinition('qa', 'duplicates', 'duplicates'),
            new PropertyColumnDefinition('qa', 'withMateMappedToDifferentChr', 'withMateMappedToDifferentChr'),
            new PropertyColumnDefinition('qa', 'properlyPaired', 'properlyPaired'),
            new PropertyColumnDefinition('qa', 'singletons', 'singletons'),
            new PropertyColumnDefinition('qa', 'totalReadCounter', 'totalReadCounter'),
            new PropertyColumnDefinition('qa', 'pairedInSequencing', 'pairedInSequencing'),
            new PropertyColumnDefinition('qa', 'insertSizeMedian', 'insertSizeMedian'),
            new PropertyColumnDefinition('config', 'programVersion', 'programVersion1'),
            new PropertyColumnDefinition('config2', 'programVersion', 'programVersion2'),
            new PropertyColumnDefinition('version', 'workflowVersion', 'workflowVersion'),
    ].asImmutable()

    final static List<String> KEY_BASE = [
            'bamId',
            'sampleType',
            'withdrawn',
            'qcStatus',
            'qcComment',
            'qcAuthor',
            'dbVersion',
    ].asImmutable()

    final static List<String> QC_KEY_BASE = [
            "percentMappedReads",
            "percentDuplicates",
            "percentDiffChr",
            "percentProperlyPaired",
            "percentSingletons",
    ].asImmutable()

    QcThresholdService qcThresholdService

    QcStatusCellService qcStatusCellService

    @Autowired
    LinkGenerator linkGenerator

    abstract Class<? extends AbstractQualityAssessment> qaClass()

    abstract List<SeqType> supportedSeqTypes()

    abstract protected List<String> additionalJoinDomains()

    abstract protected List<String> additionalDomainHierarchies()

    abstract protected List<String> restriction()

    abstract protected List<ColumnDefinition> columnDefinitionList()

    abstract protected Map<String, ? extends Object> additionalParameters()

    abstract protected List<String> qcKeys()

    abstract protected Map<String, Double> qcKeysMap(Map<String, ?> qaMap)

    abstract protected Map<String, ?> extractSpecificValues(Project project, Map<String, ?> qaMap)

    final String createQuery(boolean includeSample) {
        String selectSample = includeSample ? "\n    and sample = :sample" : ""

        List<? extends ColumnDefinition> columnDefinitions = []
        columnDefinitions.addAll(BASE_COLUMN_DEFINITIONS)
        columnDefinitions.addAll(columnDefinitionList())
        String projections = columnDefinitions*.toHql().join(',\n        ')
        String baseHierarchy = (["${qaClass().name} qa".toString()] + BASE_JOIN_DOMAINS + additionalJoinDomains()).join("\n    ")
        String from = ([baseHierarchy] + additionalDomainHierarchies()).join(",\n    ")
        String restrictions = (BASE_RESTRICTION + restriction()).join("\n    and ")

        final String hql = """
            |select
            |    new map(
            |        ${projections}
            |    )
            |from
            |    ${from}
            |where
            |    ${restrictions}${selectSample}
        """.stripMargin()
        return hql
    }

    Map<String, ? extends Object> createParameterMap(Project project, SeqType seqType, Sample sample = null) {
        Map<String, ? extends Object> parameters = [:]
        parameters.put("project", project)
        parameters.put("seqType", seqType)
        if (sample) {
            parameters.put("sample", sample)
        }
        parameters.putAll(additionalParameters())

        return parameters
    }

    void addDerivedData(List<Map<String, ?>> qaMapList) {
        qaMapList.each { Map<String, ?> qaMap ->
            qaMap.percentMappedReads = calculatePercentage((Number) qaMap.totalMappedReadCounter, (Number) qaMap.totalReadCounter)
            qaMap.percentDuplicates = calculatePercentage((Number) qaMap.duplicates, (Number) qaMap.totalReadCounter)
            qaMap.percentDiffChr = calculatePercentage((Number) qaMap.withMateMappedToDifferentChr, (Number) qaMap.totalReadCounter)
            qaMap.percentProperlyPaired = calculatePercentage((Number) qaMap.properlyPaired, (Number) qaMap.pairedInSequencing)
            qaMap.percentSingletons = calculatePercentage((Number) qaMap.singletons, (Number) qaMap.totalReadCounter)
        }
    }

    protected Double calculatePercentage(Number numerator, Number denominator) {
        return ((numerator != null && denominator) ? (100.0 * numerator.doubleValue() / denominator.doubleValue()) : null)
    }

    List<Map<String, ?>> createList(Project project, SeqType seqType, List<Map<String, ?>> qaMapList) {
        QcThresholdService.ThresholdColorizer thresholdColorizer =
                qcThresholdService.createThresholdColorizer(project, seqType, (Class<QcTrafficLightValue>) qaClass())

        List<Map<String, ?>> result = []
        qaMapList.each { Map<String, ?> qaMap ->
            Map<String, ?> qcTableRow = [:]
            KEY_BASE.each {
                qcTableRow[it] = qaMap[it]
            }
            qcTableRow += [
                    pid               : new TableCellValue(
                            value: qaMap.pid,
                            link: linkGenerator.link(
                                    controller: 'individual',
                                    action: 'show',
                                    id: qaMap.individualId,
                            ).toString()
                    ),
                    kit               : new TableCellValue(
                            value: qaMap.libraryPreparationKitName ?: "-",
                            warnColor: null,
                            link: null,
                            tooltip: qaMap.libraryPreparationKitName ?: ""
                    ),
                    dateFromFileSystem: TimeFormats.DATE.getFormattedDate((Date) qaMap.dateFromFileSystem),
                    qcStatusGui       : qcStatusCellService.generateQcStatusCell(qaMap),
                    pipelineName      : ((Pipeline.Name) qaMap.pipelineName).displayName,
            ]
            Map<String, Double> keyMapBase = [
                    insertSizeMedian: (Double) qaMap.readLength,
            ]
            qcTableRow += thresholdColorizer.colorize(QC_KEY_BASE, qaMap)
            qcTableRow += thresholdColorizer.colorize(qcKeys(), qaMap)
            qcTableRow += thresholdColorizer.colorize(keyMapBase, qaMap)
            qcTableRow += thresholdColorizer.colorize(qcKeysMap(qaMap), qaMap)
            qcTableRow += extractSpecificValues(project, qaMap)
            result << qcTableRow
        }
        return result
    }
}
