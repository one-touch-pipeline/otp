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
package de.dkfz.tbi.otp.workflow.bamImport

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteClusterPipelineJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class BamImportCopyJob extends AbstractExecuteClusterPipelineJob implements BamImportShared {

    @Autowired
    ProcessingOptionService processingOptionService

    @Override
    protected List<String> createScripts(WorkflowStep workflowStep) {
        ExternallyProcessedBamFile bamFile = getBamFile(workflowStep)
        BamImportInstance importInstance = getImportInstance(bamFile)

        if (importInstance.linkOperation.linkSource) {
            return []
        }

        String moduleLoader = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)
        String samtoolsActivation = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_SAMTOOLS)
        String groovyActivation = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY)
        String samtoolsCommand = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_SAMTOOLS)
        String groovyCommand = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_GROOVY)

        Path maxReadLengthScript = configService.toolsPath.resolve('bamMaxReadLength.groovy')

        Path sourceBam = externallyProcessedBamFileService.getSourceBamFilePath(bamFile)
        Path sourceBai = externallyProcessedBamFileService.getSourceBaiFilePath(bamFile)
        Path sourceBaseDir = externallyProcessedBamFileService.getSourceBaseDirFilePath(bamFile)

        Path targetBam = externallyProcessedBamFileService.getBamFile(bamFile)
        Path targetBai = externallyProcessedBamFileService.getBaiFile(bamFile)
        Path targetBaseDir = externallyProcessedBamFileService.getImportFolder(bamFile)

        Path bamMaxReadLengthFile = externallyProcessedBamFileService.getBamMaxReadLengthFile(bamFile)

        String updateBaseDir = "sed -e 's#${sourceBaseDir}#${targetBaseDir}#'"

        String md5sumBam
        if (bamFile.md5sum) {
            md5sumBam = "echo ${bamFile.md5sum}  ${targetBam} > ${targetBam}.md5sum"
        } else {
            md5sumBam = "md5sum ${sourceBam} | ${updateBaseDir} > ${targetBam}.md5sum"
        }

        String furtherFilesSource = bamFile.furtherFiles.collect {
            sourceBaseDir.resolve(it)
        }.join(' ')

        String furtherFilesMd5sumCheck = furtherFilesSource ? """\
md5sum `find -L ${furtherFilesSource} -type f` | ${updateBaseDir} > ${targetBaseDir}/md5sum.md5sum
md5sum -c ${targetBaseDir}/md5sum.md5sum\
""".stripIndent() : ""

        String furtherFilesTarget = bamFile.furtherFiles.collect {
            targetBaseDir.resolve(it)
        }.join(' ')

        String furtherFilesCopy = bamFile.furtherFiles.collect { String relativePath ->
            Path sourceFurtherFile = sourceBaseDir.resolve(relativePath)
            Path targetFurtherFile = targetBaseDir.resolve(relativePath)
            return "mkdir -p -m 2750 ${targetFurtherFile.parent}\n" +
                    "cp -HLR ${sourceFurtherFile} ${targetFurtherFile}"
        }.join("\n")

        return ["""
set -o pipefail
set -v

${moduleLoader}
${samtoolsActivation}
${groovyActivation}

if [ -e "${targetBam}" ]; then
    echo "File ${targetBam} already exists."
    rm -rf ${targetBam}* ${furtherFilesTarget}
fi

mkdir -p -m 2750 ${targetBaseDir}
# copy and calculate max read length at the same time
cat ${sourceBam} | tee ${targetBam} | ${samtoolsCommand} view - | ${groovyCommand} ${maxReadLengthScript} > ${bamMaxReadLengthFile}
cp -HL ${sourceBai} ${targetBai}

${furtherFilesCopy}

cd ${targetBaseDir}
${md5sumBam}
md5sum -c ${targetBam}.md5sum

md5sum ${sourceBai} | ${updateBaseDir} > ${targetBai}.md5sum
md5sum -c ${targetBai}.md5sum

${furtherFilesMd5sumCheck}
""".toString()]
    }
}
