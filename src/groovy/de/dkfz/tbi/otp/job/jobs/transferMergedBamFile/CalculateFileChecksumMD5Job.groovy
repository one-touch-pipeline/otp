package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile


import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class CalculateFileChecksumMD5Job extends AbstractJobImpl {

    final String JOB = "__pbsIds"
    final String REALM = "__pbsRealm"

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    PbsService pbsService

    @Autowired
    ConfigService configService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.get(id)

        assert bamFile.fileOperationStatus == FileOperationStatus.INPROGRESS

        Project project = processedMergedBamFileService.project(bamFile)
        Map<String, String> locations = processedMergedBamFileService.locationsForFileCopying(bamFile)
        Realm realm = configService.getRealmDataProcessing(project)
        String projectDir = configService.getProjectRootPath(project) + "/" + project.dirName
        String cmd = scriptText(bamFile, locations, projectDir)
        String jobId = pbsService.executeJob(realm, cmd)
        log.debug "Job " + jobId + " submitted to PBS"
        addOutputParameter(JOB, jobId)
        addOutputParameter(REALM, realm.id.toString())
    }

    private String scriptText(ProcessedMergedBamFile file, Map<String, String> locations, String projectDir) {
        QualityAssessmentMergedPass pass = qualityAssessmentMergedPassService.latestQualityAssessmentMergedPass(file)
        String qaResultDirectory = processedMergedBamFileQaFileService.directoryPath(pass)
        String qaResultMd5sumFile = processedMergedBamFileQaFileService.qaResultsMd5sumFile(file)
        String source = locations.get("sourceDirectory")
        String bamFile = locations.get("bamFile")
        String baiFile = locations.get("baiFile")
        String md5Bam = locations.get("md5BamFile")
        String picardMd5 = checksumFileService.picardMd5FileName(bamFile)
        String md5Bai = locations.get("md5BaiFile")
        String tmpDirectory = locations.get("temporalDestinationDir")

        // the md5sum of the merged bam file is written to the file md5Bam
        // the md5sum of the bai file is written to the file md5Bai
        // the md5sums of the qa results for the merged bam file and for the single lane bam files are written to the file "MD5SUMS"
        String text = """
mkdir -p -m 2750 ${tmpDirectory}
find ${projectDir} -user \${USER} -type d -not -perm 2750 -exec chmod 2750 '{}' \\;
cd ${source}
# The md5sum file produced by PICARD does not contain the name of the bam file.
# Therefore the bam file name is added to the md5 sum.
# To be consistent in the naming the md5sum file is renamed.

if [ -f ${picardMd5} ]
then
    if ! grep -q ${bamFile} ${picardMd5}
    then
        sed -e "s,\$,  ${bamFile}," -i ${picardMd5}
    fi
    mv ${picardMd5} ${md5Bam}
    chmod 0640 ${md5Bam}
fi
if [ ! -f ${md5Bai} ] || [ ! -s ${md5Bai} ]
then
    md5sum ${baiFile} > ${md5Bai}
    chmod 0640 ${md5Bai}
fi
cd ${qaResultDirectory}
rm -f ${qaResultMd5sumFile}
find . -type f -a -not -name ${processedMergedBamFileQaFileService.MD5SUM_NAME} -exec md5sum '{}' \\; >> ${qaResultMd5sumFile}
"""
        text += "chmod 0640 ${qaResultMd5sumFile} ; "
        return text
    }
}
