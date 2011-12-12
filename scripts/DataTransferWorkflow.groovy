import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterMapping
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage

JobExecutionPlan jep = new JobExecutionPlan(
    name: "DataTransferWorkflow",
    planVersion: 0,
    enabled: true
)
assert(jep.save())

/*
 * Start job looks over runs with loaded meta-data 
 * (finished metaDataWorkflow) and create new processes.
 * Start job controlls number of processes running
 */
StartJobDefinition definition = new StartJobDefinition(
    name: "start",
    bean: "dataTransferStartJob",
    plan: jep
 )
assert(definition.save())
jep.startJob = definition

/*
 * Thie job will check if input files exists on file system.
 * This job is end state aware
 */
JobDefinition checkInputFilesJob = new JobDefinition(
    name: "checkInputFiles",
    bean: "checkInputFiles",
    plan: jep
)
assert(checkInputFilesJob.save())

/*
 * This jobs creates output directories if needed
 */
JobDefinition createOutputDirectoryJob = new JobDefinition(
    name: "createOutputDirectoy",
    baen: "createOutputDirectory",
    plan: jep,
    previous: checkInputFilesJob
)
assert(createOutputDirectoryJob.save())

/*
 * This job sends PBS jobs with copy command
 * Output of this job shall be list of PBS jobs
 */
JobDefinition copyFilesJob = new JobDefinition(
    name: "copyFilesJob",
    bean: "copyFilesJob",
    plan: jep,
    previous: createOutputDirectoryJob
)
assert(copyFilesJob.save())

/*
 * This job watch-dog PBS copu jobs
 * This job uses universal bean for watchdog
 */
JobDefinition copyFilesWatchdogJob = new JobDefinition(
    name: "copyFilesWatchdogJob",
    bean: "pbsWatchdog",
    plan: jep,
    previous: copyFilesJob
)
assert(copyFilesWatchdogJob.save())

/*
 * This jobs checks if files are in the final location
 * and fills statistics (file size, creation date) 
 * into DataFile objects
 */
JobDefinition checkFinalLocationJob = new JobDefinition(
    name: "checkFinalLoaction",
    bean: "checkFinalLocation",
    plan: jep,
    previous: copyFilesWatchdogJob,
    milestone: true
)
assert(checkFinalLocationJob.save())

/*
 * This job will evelutate performance of the copy job
 * from the time of the job and file size.
 * Non essential but for throuput optimization
 */
JobDefinition copyStatisticsJob = new JobDefinition(
    name: "copyStatistics",
    bean: "copyStatistics",
    plan: jep,
    previous: checkFinalLocationJob
)
assert(copyStatisticsJob.save())

/*
 * This job calculates md5sum
 * for each file an md5sum program is run on PBS
 * the output is appended to the text file 
 * The output of this job is a list of PBS job ids
 */
JobDefinition md5sumJob = new JobDefinition(
    name: "md5sum",
    bean: "md5sum",
    plan: jep,
    previous: copyStatisticsJob
)
assert(md5sumJob.save())

/*
 * Watchdog for md5sum
 */
JobDefinition md5sumWatchdogJob = new JobDefinition(
    name: "md5sumWatchdog",
    beam: "pbsWatchdog",
    plan: jep,
    previous: md5sumJob
)
assert(md5sumWatchdogJob.save())

/*
 * This job parses md5sum file and compares values with
 * original values froder in DB. This job is end state aware
 */
JobDefinition compareMd5sumJob = new JobDefinition(
    name: "compareMd5sum",
    bean: "compareMd5sum",
    plan: jep,
    previous: md5sumWatchdogJob,
    milestone: true
)
assert(compareMd5sumJob.save())


/*
 * Maping of the output to input
 * used to connect PBS jobs to watchdogs
 */



// set the first job
jep.firstJob = checkInputFilesJob
assert(jep.save(flush: true))


