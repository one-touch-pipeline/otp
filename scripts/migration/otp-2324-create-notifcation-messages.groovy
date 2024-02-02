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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Stream

// input

// create notification for runs created before this date (= when the NotificationJobs were activated)
Date date = Date.from(Instant.parse('2007-12-03T10:15:30.00Z'))

// script
AlignmentInfoService alignmentInfoService = ctx.alignmentInfoService
MessageSourceService messageSourceService = ctx.messageSourceService
RoddyBamFileService roddyBamFileService = ctx.roddyBamFileService

Pattern pattern = Pattern.compile(/declare -x    (?<key>\w+)=(?<value>.*)/)

List<Workflow> workflows = Workflow.findAllByNameInListAndDeprecatedDateIsNull([PanCancerWorkflow.WORKFLOW, WgbsWorkflow.WORKFLOW, RnaAlignmentWorkflow.WORKFLOW])
List<WorkflowRun.State> states = [WorkflowRun.State.SUCCESS, WorkflowRun.State.LEGACY]

WorkflowRun.withTransaction {

    WorkflowRun.findAllByNotificationTextIsNullAndDateCreatedLessThanAndWorkflowInListAndStateInList(date, workflows, states).each { WorkflowRun workflowRun ->
        WorkflowArtefact workflowArtefact = workflowRun.outputArtefacts[AlignmentWorkflow.OUTPUT_BAM]
        if (!workflowArtefact) {
            println "No workflow artefact for WR ${workflowRun.id} 😓"
            return
        }
        if (workflowArtefact.artefact.isEmpty()) {
            println "No concrete artefact (BAM file) for WR ${workflowRun.id} 😓"
            return
        }
        RoddyBamFile roddyBamFile = workflowArtefact.artefact.get() as RoddyBamFile
        if (!roddyBamFile.isMostRecentBamFile()) {
            println "BAM file isn't most receent: WR ${workflowRun.id} BF ${roddyBamFile.id} 😓"
            return
        }
        if (roddyBamFile != roddyBamFile.mergingWorkPackage.processableBamFileInProjectFolder) {
            println "BAM file isn't in MWP: WR ${workflowRun.id} BF ${roddyBamFile.id} 😓"
            return
        }
        if (!roddyBamFile.roddyExecutionDirectoryNames) {
            println "BAM file doesn't have execution dirs: WR ${workflowRun.id} BF ${roddyBamFile.id} 😓"
            return
        }
        if ((!Files.isReadable(roddyBamFileService.getFinalExecutionDirectories(roddyBamFile).last())) ||
                (!Files.isReadable(roddyBamFileService.getBaseDirectory(roddyBamFile)))) {
            println "BAM file dir is not readable: WR ${workflowRun.id} BF ${roddyBamFile.id} 😓"
            return
        }
        Stream<Path> dir = Files.list(roddyBamFileService.getFinalExecutionDirectories(roddyBamFile).last())
        Path parameterFile = dir.find { it.fileName.toString().endsWith('.parameters') }
        dir.close()
        Path runtimeConfigFile = roddyBamFileService.getBaseDirectory(roddyBamFile).resolve('runtimeConfig.sh')

        Path file
        if (parameterFile) {
            file = parameterFile
        } else if (Files.exists(runtimeConfigFile)) {
            file = runtimeConfigFile
        } else {
            println "No files found for WR ${workflowRun.id} BF ${roddyBamFile.id} 😓"
            return
        }

        Map<String, String> config = [:]
        file.text.eachLine { line ->
            Matcher m = pattern.matcher(line)
            if (m.matches()) {
                config[m.group('key')] = m.group('value')
            }
        }

        RoddyAlignmentInfo alignmentInfo
        String version = workflowRun.workflowVersion?.workflowVersion ?: roddyBamFile.config.programVersion
        try {
            alignmentInfo = alignmentInfoService.generateRoddyAlignmentInfo(config, roddyBamFile.project, roddyBamFile.seqType, version)
        } catch (ParsingException e) {
            println "Couldn't extract values for WR ${workflowRun.id} BF ${roddyBamFile.id} 😓"
            return
        }

        StringBuilder builder = new StringBuilder()
        builder << messageSourceService.createMessage("notification.template.alignment.processing", [
                seqType           : roddyBamFile.seqType.displayNameWithLibraryLayout,
                individuals       : "",
                referenceGenome   : roddyBamFile.referenceGenome,
                alignmentProgram  : alignmentInfo.alignmentProgram,
                alignmentParameter: alignmentInfo.alignmentParameter,
        ])
        Map<String, Object> codeAndParams = alignmentInfo.alignmentSpecificMessageAttributes
        builder << messageSourceService.createMessage(codeAndParams.code as String, codeAndParams.params as Map)

        workflowRun.notificationText = builder.toString()
        workflowRun.save(flush: true)
    }
}
[]
