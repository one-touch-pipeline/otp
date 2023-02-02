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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic

import de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.TableCellValue

@CompileStatic
@Transactional(readOnly = true)
class CellRangerQaOverviewService extends AbstractQaOverviewService {

    final static List<? extends ColumnDefinition> COLUMN_DEFINITIONS = [
            new PropertyColumnDefinition('mergingWorkPackage', 'expectedCells', 'expectedCells'),
            new PropertyColumnDefinition('mergingWorkPackage', 'enforcedCells', 'enforcedCells'),
            new PropertyColumnDefinition('qa', 'estimatedNumberOfCells', 'estimatedNumberOfCells'),
            new PropertyColumnDefinition('qa', 'meanReadsPerCell', 'meanReadsPerCell'),
            new PropertyColumnDefinition('qa', 'medianGenesPerCell', 'medianGenesPerCell'),
            new PropertyColumnDefinition('qa', 'numberOfReads', 'numberOfReads'),
            new PropertyColumnDefinition('qa', 'validBarcodes', 'validBarcodes'),
            new PropertyColumnDefinition('qa', 'sequencingSaturation', 'sequencingSaturation'),
            new PropertyColumnDefinition('qa', 'q30BasesInBarcode', 'q30BasesInBarcode'),
            new PropertyColumnDefinition('qa', 'q30BasesInRnaRead', 'q30BasesInRnaRead'),
            new PropertyColumnDefinition('qa', 'q30BasesInUmi', 'q30BasesInUmi'),
            new PropertyColumnDefinition('qa', 'readsMappedConfidentlyToIntergenicRegions', 'readsMappedConfidentlyToIntergenicRegions'),
            new PropertyColumnDefinition('qa', 'readsMappedConfidentlyToIntronicRegions', 'readsMappedConfidentlyToIntronicRegions'),
            new PropertyColumnDefinition('qa', 'readsMappedConfidentlyToExonicRegions', 'readsMappedConfidentlyToExonicRegions'),
            new PropertyColumnDefinition('qa', 'readsMappedConfidentlyToTranscriptome', 'readsMappedConfidentlyToTranscriptome'),
            new PropertyColumnDefinition('qa', 'fractionReadsInCells', 'fractionReadsInCells'),
            new PropertyColumnDefinition('qa', 'totalGenesDetected', 'totalGenesDetected'),
            new PropertyColumnDefinition('qa', 'medianUmiCountsPerCell', 'medianUmiCountsPerCell'),
            new PropertyColumnDefinition('referenceGenome', 'name', 'referenceGenomeName'),
            new PropertyColumnDefinition('toolName', 'name', 'toolNameName'),
            new PropertyColumnDefinition('referenceGenomeIndex', 'indexToolVersion', 'indexToolVersion'),
            new PropertyColumnDefinition('config', 'programVersion', 'programVersion'),
    ].asImmutable()

    final static List<String> QC_KEY = [
            'expectedCells',
            'enforcedCells',
            'estimatedNumberOfCells',
            'meanReadsPerCell',
            'medianGenesPerCell',
            'numberOfReads',
            'validBarcodes',
            'sequencingSaturation',
            'q30BasesInBarcode',
            'q30BasesInRnaRead',
            'q30BasesInUmi',
            'readsMappedConfidentlyToIntergenicRegions',
            'readsMappedConfidentlyToIntronicRegions',
            'readsMappedConfidentlyToExonicRegions',
            'readsMappedConfidentlyToTranscriptome',
            'fractionReadsInCells',
            'totalGenesDetected',
            'medianUmiCountsPerCell',
    ].asImmutable()

    @Override
    Class<? extends AbstractQualityAssessment> qaClass() {
        return CellRangerQualityAssessment
    }

    @Override
    List<SeqType> supportedSeqTypes() {
        return SeqTypeService.cellRangerAlignableSeqTypes
    }

    @Override
    protected List<String> additionalJoinDomains() {
        return [
                "join mergingWorkPackage.referenceGenomeIndex referenceGenomeIndex",
                "join referenceGenomeIndex.referenceGenome referenceGenome",
                "join referenceGenomeIndex.toolName toolName",
                "join mergingWorkPackage.config config",
        ]
    }

    @Override
    protected List<String> additionalDomainHierarchies() {
        return []
    }

    @Override
    protected List<String> restriction() {
        return []
    }

    @Override
    protected Map<String, ? extends Object> additionalParameters() {
        return [:]
    }

    @Override
    protected List<ColumnDefinition> columnDefinitionList() {
        return COLUMN_DEFINITIONS
    }

    @Override
    protected List<String> qcKeys() {
        return QC_KEY
    }

    @Override
    protected Map<String, Double> qcKeysMap(Map<String, ?> qaMap) {
        return [:]
    }

    @Override
    protected Map<String, ?> extractSpecificValues(Project project, Map<String, ?> qaMap) {
        return [
                cellRangerVersion: "${qaMap.programVersion}",
                referenceGenome  : "${qaMap.referenceGenomeName} ${qaMap.toolNameName} ${qaMap.indexToolVersion}",
                summary          : new TableCellValue(
                        archived: project.archived,
                        value: "Summary",
                        linkTarget: "_blank",
                        link: linkGenerator.link(
                                action: "viewCellRangerSummary",
                                params: [
                                        "singleCellBamFile.id": qaMap.bamId,
                                ],
                        ).toString()
                ),
        ]
    }
}
