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

import io.swagger.client.wes.model.State

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.StackTraceUtils
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationConditionalFailJob
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowMessageLog
import de.dkfz.tbi.otp.workflowExecution.wes.*

import java.nio.file.*
import java.time.*

/**
 * Change it to generate more or less data sets of workflow runs.
 */
int numberOfDemoSets = 6


WorkflowRun.withNewTransaction {
    SeqType seqType = SeqType.findByName('EXAMPLE')
    SeqTrack seqTrack = SeqTrack.findBySeqType(seqType)
    Individual individual = seqTrack.individual
    Project project = individual.project
    ProcessingPriority priority = project.processingPriority

    String workflowName = "Example Workflow ${Workflow.count()}"
    String displayText = [
            project   : project.name,
            individual: individual.pid,
            seqType   : seqType.displayNameWithLibraryLayout,
            sampleType: seqTrack.sampleType,
            run       : seqTrack.run.name,
            laneId    : seqTrack.laneId,
    ].collect {
        [
                it.key,
                it.value,
        ].join(': ')
    }.join('\n')

    println "worklow name: ${workflowName}"

    Workflow workflow = CollectionUtils.atMostOneElement(Workflow.findAllByName(workflowName)) ?: new Workflow([
            name    : workflowName,
            beanName: DataInstallationWorkflow.simpleName.uncapitalize(),
            enabled : true
    ]).save(flush: true)

    Closure createArtefact = { WorkflowRun producedBy, String outputRole ->
        WorkflowArtefact workflowArtefact = new WorkflowArtefact([
                state       : WorkflowArtefact.State.SUCCESS,
                producedBy  : producedBy,
                outputRole  : outputRole,
                artefactType: ArtefactType.FASTQ,
                displayName : "artefact ${WorkflowArtefact.count() + 1}\n${displayText}",
        ]).save(flush: true)

        new SeqTrack([
                sample               : seqTrack.sample,
                seqType              : seqTrack.seqType,
                run                  : seqTrack.run,
                laneId               : SeqTrack.count() + 1,
                sampleIdentifier     : "sample_${SeqTrack.count()}",
                pipelineVersion      : seqTrack.pipelineVersion,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                libraryPreparationKit: seqTrack.libraryPreparationKit,
                kitInfoReliability   : InformationReliability.KNOWN,
                workflowArtefact     : workflowArtefact,
        ]).save(flush: true)
        return workflowArtefact
    }

    Closure createInputArtefact = { WorkflowRun input, String inputRole ->
        return new WorkflowRunInputArtefact([
                workflowRun     : input,
                role            : inputRole,
                workflowArtefact: createArtefact(null, null)
        ]).save(flush: true)
    }

    Closure createWorkflowRun = { WorkflowRun.State workflowState, Map map = [:] ->
        return new WorkflowRun([
                workflow        : workflow,
                state           : workflowState,
                project         : project,
                priority        : priority,
                displayName     : "run ${WorkflowRun.count() + 1}: ${workflowState}\n${displayText}",
                shortDisplayName: "run ${WorkflowRun.count() + 1}: ${workflowState}",
                combinedConfig  : '{}',
                workflowSteps   : [],
        ] + map).save(flush: true)
    }

    Closure createWorkflowStep = { WorkflowRun workflowRun, WorkflowStep.State state, Map map = [:] ->
        WorkflowStep workflowStep = new WorkflowStep([
                workflowRun: workflowRun,
                beanName   : DataInstallationConditionalFailJob.simpleName.uncapitalize(),
                state      : state,
                clusterJobs: [] as Set,
        ] + map).save(flush: true)

        (1..5).each {
            new WorkflowMessageLog([
                    workflowStep: workflowStep,
                    message     : "Log ${it} for ${workflowStep}",
                    createdBy   : "SYSTEM",
            ]).save(flush: true)
        }
        return workflowStep
    }

    Closure createClusterJob = { WorkflowStep workflowStep, int i, Map map = [:] ->
        ClusterJob clusterJob = new ClusterJob([
                oldSystem     : false,
                workflowStep  : workflowStep,
                jobLog        : "/tmp/log${i}.out",
                jobClass      : "JobClass",
                clusterJobName: "clusterJobName_${i}_JobClass",
                clusterJobId  : 1000000 + i,
                userName      : "otp",
                queued        : ZonedDateTime.now(),
                checkStatus   : ClusterJob.CheckStatus.CREATED,
        ] + map).save(flush: true)
        workflowStep.clusterJobs.add(clusterJob)
        return clusterJob
    }
    Closure createEndedClusterJob = { WorkflowStep workflowStep, int i, Map map = [:] ->
        return createClusterJob(workflowStep, i, [
                checkStatus      : ClusterJob.CheckStatus.FINISHED,
                queued           : ZonedDateTime.now().minusDays(4),
                eligible         : ZonedDateTime.now().minusDays(3),
                started          : ZonedDateTime.now().minusDays(2),
                ended            : ZonedDateTime.now().minusDays(1),
                requestedWalltime: Duration.ofHours(48),
                requestedCores   : 3,
                usedCores        : 2,
                cpuTime          : Duration.ofHours(20),
                requestedMemory  : 3000000,
                usedMemory       : 2000000,
                usedSwap         : 1000,
                startCount       : 1,
                node             : "Node ${i}",
        ] + map)
    }
    Closure createSuccessClusterJob = { WorkflowStep workflowStep, int i ->
        return createEndedClusterJob(workflowStep, i, [
                exitStatus: ClusterJob.Status.COMPLETED,
                exitCode  : 0,
        ])
    }
    Closure createFailedClusterJob = { WorkflowStep workflowStep, int i ->
        return createEndedClusterJob(workflowStep, i, [
                exitStatus: ClusterJob.Status.FAILED,
                exitCode  : 1 + i,
        ])
    }

    Closure createWesLog = { String finished ->
        return new WesLog(
                name: "wes log name",
                cmd: "COMMAND CMD",
                startTime: LocalDateTime.now().minusHours(6),
                endTime: finished ? LocalDateTime.now() : null,
                stdout: (("OUTOUT " * 20) + "\n") * 30,
                stderr: finished == "failed" ? (("ERROR " * 20) + "\n") * 30 : "",
                exitCode: finished == "" ? null : (finished == "success" ? 0 : 100),
        ).save(flush: true)
    }

    Closure createWesRunLog = { String finished ->
        return new WesRunLog(
                state: finished == "" ? State.RUNNING : (finished == "success" ? State.COMPLETE : State.RUNNING),
                runLog: createWesLog(finished),
                taskLogs: [createWesLog(finished), createWesLog(finished), createWesLog(finished)],
                runRequest: "REQUEST 12345 asdgf",
        ).save(flush: true)
    }

    Closure createWesRun = { WorkflowStep workflowStep, int i, Map map = [:], String finished = "" ->
        WesRun wesRun = new WesRun(
                workflowStep: workflowStep,
                wesIdentifier: "wesRunName_${i}_JobClass",
                subPath: "sub/path",
                state: WesRun.MonitorState.CHECKING,
                wesRunLog: createWesRunLog(finished),
        ).save(flush: true)
        workflowStep.wesRuns.add(wesRun)
        return wesRun
    }

    Closure createSuccessWesRun = { WorkflowStep workflowStep, int i ->
        return createWesRun(workflowStep, i, [
                state: WesRun.MonitorState.FINISHED,
        ], "success")
    }
    Closure createFailedWesRun = { WorkflowStep workflowStep, int i ->
        return createWesRun(workflowStep, i, [
                state: WesRun.MonitorState.FINISHED,
        ], "failed")
    }

    Closure createWorkflowError = {
        return new WorkflowError([
                message   : "some error message",
                stacktrace: StackTraceUtils.getStackTrace(new OtpRuntimeException("Some Error")),
        ])
    }

    for (int i in 1..numberOfDemoSets) {
        WorkflowRun.State.values().each { WorkflowRun.State workflowState ->
            println "create worklow with state: ${workflowState}"
            WorkflowRun workflowRun = createWorkflowRun(workflowState)
            createArtefact(workflowRun, "output artefact")
            createInputArtefact(workflowRun, "input artefact")

            switch (workflowState) {
                case WorkflowRun.State.OMITTED_MISSING_PRECONDITION:
                    createWorkflowStep(workflowRun, WorkflowStep.State.OMITTED)
                    workflowRun.omittedMessage = new OmittedMessage(
                            category: OmittedMessage.Category.PREREQUISITE_WORKFLOW_RUN_NOT_SUCCESSFUL,
                            message: "omitted info",
                    )
                    break
                case WorkflowRun.State.RUNNING_OTP:
                    WorkflowStep step1 = createWorkflowStep(workflowRun, WorkflowStep.State.SUCCESS)
                    createWorkflowStep(workflowRun, WorkflowStep.State.RUNNING, [previous: step1])
                    break
                case WorkflowRun.State.SUCCESS:
                    WorkflowStep lastStep = null
                    (1..10).each {
                        lastStep = createWorkflowStep(workflowRun, WorkflowStep.State.SUCCESS, [previous: lastStep])
                    }
                    (0..3).each {
                        createSuccessWesRun(lastStep, it)
                    }
                    break
                case WorkflowRun.State.FAILED:
                    WorkflowStep step1 = createWorkflowStep(workflowRun, WorkflowStep.State.SUCCESS, [beanName: "conditionalFailJob"])
                    WorkflowStep step2 = createWorkflowStep(workflowRun, WorkflowStep.State.SUCCESS, [beanName: "copyJob", obsolete: true, previous: step1])
                    WorkflowStep step3 = createWorkflowStep(workflowRun, WorkflowStep.State.FAILED, [beanName: "validateJob", workflowError: createWorkflowError(), obsolete: true, previous: step2])
                    WorkflowStep step4 = createWorkflowStep(workflowRun, WorkflowStep.State.SUCCESS, [restartedFrom: step2, beanName: step2.beanName, previous: step3])
                    createSuccessClusterJob(step4, 1)
                    createFailedClusterJob(step4, 2)
                    WorkflowStep step5 = createWorkflowStep(workflowRun, WorkflowStep.State.FAILED, [beanName: "linkJob", workflowError: createWorkflowError(), previous: step4])
                    (0..3).each {
                        createFailedWesRun(step5, it)
                    }
                    break
                case WorkflowRun.State.FAILED_FINAL:
                case WorkflowRun.State.RESTARTED:
                    WorkflowStep step1 = createWorkflowStep(workflowRun, WorkflowStep.State.SUCCESS)
                    createWorkflowStep(workflowRun, WorkflowStep.State.FAILED, [workflowError: createWorkflowError(), previous: step1])
                    break
                case WorkflowRun.State.RUNNING_WES:
                    WorkflowStep workflowStep = createWorkflowStep(workflowRun, WorkflowStep.State.SUCCESS)
                    (0..3).each {
                        createClusterJob(workflowStep, it * 3 + 1)
                        createSuccessClusterJob(workflowStep, it * 3 + 2)
                        createFailedClusterJob(workflowStep, it * 3 + 3)
                        createWesRun(workflowStep, it)
                    }
                    break
                case WorkflowRun.State.WAITING_FOR_USER:
                    createWorkflowStep(workflowRun, WorkflowStep.State.SUCCESS)
                    break
            }
            workflowRun.save(flush: true)
        }
    }

    println "create workflow with restart"
    WorkflowRun workflowRun1 = createWorkflowRun(WorkflowRun.State.RESTARTED, [
            displayName     : "Workflow with restarts: First run\n${displayText}",
            shortDisplayName: 'Workflow with restarts: First run',
    ])
    WorkflowStep step1 = createWorkflowStep(workflowRun1, WorkflowStep.State.SUCCESS)
    WorkflowStep step2 = createWorkflowStep(workflowRun1, WorkflowStep.State.FAILED, [workflowError: createWorkflowError(), previous: step1])
    WorkflowStep step3 = createWorkflowStep(workflowRun1, WorkflowStep.State.FAILED, [workflowError: createWorkflowError(), restartedFrom: step2, beanName: step2.beanName, previous: step2])
    WorkflowStep step4 = createWorkflowStep(workflowRun1, WorkflowStep.State.FAILED, [workflowError: createWorkflowError(), restartedFrom: step3, beanName: step3.beanName, previous: step3])
    WorkflowStep step5 = createWorkflowStep(workflowRun1, WorkflowStep.State.SUCCESS, [previous: step4])
    createWorkflowStep(workflowRun1, WorkflowStep.State.FAILED, [workflowError: createWorkflowError(), previous: step5])

    WorkflowRun workflowRun2 = createWorkflowRun(WorkflowRun.State.RESTARTED, [
            displayName     : "Workflow with restarts: Second run\n${displayText}",
            shortDisplayName: 'Workflow with restarts: Second run',
            restartedFrom   : workflowRun1,
    ])
    WorkflowStep step2_1 = createWorkflowStep(workflowRun2, WorkflowStep.State.SUCCESS)
    WorkflowStep step2_2 = createWorkflowStep(workflowRun2, WorkflowStep.State.SUCCESS, [previous: step2_1])
    createWorkflowStep(workflowRun2, WorkflowStep.State.FAILED, [workflowError: createWorkflowError(), previous: step2_2])

    WorkflowRun workflowRun3 = createWorkflowRun(WorkflowRun.State.SUCCESS, [
            displayName     : "Workflow with restarts: Final run\n${displayText}",
            shortDisplayName: 'Workflow with restarts: Final run',
            restartedFrom   : workflowRun2,
    ])
    WorkflowStep step3_1 = createWorkflowStep(workflowRun3, WorkflowStep.State.SUCCESS)
    WorkflowStep step3_2 = createWorkflowStep(workflowRun3, WorkflowStep.State.SUCCESS, [previous: step3_1])
    createWorkflowStep(workflowRun3, WorkflowStep.State.SUCCESS, [previous: step3_2])

    [
            workflowRun1,
            workflowRun2,
            workflowRun3,
    ].each { WorkflowRun workflowRun ->
        (1..4).each {
            createInputArtefact(workflowRun, "FASTQ input artefact ${it}")
        }
        (1..4).each {
            createInputArtefact(workflowRun, "FASTQC input artefact ${it}")
        }
        (1..4).each {
            createArtefact(workflowRun, "Bam output artefact ${it}")
        }
    }
    [
            step2,
            step3,
            step4,
            step5,
            step2_2,
            step3_2
    ].each { WorkflowStep step ->
        (1..2).each {
            createSuccessClusterJob(step, it)
            createSuccessWesRun(step, it)
        }
    }
    [
            step2,
            step3,
            step4,
    ].each { WorkflowStep step ->
        (3..4).each {
            createFailedClusterJob(step, it)
            createFailedWesRun(step, it)
        }
    }
}

FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService

FileSystem fileSystem = fileSystemService.remoteFileSystem
(1..3).each {
    Path path = fileSystem.getPath("/tmp/log${it}.out")
    if (!Files.exists(path)) {
        println "create file: ${path}"
        fileService.createFileWithContent(path, "Example log ${it}\n\nSome content\nMorecontent")
    }
}

''
