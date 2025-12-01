/*
 * Copyright 2011-2025 The OTP authors
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
package initializations.demoImports

import groovy.transform.Field
import io.swagger.client.wes.model.State

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.filestore.*
import de.dkfz.tbi.otp.infrastructure.CreateLinkOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.StackTraceUtils
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowMessageLog
import de.dkfz.tbi.otp.workflowExecution.wes.*

import java.nio.file.*
import java.time.*

/**
 * Script to create a complete WesFastQcWorkflow (nf-seq-qc) with all jobs/workflow steps
 * for testing the WesRunController UI based on the predefined scenarios.
 */

// ====== INPUT PARAMETERS ======
/**
 * Name of the project to create or use (e.g. "ExampleProject")
 */
String projectName = ""

/**
 * Base directory where the OTP file structure will be created (e.g. "/data/otp")
 * Make sure this path exists and OTP has write permissions
 */
String baseDir = ""

/**
 * UUID root folder name within the base directory (e.g. "uuid-root")
 * This folder will be created only if work folder does not exist
 */
String uuidRoot = ""

@Field FilestoreService filestoreService = ctx.filestoreService
@Field FileService fileService = ctx.fileService
@Field FileSystemService fileSystemService = ctx.fileSystemService
@Field RawSequenceDataViewFileService rawSequenceDataViewFileService = ctx.rawSequenceDataViewFileService

println "Creating WesFastQcWorkflow test data for project: ${projectName}..."

/**
 * Included test scenarios
 */
enum ScenarioType {
    SUCCESS,
    RUNNING,
    FAILED_OTP,
    FAILED_WES
}

// ====== HELPER METHODS ======
/**
 * Find or create the project with the given name
 */
Project findOrCreateProject(String name) {
    Project project = Project.findByName(name)
    if (!project) {
        println "Creating new project: ${name}"
        ProcessingPriority priority = ProcessingPriority.first() ?: new ProcessingPriority(
            name: "NORMAL",
            priority: 5,
            errorMailPrefix: "[OTP-ERROR]"
        ).save(flush: true)

        project = new Project(
            name: name,
            projectType: Project.ProjectType.SEQUENCING,
            unixGroup: "WES_GROUP",
            individualPrefix: name.toLowerCase().replaceAll(/[^a-z0-9]/, ''),
            dirName: name.toLowerCase().replaceAll(/[^a-z0-9_-]/, '_'),
            description: "Test project for WES FastQC workflow testing",
            processingPriority: priority,
            state: Project.State.OPEN
        ).save(flush: true)
    }
    return project
}

/**
 * Find or create an individual for the project
 */
Individual findOrCreateIndividual(Project project, String pid) {
    Individual individual = Individual.findByPidAndProject(pid, project)
    if (!individual) {
        println "Creating individual: ${pid}"
        individual = new Individual(
            pid: pid,
            mockPid: pid,
            mockFullName: "Test Individual ${pid}",
            project: project,
            type: Individual.Type.REAL
        ).save(flush: true)
    } else {
        println "Found existing individual: ${pid}"
    }
    return individual
}

/**
 * Find or create a SeqTrack for the individual with a sequence file
 */
SeqTrack findOrCreateSeqTrack(Individual individual, SeqType seqType, String identifier) {
    // Find or create Run first
    Run run = findOrCreateRun(identifier)

    // Find or create SampleType
    SampleType sampleType = findOrCreateSampleType("blood")

    // Find or create Sample
    Sample sample = findOrCreateSample(individual, sampleType)

    // Check if SeqTrack already exists
    SeqTrack seqTrack = SeqTrack.findBySampleAndSeqTypeAndRunAndLaneId(
            sample, seqType, run, "L00${identifier[-1]}"
    )

    if (!seqTrack) {
        println "Creating SeqTrack for individual: ${individual.pid}"
        seqTrack = new SeqTrack(
            sample: sample,
            seqType: seqType,
            run: run,
            laneId: "L00${identifier[-1]}",
            sampleIdentifier: "SAMPLE_${identifier}",
            pipelineVersion: findOrCreateSoftwareTool(),
            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
            fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
            libraryPreparationKit: LibraryPreparationKit.first(),
            kitInfoReliability: InformationReliability.KNOWN
        ).save(flush: true)

        FastqImportInstance fastqImportInstance = new FastqImportInstance([
                importMode: FastqImportInstance.ImportMode.MANUAL,
        ]).save(flush: true)

        new FastqFile(
                seqTrack: seqTrack,
                mateNumber: 1,
                vbpFileName: "sample_${identifier}_R1.fastq.gz",
                indexFile: false,
                sequenceLength: 150,
                nReads: 1000000,
                fastqImportInstance: fastqImportInstance,
                fileType: FileType.findAllByType(FileType.Type.SEQUENCE).first(),
                fileName: "sample_${identifier}_R1.fastq.gz",
                pathName: '',
                initialDirectory: '/tmp',
                fastqMd5sum: "0" * 32,
                run: seqTrack.run,
                project: seqTrack.project,
                fileExists: true,
                fileLinked: true,
                fileSize: 1000000000,
                dateLastChecked: new Date()
        ).save(flush: true)
    } else {
        println "Found existing SeqTrack for individual: ${individual.pid}"
    }

    return seqTrack
}

/**
 * Find or create a Run
 */
Run findOrCreateRun(String identifier) {
    String runName = "TEST_RUN_${identifier}"
    Run run = Run.findByName(runName)
    if (!run) {
        println "Creating run: ${runName}"
        run = new Run(
            name: runName,
            dateExecuted: new Date(),
            seqCenter: findOrCreateSeqCenter(),
            seqPlatform: findOrCreateSeqPlatform()
        ).save(flush: true)
    } else {
        println "Found existing run: ${runName}"
    }
    return run
}

/**
 * Find or create a SeqCenter
 */
SeqCenter findOrCreateSeqCenter() {
    SeqCenter seqCenter = SeqCenter.findByName("TEST_CENTER")
    if (!seqCenter) {
        seqCenter = new SeqCenter(
            name: "TEST_CENTER",
            dirName: "test_center"
        ).save(flush: true)
    }
    return seqCenter
}

/**
 * Find or create a SeqPlatform
 */
SeqPlatform findOrCreateSeqPlatform() {
    SeqPlatform seqPlatform = SeqPlatform.findByName("HISEQ_2500")
    if (!seqPlatform) {
        seqPlatform = new SeqPlatform(
            name: "HISEQ_2500",
            seqPlatformGroups: [] as Set
        ).save(flush: true)
    }
    return seqPlatform
}

/**
 * Find or create a SampleType
 */
SampleType findOrCreateSampleType(String name) {
    SampleType sampleType = SampleType.findByName(name)
    if (!sampleType) {
        sampleType = new SampleType(
            name: name,
        ).save(flush: true)
    }
    return sampleType
}

/**
 * Find or create a Sample
 */
Sample findOrCreateSample(Individual individual, SampleType sampleType) {
    Sample sample = Sample.findByIndividualAndSampleType(individual, sampleType)
    if (!sample) {
        sample = new Sample(
            individual: individual,
            sampleType: sampleType
        ).save(flush: true)
    }
    return sample
}

/**
 * Find or create workflow artefacts
 */
WorkflowArtefact findOrCreateArtefact(WorkflowRun producedBy, String outputRole, SeqTrack seqTrack) {
    // Check if artefact already exists for this SeqTrack
    if (seqTrack.workflowArtefact) {
        println "Found existing workflow artefact for SeqTrack: ${seqTrack.id}"
        return seqTrack.workflowArtefact
    }

    String displayText = [
        project: seqTrack.individual.project.name,
        individual: seqTrack.individual.pid,
        seqType: seqTrack.seqType.displayNameWithLibraryLayout,
        sampleType: seqTrack.sampleType,
        run: seqTrack.run.name,
        laneId: seqTrack.laneId,
    ].collect { "${it.key}: ${it.value}" }.join('\n')

    WorkflowArtefact workflowArtefact = new WorkflowArtefact(
        state: WorkflowArtefact.State.SUCCESS,
        producedBy: producedBy,
        outputRole: outputRole,
        artefactType: ArtefactType.FASTQ,
        displayName: "FastQC artefact\n${displayText}",
    ).save(flush: true)

    seqTrack.workflowArtefact = workflowArtefact
    seqTrack.save(flush: true)
    return workflowArtefact
}

/**
 * Find or create input artefact for workflow run
 */
WorkflowRunInputArtefact findOrCreateInputArtefact(WorkflowRun workflowRun, String inputRole, SeqTrack seqTrack) {
    WorkflowRunInputArtefact inputArtefact = WorkflowRunInputArtefact.findByWorkflowRunAndRole(workflowRun, inputRole)
    if (!inputArtefact) {
        inputArtefact = new WorkflowRunInputArtefact(
            workflowRun: workflowRun,
            role: inputRole,
            workflowArtefact: findOrCreateArtefact(null, null, seqTrack)
        ).save(flush: true)
    }
    return inputArtefact
}

SoftwareTool findOrCreateSoftwareTool() {
    String name = "ExampleSoftwareTool"
    return CollectionUtils.atMostOneElement(SoftwareTool.findAllByProgramName(name)) ?: new SoftwareTool([
            programName   : "ExampleSoftwareTool",
            programVersion: "1.2.3",
            type          : SoftwareTool.Type.BASECALLING,
    ]).save(flush: true)
}

/**
 * Find or create a complete workflow run with specified state and characteristics
 */
WorkflowRun findOrCreateWorkflowRun(Workflow workflow, Project project, SeqTrack seqTrack,
                              WorkflowRun.State workflowState, String displaySuffix,
                              BaseFolder baseFolder, ScenarioType scenarioType) {

    String displayText = [
        project: project.name,
        individual: seqTrack.individual.pid,
        seqType: seqTrack.seqType.displayNameWithLibraryLayout,
        sampleType: seqTrack.sampleType,
        run: seqTrack.run.name,
        laneId: seqTrack.laneId,
    ].collect { "${it.key}: ${it.value}" }.join('\n')

    String shortDisplayName = "WesFastQcWorkflow ${displaySuffix}"

    // Check if workflow run already exists for this combination
    WorkflowRun workflowRun = WorkflowRun.findByWorkflowAndProjectAndShortDisplayName(workflow, project, shortDisplayName)

    if (!workflowRun) {
        println "Creating new workflow run: ${shortDisplayName} for ${seqTrack.individual.pid}"

        WorkFolder workFolder = filestoreService.createWorkFolder(baseFolder)

        workflowRun = new WorkflowRun(
            workflow: workflow,
            state: workflowState,
            project: project,
            priority: project.processingPriority,
            displayName: "WesFastQcWorkflow ${displaySuffix}\n${displayText}",
            shortDisplayName: shortDisplayName,
            combinedConfig: '{}',
            workflowSteps: [],
            workFolder: workFolder,
        ).save(flush: true)

        // Create input and output artefacts
        findOrCreateArtefact(workflowRun, WesFastQcWorkflow.OUTPUT_FASTQC, seqTrack)
        findOrCreateInputArtefact(workflowRun, WesFastQcWorkflow.INPUT_FASTQ, seqTrack)

        // Create workflow steps based on scenario
        createWorkflowSteps(workflowRun, scenarioType)

        workflowRun.save(flush: true)
    } else {
        println "Found existing workflow run: ${shortDisplayName} for ${seqTrack.individual.pid} (ID: ${workflowRun.id})"

        // Update state if it's different (useful for re-running script with different states)
        if (workflowRun.state != workflowState) {
            println "Updating workflow run state from ${workflowRun.state} to ${workflowState}"
            workflowRun.state = workflowState

            // Also update the processing step state for data consistency
            if (workflowRun.workflowSteps) {
                SeqTrack seqTrackUpdate = SeqTrack.findByWorkflowArtefact(
                    WorkflowRunInputArtefact.findByWorkflowRun(workflowRun)?.workflowArtefact
                )
                if (seqTrackUpdate) {
                    seqTrackUpdate.fastqcState = workflowState == WorkflowRun.State.SUCCESS ?
                        SeqTrack.DataProcessingState.FINISHED :
                        SeqTrack.DataProcessingState.NOT_STARTED
                    seqTrackUpdate.save(flush: true)
                }
            }

            workflowRun.save(flush: true)
        }
    }

    return workflowRun
}

/**
 * Find or create workflow steps based on scenario type - only create if no steps exist
 */
void findOrCreateWorkflowSteps(WorkflowRun workflowRun, String scenarioType) {
    // Check if workflow steps already exist
    if (workflowRun.workflowSteps && !workflowRun.workflowSteps.isEmpty()) {
        println "Workflow steps already exist for workflow run ${workflowRun.id}, skipping creation"
        return
    }

    createWorkflowSteps(workflowRun, scenarioType)
}

/**
 * Create workflow steps based on scenario type
 */
void createWorkflowSteps(WorkflowRun workflowRun, ScenarioType scenarioType) {
    WorkflowStepsCreator creator = WorkflowStepsCreatorFactory.getInstance(scenarioType, workflowRun)
    creator.createWorkflowSteps()
}

class WorkflowStepsCreatorFactory {
    static Map<ScenarioType, Class> scenarioCreators = [
            (ScenarioType.SUCCESS)    : SuccessfulWorkflowRunCreator,
            (ScenarioType.RUNNING)    : RunningWorkflowRunCreator,
            (ScenarioType.FAILED_OTP) : OtpFailedWorkflowRunCreator,
            (ScenarioType.FAILED_WES) : WesFailedWorkflowRunCreator,
    ]
    static WorkflowStepsCreator getInstance(ScenarioType scenarioType, WorkflowRun run) {
        return scenarioCreators[scenarioType].newInstance(scenarioType, run)
    }
}

trait WorkflowStepsCreator {
    ScenarioType scenarioType
    WorkflowRun workflowRun

    static List<String> jobNames = new WesFastQcWorkflow().jobList*.simpleName.collect {
        it.uncapitalize()
    }

    abstract int getLastJob()

    abstract WorkflowStep.State getStepState(int idx)

    abstract void handleExecutionStep(WorkflowStep step, int idx)

    void createWorkflowSteps() {
        int idx = 0
        WorkflowStep previousStep = null
        while(idx < lastJob) {
            WorkflowStep step = createWorkflowStep(jobNames[idx], previousStep)
            if (idx == 5) {
                step.state = getStepState(idx)
                handleExecutionStep(step, idx)
            }
            // handle the validation job
            if (idx == 6) {
                if (previousStep.wesRuns.first().wesRunLog.state == State.EXECUTOR_ERROR) {
                    step.state = WorkflowStep.State.FAILED
                    step.workflowError = createWorkflowError("Validation step failed.")
                    step.save(flush: true)
                }
            }

            // Add log messages
            (1..3).each { i ->
                String jobName = jobNames[idx]
                String logMessage = step.state == WorkflowStep.State.FAILED ?
                        "ERROR: Step ${i} failed for ${jobName}" :
                        "Log message ${i} for ${jobName}"
                new WorkflowMessageLog(
                        workflowStep: step,
                        message: logMessage,
                        createdBy: "SYSTEM",
                ).save(flush: true)
            }

            previousStep = step
            step.save(flush: true)

            idx++
        }
    }

    // Helper functions
    WorkflowStep createWorkflowStep(String jobName, WorkflowStep previousStep) {
        return new WorkflowStep([
                workflowRun: workflowRun,
                beanName: jobName,
                state: WorkflowStep.State.SUCCESS,
                previous: previousStep,
                clusterJobs: [] as Set,
                wesRuns: [] as Set,
        ]).save(flush: false)
    }

    /**
     * Create WES run for workflow step
     */
    WesRun createWesRun(WorkflowStep workflowStep, String jobName, WesRun.MonitorState monitorState, State jobState, String subPath) {
        WesRun wesRun = new WesRun(
                workflowStep: workflowStep,
                wesIdentifier: "${jobName}_${System.currentTimeMillis()}",
                subPath: subPath,
                state: monitorState,
                wesRunLog: createWesRunLog(jobState, subPath),
        ).save(flush: true)
        workflowStep.wesRuns.add(wesRun)
        return wesRun
    }


    /**
     * Create WES run log based on state
     */
    WesRunLog createWesRunLog(State state, String subPath) {
        return new WesRunLog(
                state: state,
                runLog: createWesLog("main", subPath, state),
                taskLogs: [
                        createWesLog("fastqc_task_1", subPath, state),
                        createWesLog("fastqc_task_2", subPath, state),
                        createWesLog("report_generation", subPath, state)
                ],
                runRequest: "{\n  \"workflow_url\": \"nf-core/fastqc\",\n  \"workflow_params\": {\n    \"input\": \"sample.fastq.gz\"\n  }\n}",
        ).save(flush: true)
    }

    /**
     * Create WES logs based on state
     */
    WesLog createWesLog(String name, String subPath, State state) {
        return new WesLog(
                name: name,
                cmd: "nextflow run nf-core/fastqc --input sample.fastq.gz --outdir results",
                startTime: ZonedDateTime.of(LocalDateTime.now().minusHours(2), ZoneId.systemDefault()),
                endTime: state == State.RUNNING ? null : ZonedDateTime.of(LocalDateTime.now().minusMinutes(30), ZoneId.systemDefault()),
                stdout: subPath + "/stdout.log",
                stderr: subPath + "/stderr.log",
                exitCode: state == State.RUNNING ? null : (state == State.COMPLETE ? 0 : 1),
        ).save(flush: true)
    }

    /**
     * Create workflow error
     */
    WorkflowError createWorkflowError(String errorMessage) {
        return new WorkflowError(
                message: errorMessage,
                stacktrace: StackTraceUtils.getStackTrace(new OtpRuntimeException(errorMessage)),
        ).save(flush: true)
    }
}

class RunningWorkflowRunCreator implements WorkflowStepsCreator {
    RunningWorkflowRunCreator(ScenarioType scenarioType, WorkflowRun workflowRun) {
        this.scenarioType = scenarioType
        this.workflowRun = workflowRun
    }

    @Override
    int getLastJob() {
        return 6
    }
    @Override
    WorkflowStep.State getStepState(int idx) {
        return WorkflowStep.State.SUCCESS
    }
    @Override
    void handleExecutionStep(WorkflowStep step, int idx) {
        createWesRun(step, jobNames[idx], WesRun.MonitorState.FINISHED, State.RUNNING, "")
    }
}

class SuccessfulWorkflowRunCreator implements WorkflowStepsCreator {
    SuccessfulWorkflowRunCreator(ScenarioType scenarioType, WorkflowRun workflowRun) {
        this.scenarioType = scenarioType
        this.workflowRun = workflowRun
    }

    @Override
    int getLastJob() {
        return jobNames.size()
    }

    @Override
    WorkflowStep.State getStepState(int idx) {
        return WorkflowStep.State.SUCCESS
    }

    @Override
    void handleExecutionStep(WorkflowStep step, int idx) {
        createWesRun(step, jobNames[idx], WesRun.MonitorState.FINISHED, State.COMPLETE, "")
    }
}

class OtpFailedWorkflowRunCreator implements WorkflowStepsCreator {
    OtpFailedWorkflowRunCreator(ScenarioType scenarioType, WorkflowRun workflowRun) {
        this.scenarioType = scenarioType
        this.workflowRun = workflowRun
    }

    @Override
    int getLastJob() {
        return 6
    }

    @Override
    WorkflowStep.State getStepState(int idx) {
        return idx == 5 ? WorkflowStep.State.FAILED : WorkflowStep.State.SUCCESS
    }

    @Override
    void handleExecutionStep(WorkflowStep step, int idx) {
        step.state = WorkflowStep.State.FAILED
        step.workflowError = createWorkflowError("OTP failed to trigger WES execution.")
        step.save(flush: true)
    }
}

class WesFailedWorkflowRunCreator implements WorkflowStepsCreator {
    WesFailedWorkflowRunCreator(ScenarioType scenarioType, WorkflowRun workflowRun) {
        this.scenarioType = scenarioType
        this.workflowRun = workflowRun
    }
    @Override
    int getLastJob() {
        return 7
    }

    @Override
    WorkflowStep.State getStepState(int idx) {
        return idx == 6 ? WorkflowStep.State.FAILED : WorkflowStep.State.SUCCESS
    }

    @Override
    void handleExecutionStep(WorkflowStep step, int idx) {
        createWesRun(step, jobNames[idx], WesRun.MonitorState.FINISHED, State.EXECUTOR_ERROR, "")
    }
}

// ====== MAIN EXECUTION ======

BaseFolder baseFolder = null
Path basePath = fileSystemService.remoteFileSystem.getPath(baseDir)

Map<ScenarioType, WorkflowRun> wesWorkflowRuns = [:]

WorkflowRun.withNewTransaction {
    // Find SeqType from database
    SeqType seqType = SeqType.findByName('WHOLE_GENOME') ?: SeqType.findByName('EXAMPLE')
    if (!seqType) {
        println "No suitable SeqType found. Please ensure test data exists."
        return
    }

    // Create or find project
    Project project = findOrCreateProject(projectName)

    // Create 3 individuals for the test scenarios
    Individual individual1 = findOrCreateIndividual(project, "${projectName}_WES_IND_001")
    Individual individual2 = findOrCreateIndividual(project, "${projectName}_WES_IND_002")
    Individual individual3 = findOrCreateIndividual(project, "${projectName}_WES_IND_003")
    Individual individual4 = findOrCreateIndividual(project, "${projectName}_WES_IND_004")

    // Create 3 SeqTracks
    SeqTrack seqTrack1 = findOrCreateSeqTrack(individual1, seqType, "${projectName}_001")
    SeqTrack seqTrack2 = findOrCreateSeqTrack(individual2, seqType, "${projectName}_002")
    SeqTrack seqTrack3 = findOrCreateSeqTrack(individual3, seqType, "${projectName}_003")
    SeqTrack seqTrack4 = findOrCreateSeqTrack(individual4, seqType, "${projectName}_004")

    String workflowName = WesFastQcWorkflow.WORKFLOW
    println "Creating workflow: ${workflowName}"

    // Find or create the WesFastQcWorkflow
    Workflow workflow = CollectionUtils.atMostOneElement(Workflow.findAllByName(workflowName)) ?: new Workflow(
        name: workflowName,
        beanName: WesFastQcWorkflow.simpleName.uncapitalize(),
        enabled: true
    ).save(flush: true)

    // Find or create BaseFolder
    try {
        baseFolder = filestoreService.findAnyWritableBaseFolder()
    } catch (AssertionError e) {
        baseFolder = new BaseFolder(
            path: basePath.resolve(uuidRoot).toString(),
            writable: true
        ).save(flush: true)
        // Create main directory structure if not done yet
        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(basePath)

        println "Created new BaseFolder: ${baseFolder.path}"
    }

    // Create workflow runs with different states & scenarios
    wesWorkflowRuns[ScenarioType.SUCCESS] = findOrCreateWorkflowRun(workflow, project, seqTrack1, WorkflowRun.State.SUCCESS, "Finished Successfully", baseFolder, ScenarioType.SUCCESS)
    wesWorkflowRuns[ScenarioType.RUNNING] = findOrCreateWorkflowRun(workflow, project, seqTrack2, WorkflowRun.State.RUNNING_WES, "Currently Running", baseFolder, ScenarioType.RUNNING)
    wesWorkflowRuns[ScenarioType.FAILED_OTP] = findOrCreateWorkflowRun(workflow, project, seqTrack3, WorkflowRun.State.FAILED, "Failed in OTP", baseFolder, ScenarioType.FAILED_OTP)
    wesWorkflowRuns[ScenarioType.FAILED_WES] = findOrCreateWorkflowRun(workflow, project, seqTrack4, WorkflowRun.State.FAILED, "Failed in WES", baseFolder, ScenarioType.FAILED_WES)

    println "Successfully created WesFastQcWorkflow test data:"
    wesWorkflowRuns.each { ScenarioType st, WorkflowRun run ->
        println "- Workflow with ID: ${run.id} and state: ${run.state}, for the scenario: ${st})"
    }
}

// ====== FILE STRUCTURE CREATION ======

/**
 * Create file structure for a specific workflow run
 */
void createFileStructureForWorkflowRun(WorkflowRun workflowRun, String baseDir, String subPath) {
    // Get the correct SeqTrack for this workflow run through input artefacts
    WorkflowRunInputArtefact inputArtefact = WorkflowRunInputArtefact.findByWorkflowRun(workflowRun)
    if (!inputArtefact) {
        println "No input artefact found for workflow run ${workflowRun.id}, skipping file structure creation"
        return
    }

    SeqTrack seqTrack = SeqTrack.findByWorkflowArtefact(inputArtefact.workflowArtefact)
    if (!seqTrack) {
        println "No SeqTrack found for workflow run ${workflowRun.id}, skipping file structure creation"
        return
    }

    Individual individual = seqTrack.individual

    println "Creating OTP file structure for workflow ${workflowRun.id} (${individual.pid}) in: ${baseDir}"

    // Use remote filesystem consistently for all path operations
    Path workFolderPath = filestoreService.getWorkFolderPath(workflowRun)

    // Create the actual directory on filesystem
    if (Files.exists(workFolderPath)) {
        fileService.deleteDirectoryRecursively(workFolderPath)
    }
    fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(workFolderPath)
    println "Created WorkFolder with UUID: ${workflowRun.workFolder.uuid}"
    println "WorkFolder path: ${workFolderPath}"

    // Create proper OTP view-by-pid structure using OTP file services
    Path runPath = rawSequenceDataViewFileService.getFilePath(seqTrack.sequenceFiles.first()).parent

    fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(runPath)

    // Create subfolders within the UUID directory using remote filesystem
    Path inputPath = workFolderPath.resolve("input")
    Path fastqcResultsPath = workFolderPath.resolve(subPath.isEmpty() ? "" : subPath)
    Path tracePath = workFolderPath.resolve("trace")
    Path logsPath = workFolderPath.resolve("logs")

    fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(inputPath)
    fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(fastqcResultsPath)
    fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(tracePath)
    fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(logsPath)

    // Create realistic FastQ files in input subfolder
    String samplePrefix = "sample_${individual.pid.toLowerCase()}"
    String fastqContent1 = """@SEQ_ID_1_${individual.pid}
GATTTGGGGTTCAAAGCAGTATCGATCAAATAGTAAATCCATTTGTTCAACTCACAGTTT
+
!''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65
@SEQ_ID_2_${individual.pid}
TTTCAGTTTTCAGATCCCAATCGGTCATTCTTTGTAACAGTCTTCCATTAACCAAAACCT
+
@@@FFFFFHHHHHJJJJIJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJ
"""

    String fastqContent2 = """@SEQ_ID_1_${individual.pid}
GATCAAATAGTAAATCCATTTGTTCAACTCACAGTTTGATTTGGGGTTCAAAGCAGTATC
+
!''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65
@SEQ_ID_2_${individual.pid}
CAGATCCCAATCGGTCATTCTTTGTAACAGTCTTCCATTAACCAAAACCTTTTCAGTTTT
+
@@@FFFFFHHHHHJJJJIJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJ
"""

    // Create FastQ files in input subfolder
    Path fastq1 = inputPath.resolve("${samplePrefix}_R1.fastq.gz")
    Path fastq2 = inputPath.resolve("${samplePrefix}_R2.fastq.gz")

    fileService.createFileWithContent(fastq1, fastqContent1)
    fileService.createFileWithContent(fastq2, fastqContent2)

    // Create FastQC result files based on workflow state
    boolean shouldCreateResults = workflowRun.state in [WorkflowRun.State.SUCCESS, WorkflowRun.State.FAILED]

    if (shouldCreateResults) {
        Path reportHtml = fastqcResultsPath.resolve("report_fastqc.html")
        Path fastqcZip1 = fastqcResultsPath.resolve("${samplePrefix}_R1_fastqc.zip")
        Path fastqcZip2 = fastqcResultsPath.resolve("${samplePrefix}_R2_fastqc.zip")

        String fastqcReport = """<!DOCTYPE html>
<html>
<head>
    <title>FastQC Report - ${individual.pid}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .summary { background-color: #f0f0f0; padding: 10px; border-radius: 5px; }
        .module { margin: 20px 0; }
        .pass { color: green; font-weight: bold; }
        .fail { color: red; font-weight: bold; }
        .warn { color: orange; font-weight: bold; }
    </style>
</head>
<body>
    <h1>WESkit Report - ${individual.pid}</h1>
    <div class="summary">
        <h2>Basic Statistics</h2>
        <table>
            <tr><td>Filename</td><td>${samplePrefix}_R1.fastq.gz</td></tr>
            <tr><td>File type</td><td>Conventional base calls</td></tr>
            <tr><td>Encoding</td><td>Sanger / Illumina 1.9</td></tr>
            <tr><td>Total Sequences</td><td>10000000</td></tr>
            <tr><td>Sequences flagged as poor quality</td><td>0</td></tr>
            <tr><td>Sequence length</td><td>151</td></tr>
            <tr><td>%GC</td><td>42</td></tr>
        </table>
    </div>
    
    <div class="module">
        <h3><span class="pass">✓</span> Per base sequence quality</h3>
        <p>The quality of all bases remains high throughout the read.</p>
    </div>
    
    <div class="module">
        <h3><span class="${workflowRun.state == WorkflowRun.State.FAILED ? 'fail' : 'pass'}">
        ${workflowRun.state == WorkflowRun.State.FAILED ? '✗' : '✓'}</span> Per sequence quality scores</h3>
        <p>${workflowRun.state == WorkflowRun.State.FAILED ? 'Quality analysis failed during processing.' : 'The per sequence quality scores are all above 30.'}</p>
    </div>
</body>
</html>"""

        String zipContent = "PK\u0003\u0004\u0014\u0000\u0000\u0000\u0008\u0000" // Fake ZIP header

        fileService.createFileWithContent(reportHtml, fastqcReport)
        fileService.createFileWithContent(fastqcZip1, zipContent + "FastQC ZIP content for ${samplePrefix}_R1")
        fileService.createFileWithContent(fastqcZip2, zipContent + "FastQC ZIP content for ${samplePrefix}_R2")
    }

    // Create Nextflow trace file
    Path traceFile = tracePath.resolve("trace_${workflowRun.id}_${System.currentTimeMillis()}.txt")
    String traceContent = """executor > local (12)
[7f/d48c0e] process > NFCORE_FASTQC:FASTQC:FASTQC (${samplePrefix}_R1) [${workflowRun.state == WorkflowRun.State.SUCCESS ? '100%' : (workflowRun.state == WorkflowRun.State.FAILED ? '50%' : '75%')}] 1 of 1 ${workflowRun.state == WorkflowRun.State.SUCCESS ? '✓' : (workflowRun.state == WorkflowRun.State.FAILED ? '✗' : '⧖')}
[9a/e5f213] process > NFCORE_FASTQC:FASTQC:FASTQC (${samplePrefix}_R2) [${workflowRun.state == WorkflowRun.State.SUCCESS ? '100%' : (workflowRun.state == WorkflowRun.State.FAILED ? '50%' : '75%')}] 1 of 1 ${workflowRun.state == WorkflowRun.State.SUCCESS ? '✓' : (workflowRun.state == WorkflowRun.State.FAILED ? '✗' : '⧖')}

${workflowRun.state == WorkflowRun.State.SUCCESS ? 'Completed' : (workflowRun.state == WorkflowRun.State.FAILED ? 'Failed' : 'Running')} at: ${new Date()}
Duration    : 2m 45s
CPU hours   : 0.1
${workflowRun.state == WorkflowRun.State.SUCCESS ? 'Succeeded' : (workflowRun.state == WorkflowRun.State.FAILED ? 'Failed' : 'Running')}   : ${workflowRun.state == WorkflowRun.State.SUCCESS ? '12' : (workflowRun.state == WorkflowRun.State.FAILED ? '6' : '8')}"""

    fileService.createFileWithContent(traceFile, traceContent)

    // Create cluster job log files
    (1..5).each { i ->
        Path logFile = logsPath.resolve("fastqc_log${i}_${individual.pid.toLowerCase()}.out")
        String logStatus = workflowRun.state == WorkflowRun.State.FAILED && i > 3 ? "[FAILED]" : "[DONE]"
        fileService.createFileWithContent(logFile, """FastQC Log ${i} for ${individual.pid}

Starting FastQC analysis...
Input file: ${samplePrefix}_${i}.fastq.gz
Output directory: ${fastqcResultsPath}

Analysis Progress:
- Reading input file... ${logStatus}
- Basic statistics... ${logStatus}
- Per base sequence quality... ${logStatus}
${workflowRun.state == WorkflowRun.State.FAILED && i > 3 ? '- ERROR: Processing failed at validation step' : '- Analysis completed successfully'}

${workflowRun.state == WorkflowRun.State.FAILED && i > 3 ? 'FastQC analysis failed.' : 'FastQC analysis completed successfully.'}
Report generated: ${samplePrefix}_${i}_fastqc.html
Result files written to: ${fastqcResultsPath}
""")
    }

    // Create view-by-pid links
    Path viewFastqcResultsPath = runPath
    Path viewLogsPath = runPath.resolve("logs")
    fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(viewFastqcResultsPath)
    fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(viewLogsPath)

    try {
        // Link input FastQ files
        fileService.createLink(runPath.resolve("${samplePrefix}_R1.fastq.gz"), fastq1, CreateLinkOption.DELETE_EXISTING_FILE)
        fileService.createLink(runPath.resolve("${samplePrefix}_R2.fastq.gz"), fastq2, CreateLinkOption.DELETE_EXISTING_FILE)

        if (shouldCreateResults) {
            // Link FastQC result files
            fileService.createLink(viewFastqcResultsPath.resolve("report_fastqc.html"),
                fastqcResultsPath.resolve("report_fastqc.html"), CreateLinkOption.DELETE_EXISTING_FILE)
            fileService.createLink(viewFastqcResultsPath.resolve("${samplePrefix}_R1_fastqc.zip"),
                fastqcResultsPath.resolve("${samplePrefix}_R1_fastqc.zip"), CreateLinkOption.DELETE_EXISTING_FILE)
            fileService.createLink(viewFastqcResultsPath.resolve("${samplePrefix}_R2_fastqc.zip"),
                fastqcResultsPath.resolve("${samplePrefix}_R2_fastqc.zip"), CreateLinkOption.DELETE_EXISTING_FILE)
        }

        fileService.createLink(viewFastqcResultsPath.resolve("trace_${workflowRun.id}_${System.currentTimeMillis()}.txt"), traceFile, CreateLinkOption.DELETE_EXISTING_FILE)

        // Link log files
        (1..5).each { i ->
            Path sourceLog = logsPath.resolve("fastqc_log${i}_${individual.pid.toLowerCase()}.out")
            Path linkLog = viewLogsPath.resolve("fastqc_log${i}_${individual.pid.toLowerCase()}.out")
            fileService.createLink(linkLog, sourceLog, CreateLinkOption.DELETE_EXISTING_FILE)
        }

        println "Created symbolic links in view-by-pid structure for ${individual.pid}"
    } catch (Exception e) {
        println "Could not create symbolic links for ${individual.pid}, copying files instead: ${e.message}"
    }

    println "File structure created for ${individual.pid} (WorkflowRun: ${workflowRun.id})"
}

// Create comprehensive OTP file structure with UUID store and view-by-pid links
WorkflowRun.withNewTransaction {
    wesWorkflowRuns.each { ScenarioType st, WorkflowRun workflowRun ->
        if (!workflowRun) {
            println "No workflow run found, skipping file structure creation"
            return
        }
        createFileStructureForWorkflowRun(workflowRun, baseDir, "")
    }
}

println ""
println "WesFastQcWorkflow test data creation completed!"
println "Complete OTP file structure with UUID store and view-by-pid links has been created."
