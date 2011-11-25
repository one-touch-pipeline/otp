import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterMapping
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage

JobExecutionPlan jep = new JobExecutionPlan(name: "example", planVersion: 0, enabled: true)
assert(jep.save())
StartJobDefinition definition = new StartJobDefinition(name: "example", bean: "exampleStartJob", plan: jep)
assert(definition.save())
jep.startJob = definition
JobDefinition unzipJob = new JobDefinition(name: "unzip", bean: "unzipJob", plan: jep)
assert(unzipJob.save())
JobDefinition untarJob = new JobDefinition(name: "untar", bean: "untarJob", plan: jep)
assert(untarJob.save())
unzipJob.next = untarJob
assert(untarJob.save())
JobDefinition tarJob = new JobDefinition(name: "tar", bean: "tarJob", plan: jep, previous: untarJob)
assert(tarJob.save())
untarJob.next = tarJob
assert(untarJob.save())
JobDefinition md5sum1Job = new JobDefinition(name: "origMd5Sum", bean: "md5SumJob", plan: jep, previous: tarJob)
assert(md5sum1Job.save())
tarJob.next = md5sum1Job
assert(tarJob.save())
JobDefinition md5sum2Job = new JobDefinition(name: "generatedMd5Sum", bean: "md5SumJob", plan: jep, previous: md5sum1Job)
assert(md5sum2Job.save())
md5sum1Job.next = md5sum2Job
assert(md5sum1Job.save())
JobDefinition compareJob = new JobDefinition(name: "compare", bean: "compareJob", plan: jep, previous: md5sum2Job)
assert(compareJob.save())
md5sum2Job.next = compareJob
assert(md5sum2Job.save())
// set the first job
jep.firstJob = unzipJob
assert(jep.save(flush: true))
// Parameters for Start Job
ParameterType type = new ParameterType(name: "directory", jobDefinition: definition, parameterUsage: ParameterUsage.INPUT)
assert(type.save())
Parameter parameter = new Parameter(type: type, value: "/tmp/otp/start")
assert(parameter.save())
ParameterType tarParameterType = new ParameterType(name: "file", jobDefinition: definition, parameterUsage: ParameterUsage.OUTPUT)
assert(tarParameterType.save())
definition.addToConstantParameters(parameter)
assert(definition.save(flush: true))

// Parameters for unzip Job
ParameterType unzipOutputDirectoryType = new ParameterType(name: "directory", jobDefinition: unzipJob, parameterUsage: ParameterUsage.INPUT)
assert(unzipOutputDirectoryType.save())
ParameterType zipFileType = new ParameterType(name: "zipFile", jobDefinition: unzipJob, parameterUsage: ParameterUsage.INPUT)
assert(zipFileType.save())
ParameterType unzipFileType = new ParameterType(name: "unzipFile", jobDefinition: unzipJob, parameterUsage: ParameterUsage.OUTPUT)
assert(unzipFileType.save())
Parameter unzipOutputDirectory = new Parameter(type: unzipOutputDirectoryType, value: "/tmp/otp/unzip")
assert(unzipOutputDirectory.save())
unzipJob.addToConstantParameters(unzipOutputDirectory)
// Parameter Mapping from start job to unzip Job
ParameterMapping unzipMapping = new ParameterMapping(from: tarParameterType, to: zipFileType, job: unzipJob)
assert(unzipMapping.save(flush: true))

// Parameters for untar Job
ParameterType directoryType = new ParameterType(name: "directory", jobDefinition: untarJob, parameterUsage: ParameterUsage.INPUT)
assert(directoryType.save())
ParameterType fileNameType = new ParameterType(name: "file", jobDefinition: untarJob, parameterUsage: ParameterUsage.INPUT)
assert(fileNameType.save())
ParameterType outputFilesType = new ParameterType(name: "extractedFiles", jobDefinition: untarJob, parameterUsage: ParameterUsage.OUTPUT)
assert(outputFilesType.save())
Parameter outputDirectoryParameter = new Parameter(type: directoryType, value: "/tmp/otp/untar")
assert(outputDirectoryParameter.save())
untarJob.addToConstantParameters(outputDirectoryParameter)
assert(untarJob.save())
ParameterType fileNamePassthroughType = new ParameterType(name: "fileNamePassthrough", jobDefinition: untarJob, parameterUsage: ParameterUsage.PASSTHROUGH)
assert(fileNamePassthroughType.save())
// ParameterMapping from start job to untar job
ParameterMapping mapping = new ParameterMapping(from: unzipFileType, to: fileNameType, job: untarJob)
assert(mapping.save(flush: true))
ParameterMapping untarParameterMapping = new ParameterMapping(from: unzipFileType, to: fileNamePassthroughType, job: untarJob)
assert(untarParameterMapping.save())

// Parameters for tar Job
ParameterType tarInputFilesType = new ParameterType(name: "files", jobDefinition: tarJob, parameterUsage: ParameterUsage.INPUT)
assert(tarInputFilesType.save())
ParameterType tarTargetDirectoryType = new ParameterType(name: "directory", jobDefinition: tarJob, parameterUsage: ParameterUsage.INPUT)
assert(tarTargetDirectoryType.save())
ParameterType outputTarFileType = new ParameterType(name: "tarFile", jobDefinition: tarJob, parameterUsage: ParameterUsage.OUTPUT)
assert(outputTarFileType.save())
Parameter constantTarDirectory = new Parameter(type: tarTargetDirectoryType, value: "/tmp/otp/tar")
assert(constantTarDirectory.save())
tarJob.addToConstantParameters(constantTarDirectory)
assert(tarJob.save())
ParameterType fileNamePassthrough2Type = new ParameterType(name: "fileNamePassthrough", jobDefinition: tarJob, parameterUsage: ParameterUsage.PASSTHROUGH)
assert(fileNamePassthrough2Type.save())
// ParameterMapping from untar job to tar job
ParameterMapping filesMapping = new ParameterMapping(from: outputFilesType, to: tarInputFilesType, job: tarJob)
assert(filesMapping.save(flush: true))
ParameterMapping tarParameterMapping = new ParameterMapping(from: fileNamePassthroughType, to: fileNamePassthrough2Type, job: tarJob)
assert(tarParameterMapping.save())

// Parameter for first MD5SumJob
ParameterType fileCheck1Type = new ParameterType(name: "file", jobDefinition: md5sum1Job, parameterUsage: ParameterUsage.INPUT)
assert(fileCheck1Type.save())
ParameterType md5Sum1Type = new ParameterType(name: "md5sum", jobDefinition: md5sum1Job, parameterUsage: ParameterUsage.OUTPUT)
assert(md5Sum1Type.save())
ParameterType fileNamePassthrough3Type = new ParameterType(name: "fileNamePassThrough", jobDefinition: md5sum1Job, parameterUsage: ParameterUsage.PASSTHROUGH)
assert(fileNamePassthrough3Type.save())
ParameterMapping md5sumPassthroughMapping = new ParameterMapping(from: outputTarFileType, to: fileNamePassthrough3Type, job: md5sum1Job)
assert(md5sumPassthroughMapping.save())
ParameterMapping md5sumMapping = new ParameterMapping(from: fileNamePassthrough2Type, to: fileCheck1Type, job: md5sum1Job)
assert(md5sumMapping.save())

// Parameter for second MD5SumJob
ParameterType fileCheck2Type = new ParameterType(name: "file", jobDefinition: md5sum2Job, parameterUsage: ParameterUsage.INPUT)
assert(fileCheck2Type.save())
ParameterType md5Sum2Type = new ParameterType(name: "md5sum", jobDefinition: md5sum2Job, parameterUsage: ParameterUsage.OUTPUT)
assert(md5Sum2Type.save())
ParameterType md5Sum2PassthroughType = new ParameterType(name: "passthrough", jobDefinition: md5sum2Job, parameterUsage: ParameterUsage.PASSTHROUGH)
assert(md5Sum2PassthroughType.save())
ParameterMapping md5sum2Mapping = new ParameterMapping(from: fileNamePassthrough3Type, to: fileCheck2Type, job: md5sum2Job)
assert(md5sum2Mapping.save())
ParameterMapping md5sum2PassthroughMapping = new ParameterMapping(from: md5Sum1Type, to: md5Sum2PassthroughType, job: md5sum2Job)
assert(md5sum2PassthroughMapping.save())

// Parameter for compare job
ParameterType compare1Type = new ParameterType(name: "value1", jobDefinition: compareJob, parameterUsage: ParameterUsage.INPUT)
assert(compare1Type.save())
ParameterType compare2Type = new ParameterType(name: "value2", jobDefinition: compareJob, parameterUsage: ParameterUsage.INPUT)
assert(compare2Type.save())
ParameterMapping compare1Mapping = new ParameterMapping(from: md5Sum2Type, to: compare1Type, job: compareJob)
assert(compare1Mapping.save())
ParameterMapping compare2Mapping = new ParameterMapping(from: md5Sum2PassthroughType, to: compare2Type, job: compareJob)
assert(compare2Mapping.save())
