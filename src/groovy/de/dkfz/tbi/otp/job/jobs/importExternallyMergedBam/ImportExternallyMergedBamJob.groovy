package de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

import java.nio.file.*

@Component
@Scope("prototype")
@UseJobLog
class ImportExternallyMergedBamJob extends AbstractOtpJob {

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    FileSystemService fileSystemService


    @Override
    protected final AbstractMultiJob.NextAction maybeSubmit() throws Throwable {
        final ImportProcess importProcess = getProcessParameterObject()
        AbstractMultiJob.NextAction action = AbstractMultiJob.NextAction.SUCCEED

        String moduleLoader = ProcessingOptionService.findOptionSafe(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER, null, null)
        String samtoolsActivation = ProcessingOptionService.findOptionSafe(ProcessingOption.OptionName.COMMAND_ACTIVATION_SAMTOOLS, null, null)
        String groovyActivation = ProcessingOptionService.findOptionSafe(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY, null, null)
        String samtoolsCommand = ProcessingOptionService.findOptionAssure(ProcessingOption.OptionName.COMMAND_SAMTOOLS, null, null)
        String groovyCommand = ProcessingOptionService.findOptionAssure(ProcessingOption.OptionName.COMMAND_GROOVY, null, null)
        File otpScriptDir = configService.getToolsPath()

        importProcess.externallyProcessedMergedBamFiles.each { ExternallyProcessedMergedBamFile epmbf ->
            Realm realm = epmbf.project.realm
            File sourceBam = new File(epmbf.importedFrom)
            File sourceBaseDir = sourceBam.parentFile
            File sourceBai = new File(sourceBaseDir, epmbf.baiFileName)

            File targetBam = epmbf.bamFile
            File targetBai = epmbf.baiFile
            File targetBaseDir = epmbf.getImportFolder()
            File checkpoint = new File(targetBaseDir, ".${epmbf.bamFileName}.checkpoint")

            String updateBaseDir = "sed -e 's#${sourceBaseDir}#${targetBaseDir}#'"

            if (checkpoint.exists()) {
                log.debug("Checkpoint found for ${sourceBam}, skip copying")
            } else {
                action = AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS
                String md5sumBam
                if (epmbf.md5sum) {
                    md5sumBam = "echo ${epmbf.md5sum}  ${targetBam} > ${targetBam}.md5sum"
                } else {
                    md5sumBam = "md5sum ${sourceBam} | ${updateBaseDir} > ${targetBam}.md5sum"
                }

                String furtherFilesSource = epmbf.furtherFiles.collect {
                    new File(sourceBaseDir, it)
                }.join(' ')
                String furtherFilesTarget = epmbf.furtherFiles.collect{
                    new File(targetBaseDir, it)
                }.join(' ')

                String furtherFilesCopy = epmbf.furtherFiles.collect { String relativePath ->
                    File sourceFurtherFile = new File(sourceBaseDir, relativePath)
                    File targetFurtherFile = new File(targetBaseDir, relativePath)
                    return "mkdir -p -m 2750 ${targetFurtherFile.parent}\n" +
                            "cp -HLR ${sourceFurtherFile} ${targetFurtherFile}"
                }.join("\n")

                String cmd = """
set -o pipefail
set -v

${moduleLoader}
${samtoolsActivation}
${groovyActivation}

if [ -e "${targetBam.path}" ]; then
    echo "File ${targetBam.path} already exists."
    rm -rf ${targetBam.path}* ${furtherFilesTarget}
fi

mkdir -p -m 2750 ${targetBam.parent}
# copy and calculate max read length at the same time
cat ${sourceBam} | tee ${targetBam} | ${samtoolsCommand} view - | ${groovyCommand} ${otpScriptDir}/bamMaxReadLength.groovy > ${epmbf.bamMaxReadLengthFile}
cp -HL ${sourceBai} ${targetBai}

${furtherFilesCopy}

cd ${targetBam.parent}
${md5sumBam}
md5sum -c ${targetBam}.md5sum

md5sum ${sourceBai} | ${updateBaseDir} > ${targetBai}.md5sum
md5sum -c ${targetBai}.md5sum

md5sum `find -L ${furtherFilesSource} -type f` | ${updateBaseDir} > ${targetBaseDir}/md5sum.md5sum
md5sum -c ${targetBaseDir}/md5sum.md5sum

chgrp -R ${executionHelperService.getGroup(epmbf.project.projectDirectory)} ${targetBaseDir}
chmod 644 `find ${targetBaseDir} -type f`
chmod 750 `find ${targetBaseDir} -type d`

touch ${checkpoint}
"""
                clusterJobSchedulerService.executeJob(realm, cmd)
            }
        }
        if (action == AbstractMultiJob.NextAction.SUCCEED) {
            validate()
        }
        return action
    }

    @Override
    protected void validate() throws Throwable {
        final ImportProcess importProcess = getProcessParameterObject()

        final Collection<String> problems = importProcess.externallyProcessedMergedBamFiles.collect {
            FileSystem fs = fileSystemService.getFilesystemForBamImport()
            String path = it.getBamFile().path
            Path target = fs.getPath(path)

            try {
                Path maxReadLengthPath = fs.getPath(it.getBamMaxReadLengthFile().absolutePath)
                if (Files.exists(maxReadLengthPath)) {
                    it.maximumReadLength = maxReadLengthPath.text as Integer
                    assert it.save(flush: true)
                    Files.delete(maxReadLengthPath)
                }
                assert it.maximumReadLength

                FileService.ensureFileIsReadableAndNotEmpty(target)
                if (!it.md5sum) {
                    Path md5Path = fs.getPath(checksumFileService.md5FileName(path))
                    it.md5sum = checksumFileService.firstMD5ChecksumFromFile(md5Path)
                    it.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
                    it.save(flush: true)
                }
                return null
            } catch (Throwable t) {
                return "Copying of target ${target} failed, ${t.message}"
            }
        }.findAll()
        if (problems) {
            throw new ProcessingException(problems.join(","))
        }
    }
}
