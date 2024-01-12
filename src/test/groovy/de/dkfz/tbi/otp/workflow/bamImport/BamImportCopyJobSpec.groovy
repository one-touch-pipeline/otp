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
package de.dkfz.tbi.otp.workflow.bamImport

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.ExternallyProcessedBamFileService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.BamImportWorkflowDomainFactory
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

class BamImportCopyJobSpec extends Specification implements DataTest, DomainFactoryCore, BamImportWorkflowDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalMergingWorkPackage,
                ImportProcess,
                ProcessingOption,
        ]
    }

    @TempDir
    Path tempDir

    BamImportCopyJob job

    WorkflowStep workflowStep

    private ExternallyProcessedBamFile epmbfWithMd5sum

    private TestConfigService configService
    static private final String SUB_DIRECTORY = "subDirectory"

    Path importDir
    Path sourcePath
    Path sourceBamFile
    Path sourceBaiFile
    Path importedBamPath
    Path importedBaiPath
    Path importedMRLPath

    void setupData(boolean hasMd5, ImportProcess.LinkOperation linkOperation) {
        sourcePath = tempDir.resolve("source")
        importDir = tempDir.resolve("import")

        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateBamImportWorkflowWorkflow(),
                ]),
        ])
        epmbfWithMd5sum = createBamFile(
                md5sum: hasMd5 ? HelperUtils.randomMd5sum : null,
                furtherFiles: [SUB_DIRECTORY]
        )

        createImportProcess(
                externallyProcessedBamFiles: [epmbfWithMd5sum],
                linkOperation: linkOperation,
        )

        sourceBamFile = sourcePath.resolve(epmbfWithMd5sum.bamFileName)
        sourceBaiFile = sourcePath.resolve(epmbfWithMd5sum.baiFileName)
        importedBamPath = importDir.resolve(epmbfWithMd5sum.bamFileName)
        importedBaiPath = importDir.resolve(epmbfWithMd5sum.baiFileName)
        importedMRLPath = importDir.resolve("${epmbfWithMd5sum.bamFileName}.maxReadLength")

        CreateFileHelper.createFile(sourceBamFile)
        CreateFileHelper.createFile(sourceBaiFile)

        job = new BamImportCopyJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, BamImportCopyJob.de_dkfz_tbi_otp_workflow_bamImport_BamImportShared__OUTPUT_ROLE) >> epmbfWithMd5sum
            0 * _
        }

        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): tempDir.resolve("root").toString(),
                (OtpProperty.PATH_TOOLS)       : "/tool-dir",
        ])

        job.configService = configService
        job.processingOptionService = new ProcessingOptionService()
        job.logService = Mock(LogService)

        job.externallyProcessedBamFileService = Mock(ExternallyProcessedBamFileService) {
            getSourceBamFilePath(epmbfWithMd5sum) >> sourceBamFile
            getSourceBaiFilePath(epmbfWithMd5sum) >> sourceBaiFile
            getSourceBaseDirFilePath(epmbfWithMd5sum) >> sourcePath

            getBamFile(epmbfWithMd5sum) >> importedBamPath
            getBaiFile(epmbfWithMd5sum) >> importedBaiPath
            getImportFolder(epmbfWithMd5sum) >> importDir

            getBamMaxReadLengthFile(epmbfWithMd5sum) >> importedMRLPath
        }

        findOrCreateProcessingOption(
                name: COMMAND_LOAD_MODULE_LOADER,
                type: null,
                value: "",
        )
        findOrCreateProcessingOption(
                name: COMMAND_ACTIVATION_SAMTOOLS,
                type: null,
                value: "module load samtools"
        )
        findOrCreateProcessingOption(
                name: COMMAND_SAMTOOLS,
                type: null,
                value: "samtools"
        )
        findOrCreateProcessingOption(
                name: COMMAND_ACTIVATION_GROOVY,
                type: null,
                value: "module load groovy"
        )
        findOrCreateProcessingOption(
                name: COMMAND_GROOVY,
                type: null,
                value: "groovy"
        )
    }

    void "test createScripts, when files have to be copied, have a md5Sum and not exist already, then create copy script using given md5sum"() {
        given:
        setupData(true, ImportProcess.LinkOperation.COPY_AND_KEEP)

        expect:
        job.createScripts(workflowStep).first() ==~ generateCopyScriptTemplate("echo [0-9a-f]{32}  \\S+.bam > \\S+.bam.md5sum")
    }

    void "test createScripts, when files have to be copied, not have a md5Sum and not exist already, then create copy script using calculate md5sum"() {
        given:
        setupData(false, ImportProcess.LinkOperation.COPY_AND_KEEP)

        expect:
        job.createScripts(workflowStep).first() ==~ generateCopyScriptTemplate("md5sum .* \\| sed -e 's#.*#.*#' > .*")
    }

    void "test createScripts, when files have to be linked, then don't create copy script"() {
        given:
        setupData(true, ImportProcess.LinkOperation.LINK_SOURCE)

        expect:
        job.createScripts(workflowStep) == []
    }

    @SuppressWarnings('ConsecutiveBlankLines') // is part of a script
    private static String generateCopyScriptTemplate(String hasOrNotMd5SumCmd) {
        return """
set -o pipefail
set -v


module load samtools
module load groovy

if \\[ -e "\\S+" \\]; then
    echo "File \\S+.bam already exists."
    rm -rf \\S+\\* \\S+
fi

mkdir -p -m 2750 \\S+
# copy and calculate max read length at the same time
cat \\S+.bam \\| tee \\S+.bam \\| samtools view - \\| groovy /tool-dir/bamMaxReadLength.groovy > \\S+.bam.maxReadLength
cp -HL \\S+.bam.bai \\S+.bam.bai

mkdir -p -m 2750 \\S+
cp -HLR \\S+ \\S+

cd \\S+
${hasOrNotMd5SumCmd}
md5sum -c \\S+.bam.md5sum

md5sum .*.bam.bai \\| sed -e 's#.*#.*#' > \\S+.bam.bai.md5sum
md5sum -c \\S+.bam.bai.md5sum

md5sum `find -L .* -type f` \\| sed -e 's#.*#.*#' > \\S+\\/md5sum.md5sum
md5sum -c \\S+\\/md5sum.md5sum
"""
    }
}
