import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterMapping
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.ProcessParameterType

JobExecutionPlan jep = new JobExecutionPlan(name: "loadMetaData", planVersion: 0, enabled: true)
assert(jep.save())
StartJobDefinition definition = new StartJobDefinition(name: "start", bean: "metaDataStartJob", plan: jep)
assert(definition.save())
jep.startJob = definition

JobDefinition registerInputFilesJob = new JobDefinition(name: "registerInputFiles", bean: "registerInputFilesJob", plan: jep)
assert(registerInputFilesJob.save())
JobDefinition loadMetaDataJob = new JobDefinition(name: "loadMetaData", bean: "metaDataJob", plan: jep, previous: registerInputFilesJob)
assert(loadMetaDataJob.save())
registerInputFilesJob.next = loadMetaDataJob
assert(registerInputFilesJob.save())
JobDefinition validateMetadataJob = new JobDefinition(name: "validateMetadata", bean: "validateMetadataJob", plan: jep, previous: loadMetaDataJob)
assert(validateMetadataJob.save())
loadMetaDataJob.next = validateMetadataJob
assert(loadMetaDataJob.save())
JobDefinition buildExecutionDateJob = new JobDefinition(name: "buildExecutionDate", bean: "buildExecutionDateJob", plan: jep, previous: validateMetadataJob)
assert(buildExecutionDateJob.save())
validateMetadataJob.next = buildExecutionDateJob
assert(validateMetadataJob.save())
JobDefinition buildSequenceTracksJob = new JobDefinition(name: "buildSequenceTracks", bean: "buildSequenceTracksJob", plan: jep, previous: buildExecutionDateJob)
assert(buildSequenceTracksJob.save())
buildExecutionDateJob.next = buildSequenceTracksJob
assert(buildExecutionDateJob.save())
JobDefinition checkSequenceTracksJob = new JobDefinition(name: "checkSequenceTracks", bean: "checkSequenceTracksJob", plan: jep, previous: buildSequenceTracksJob)
assert(checkSequenceTracksJob.save())
buildSequenceTracksJob.next = checkSequenceTracksJob
assert(buildSequenceTracksJob.save())
// set the first job
jep.firstJob = registerInputFilesJob
assert(jep.save(flush: true))
ProcessParameterType type = new ProcessParameterType(name: "run", plan: jep)
assert(type.save(flush: true))
