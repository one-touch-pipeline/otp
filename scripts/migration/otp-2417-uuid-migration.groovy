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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RnaRoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.filestore.BaseFolder
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflow.bamImport.BamImportWorkflow
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.TimeFormats

import java.nio.file.FileSystem
import java.nio.file.Path

// input

// the name of the projects
List projectNames = """
#projectName1
#projectName2

""".split("\n")*.trim().findAll {
    it && !it.startsWith('#')
}

// the name of the new workflow in the new system choose one
List<String> workflows = [
        //DataInstallationWorkflow.WORKFLOW, //NYI
        //BashFastQcWorkflow.WORKFLOW, //NYI
        //PanCancerWorkflow.WORKFLOW, //NYI
        //WgbsWorkflow.WORKFLOW, //NYI
        RnaAlignmentWorkflow.WORKFLOW,
        //BamImportWorkflow.WORKFLOW //NYI
]

// samples per script
int workflowRunsPerScript = 100

// Run this script w/o modification of database if set to true
boolean dryRun = true

// script
FilestoreService filestoreService = ctx.filestoreService
ConcreteArtefactService concreteArtefactService = ctx.concreteArtefactService
RnaRoddyBamFileService rnaRoddyBamFileService = ctx.rnaRoddyBamFileService
FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService
ConfigService configService = ctx.configService
ProcessingOptionService processingOptionService = ctx.processingOptionService

List<Project> projects = Project.findAllByNameInList(projectNames)
Workflow workflow = CollectionUtils.exactlyOneElement(Workflow.findAllByNameInListAndDeprecatedDateIsNull(workflows))

List<WorkflowRun> workflowRuns = WorkflowRun.findAllByProjectInListAndWorkflowAndWorkFolderIsNull(projects, workflow)
FileSystem fileSystem = fileSystemService.remoteFileSystem
final Path scriptOutputDirectory = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve('migrationToUUID').resolve("${TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())}_${workflow.name.replace(" ", "_")}")
fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(scriptOutputDirectory)
fileService.setPermission(scriptOutputDirectory, FileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)

int amountStringBuilders = Math.ceil(workflowRuns.size() / workflowRunsPerScript)
List<StringBuilder> outputStringBuilders = []
(0..amountStringBuilders - 1).each {
    outputStringBuilders[it] = new StringBuilder("""
#!/bin/bash

set -ve
""")
}

WorkflowRun.withTransaction {
    BaseFolder baseFolder = filestoreService.findAnyWritableBaseFolder()
    String group = processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_USER_LINUX_GROUP)
    workflowRuns.eachWithIndex { WorkflowRun workflowRun, int index ->
        switch (workflow.name) {
            case DataInstallationWorkflow.WORKFLOW:
            case BashFastQcWorkflow.WORKFLOW:
            case PanCancerWorkflow.WORKFLOW:
            case WgbsWorkflow.WORKFLOW:
            case BamImportWorkflow.WORKFLOW:
                assert false: "Not yet Implemented"
                break
            case RnaAlignmentWorkflow.WORKFLOW:
                RnaRoddyBamFile rnaRoddyBamFile = concreteArtefactService.<RnaRoddyBamFile> getOutputArtefact(workflowRun, RnaAlignmentWorkflow.OUTPUT_BAM, false)
                if (!rnaRoddyBamFile) {
                    println "Skipping ${workflowRun}, since no RnaRoddyBamFile was found"
                    return
                } else if (rnaRoddyBamFile.workPackage.bamFileInProjectFolder != rnaRoddyBamFile) {
                    println "Skipping ${workflowRun}, since RnaRoddyBamFile is not bamFileInProjectFolder"
                    return
                }

                Path oldBaseDir = rnaRoddyBamFileService.getBaseDirectory(rnaRoddyBamFile)
                Path oldWorkDir = rnaRoddyBamFileService.getWorkDirectory(rnaRoddyBamFile)
                WorkFolder workFolder = filestoreService.createWorkFolder(baseFolder)
                Path workFolderPath = filestoreService.getWorkFolderPath(workFolder)
                filestoreService.attachWorkFolder(workflowRun, workFolder)

                String output = """
# create the first two uuid fragment dirs with 2755 if they dont already exist
if [ ! -d '${workFolderPath.parent.parent}' ]; then
mkdir -p ${workFolderPath.parent.parent}
chgrp ${group} ${workFolderPath.parent.parent}
chmod ${fileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING} ${workFolderPath.parent.parent}
fi
if [ ! -d '${workFolderPath.parent}' ]; then
mkdir -p ${workFolderPath.parent}
chgrp ${group} ${workFolderPath.parent}
chmod ${fileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING} ${workFolderPath.parent}
fi

# create the last uuid fragment (the new workdir) with 2750 permissions
mkdir ${workFolderPath}
chgrp ${workflowRun.project.unixGroup} ${workFolderPath}
chmod ${fileService.DEFAULT_DIRECTORY_PERMISSION_STRING} ${workFolderPath}

# copy files to new workfolder
rsync -ulrptg ${oldWorkDir}/ ${workFolderPath}/

# force create new links between vbp structure and uuid structure
ln -rsf ${workFolderPath}/* ${oldBaseDir}/

# delete all .merging folders 
rm -rf ${oldBaseDir}/.merging*

"""
                outputStringBuilders[index % amountStringBuilders].append(output)
        }
    }

    println "Files will be created under: ${scriptOutputDirectory}"
    outputStringBuilders.eachWithIndex { StringBuilder stringBuilder, int index ->
        fileService.createFileWithContent(scriptOutputDirectory.resolve("script_${index.toString().padLeft(3, "0")}.sh"), stringBuilder.toString())
    }

    assert !dryRun: "DRY RUN: transaction intentionally failed to rollback changes"
}
[]
