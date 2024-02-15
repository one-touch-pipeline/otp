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
package de.dkfz.tbi.otp.infrastructure.alignment

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFileService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact

import java.nio.file.Paths

class CellRangerWorkFileServiceSpec extends Specification implements ServiceUnitTest<CellRangerWorkFileService>, DataTest, CellRangerFactory, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                CellRangerConfig,
                CellRangerMergingWorkPackage,
                FastqFile,
                FastqImportInstance,
                FileType,
                Individual,
                ReferenceGenomeProjectSeqType,
                Sample,
                SampleType,
                SingleCellBamFile,
                WorkflowArtefact,
        ]
    }

    SingleCellBamFile bamFile

    void setupNonUuid() {
        bamFile = createBamFile()
        service.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get("/base-dir")
        }
    }

    void setupUuid() {
        bamFile = createBamFile([
                workflowArtefact: createWorkflowArtefact([
                        producedBy: createWorkflowRun([
                                workFolder: createWorkFolder(),
                        ]),
                ]),
        ])
        service.filestoreService = Mock(FilestoreService) {
            1 * getWorkFolderPath(_) >> Paths.get("/base-dir-uuid")
        }
    }

    void "test getInsertSizeFile"() {
        given:
        setupNonUuid()

        when:
        service.getInsertSizeFile(bamFile)

        then:
        thrown(UnsupportedOperationException)
    }

    void "test getDirectoryPath"() {
        given:
        setupNonUuid()

        expect:
        service.getDirectoryPath(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}"
    }

    void "test buildWorkDirectoryName"() {
        given:
        setupNonUuid()

        CellRangerMergingWorkPackage mwp = createMergingWorkPackage(expectedCells: expectecCells, enforcedCells: enforcedCells)

        expect:
        service.buildWorkDirectoryName(mwp, 1234) ==
                "RG_${mwp.referenceGenome.name}_TV_${mwp.referenceGenomeIndex.toolWithVersion.replace(" ", "-")}_" +
                "EC_${expectecCells ?: "-"}_FC_${enforcedCells ?: "-"}_PV_${mwp.config.programVersion}_ID_1234"

        where:
        expectecCells | enforcedCells || _
        12            | null          || _
        null          | 34            || _
        null          | null          || _
    }

    void "test getSampleDirectory"() {
        given:
        setupNonUuid()

        expect:
        service.getSampleDirectory(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/cell-ranger-input/${bamFile.id}"
    }

    void "test getOutputDirectory"() {
        given:
        setupNonUuid()

        expect:
        service.getOutputDirectory(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${bamFile.id}"
    }

    void "test getResultDirectory"() {
        given:
        setupNonUuid()

        expect:
        service.getResultDirectory(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${bamFile.id}/outs"
    }

    void "test getFileMappingForLinks"() {
        given:
        setupNonUuid()

        expect:
        service.getFileMappingForLinks(bamFile) == [
                "web_summary.html"             : "web_summary.html",
                "metrics_summary.csv"          : "metrics_summary.csv",
                (bamFile.bamFileName)          : "possorted_genome_bam.bam",
                (bamFile.baiFileName)          : "possorted_genome_bam.bam.bai",
                (bamFile.md5SumFileName)       : "possorted_genome_bam.md5sum",
                "filtered_feature_bc_matrix.h5": "filtered_feature_bc_matrix.h5",
                "raw_feature_bc_matrix.h5"     : "raw_feature_bc_matrix.h5",
                "molecule_info.h5"             : "molecule_info.h5",
                "cloupe.cloupe"                : "cloupe.cloupe",
                "filtered_feature_bc_matrix"   : "filtered_feature_bc_matrix",
                "raw_feature_bc_matrix"        : "raw_feature_bc_matrix",
                "analysis"                     : "analysis",
        ]
    }

    void "test getLinkedResultFiles"() {
        given:
        setupNonUuid()

        expect:
        service.getLinkedResultFiles(bamFile)*.toString() == [
                "/base-dir/${bamFile.workDirectoryName}/web_summary.html",
                "/base-dir/${bamFile.workDirectoryName}/metrics_summary.csv",
                "/base-dir/${bamFile.workDirectoryName}/${bamFile.bamFileName}",
                "/base-dir/${bamFile.workDirectoryName}/${bamFile.baiFileName}",
                "/base-dir/${bamFile.workDirectoryName}/${bamFile.md5SumFileName}",
                "/base-dir/${bamFile.workDirectoryName}/filtered_feature_bc_matrix.h5",
                "/base-dir/${bamFile.workDirectoryName}/raw_feature_bc_matrix.h5",
                "/base-dir/${bamFile.workDirectoryName}/molecule_info.h5",
                "/base-dir/${bamFile.workDirectoryName}/cloupe.cloupe",
                "/base-dir/${bamFile.workDirectoryName}/filtered_feature_bc_matrix",
                "/base-dir/${bamFile.workDirectoryName}/raw_feature_bc_matrix",
                "/base-dir/${bamFile.workDirectoryName}/analysis",
        ]
    }

    void "test getQualityAssessmentCsvFile"() {
        given:
        setupNonUuid()

        expect:
        service.getQualityAssessmentCsvFile(bamFile).toString() ==
                "/base-dir/${bamFile.workDirectoryName}/${bamFile.id}/outs/metrics_summary.csv"
    }

    void "test getWebSummaryResultFile"() {
        given:
        setupNonUuid()

        expect:
        service.getWebSummaryResultFile(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${bamFile.id}/outs/web_summary.html"
    }

    void "test getDirectoryPath for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getDirectoryPath(bamFile).toString() == "/base-dir-uuid"
    }

    @Unroll
    void "test buildWorkDirectoryName, check that returned name is correct"() {
        given:
        CellRangerMergingWorkPackage mwp = createMergingWorkPackage(expectedCells: expectecCells, enforcedCells: enforcedCells)

        expect:
        service.buildWorkDirectoryName(mwp, 1234) ==
                "RG_${mwp.referenceGenome.name}_TV_${mwp.referenceGenomeIndex.toolWithVersion.replace(" ", "-")}_" +
                "EC_${expectecCells ?: "-"}_FC_${enforcedCells ?: "-"}_PV_${mwp.config.programVersion}_ID_1234"

        where:
        expectecCells | enforcedCells || _
        12            | null          || _
        null          | 34            || _
        null          | null          || _
    }

    void "test getSampleDirectory for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getSampleDirectory(bamFile).toString() == "/base-dir-uuid/cell-ranger-input/${bamFile.id}"
    }

    void "test getOutputDirectory for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getOutputDirectory(bamFile).toString() == "/base-dir-uuid/${bamFile.id}"
    }

    void "test getResultDirectory for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getResultDirectory(bamFile).toString() == "/base-dir-uuid/${bamFile.id}/outs"
    }

    void "test getLinkedResultFiles for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getLinkedResultFiles(bamFile)*.toString() == [
                "/base-dir-uuid/web_summary.html",
                "/base-dir-uuid/metrics_summary.csv",
                "/base-dir-uuid/${bamFile.bamFileName}",
                "/base-dir-uuid/${bamFile.baiFileName}",
                "/base-dir-uuid/${bamFile.md5SumFileName}",
                "/base-dir-uuid/filtered_feature_bc_matrix.h5",
                "/base-dir-uuid/raw_feature_bc_matrix.h5",
                "/base-dir-uuid/molecule_info.h5",
                "/base-dir-uuid/cloupe.cloupe",
                "/base-dir-uuid/filtered_feature_bc_matrix",
                "/base-dir-uuid/raw_feature_bc_matrix",
                "/base-dir-uuid/analysis",
        ]
    }

    void "test getQualityAssessmentCsvFile for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getQualityAssessmentCsvFile(bamFile).toString() ==
                "/base-dir-uuid/${bamFile.id}/outs/metrics_summary.csv"
    }

    void "test getWebSummaryResultFile for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getWebSummaryResultFile(bamFile).toString() == "/base-dir-uuid/${bamFile.id}/outs/web_summary.html"
    }
}
