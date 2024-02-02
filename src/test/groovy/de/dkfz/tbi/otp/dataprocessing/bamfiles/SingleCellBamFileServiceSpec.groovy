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
package de.dkfz.tbi.otp.dataprocessing.bamfiles

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFileService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.Paths

class SingleCellBamFileServiceSpec extends Specification implements ServiceUnitTest<SingleCellBamFileService>, DataTest, CellRangerFactory {

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
        ]
    }

    SingleCellBamFile bamFile
    String testDir

    void setup() {
        bamFile = createBamFile()
        testDir = "/base-dir"
        service.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get("/base-dir")
        }
    }

    void "test getWorkDirectory"() {
        expect:
        service.getWorkDirectory(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}"
    }

    void "test buildWorkDirectoryName"() {
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

    void "test getSampleDirectory"() {
        expect:
        service.getSampleDirectory(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/cell-ranger-input/${bamFile.singleCellSampleName}"
    }

    void "test getOutputDirectory"() {
        expect:
        service.getOutputDirectory(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${bamFile.singleCellSampleName}"
    }

    void "test getResultDirectory"() {
        expect:
        service.getResultDirectory(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${bamFile.singleCellSampleName}/outs"
    }

    void "test getFileMappingForLinks"() {
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

    void "test getFinalInsertSizeFile"() {
        when:
        service.getFinalInsertSizeFile(bamFile)

        then:
        thrown(UnsupportedOperationException)
    }

    void "test getPathForFurtherProcessing, should return final directory"() {
        expect:
        service.getPathForFurtherProcessing(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${bamFile.bamFileName}"
    }

    void "test getPathForFurtherProcessing, when not set in mergingWorkPackage, should throw exception"() {
        given:
        bamFile.fileOperationStatus = AbstractBamFile.FileOperationStatus.DECLARED
        bamFile.md5sum = null
        bamFile.save(flush: true)
        bamFile.mergingWorkPackage.bamFileInProjectFolder = null
        bamFile.mergingWorkPackage.save(flush: true)

        when:
        service.getPathForFurtherProcessing(bamFile)

        then:
        thrown(IllegalStateException)
    }

    void "test getQualityAssessmentCsvFile"() {
        expect:
        service.getQualityAssessmentCsvFile(bamFile).toString() ==
                "/base-dir/${bamFile.workDirectoryName}/${bamFile.singleCellSampleName}/outs/metrics_summary.csv"
    }

    void "test getWebSummaryResultFile"() {
        expect:
        service.getWebSummaryResultFile(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${bamFile.singleCellSampleName}/outs/web_summary.html"
    }
}
