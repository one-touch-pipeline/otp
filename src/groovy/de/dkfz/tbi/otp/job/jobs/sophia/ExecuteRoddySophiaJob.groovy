package de.dkfz.tbi.otp.job.jobs.sophia

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*
import org.hibernate.*

@Component
@Scope("prototype")
@UseJobLog
class ExecuteRoddySophiaJob extends AbstractExecutePanCanJob<SophiaInstance> implements AutoRestartableJob {

    @Autowired
    SophiaService sophiaService

    @Override
    protected List<String> prepareAndReturnWorkflowSpecificCValues(SophiaInstance sophiaInstance) {
        assert sophiaInstance

        sophiaService.validateInputBamFiles(sophiaInstance)

        AbstractMergedBamFile bamFileDisease = sophiaInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = sophiaInstance.sampleType2BamFile
        File bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing
        File bamFileControlPath = bamFileControl.pathForFurtherProcessing
        File diseaseInsertSizeFile = bamFileDisease.finalInsertSizeFile
        File controlInsertSizeFile = bamFileControl.finalInsertSizeFile
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(diseaseInsertSizeFile)
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(controlInsertSizeFile)

        Integer tumorDefaultReadLength = bamFileDisease.getMaximalReadLength()
        Integer controlDefaultReadLength = bamFileControl.getMaximalReadLength()

        assert tumorDefaultReadLength && controlDefaultReadLength: "neither tumorDefaultReadLength nor controlDefaultReadLength may be null"

        List<String> cValues = []

        if (([bamFileDisease, bamFileControl].every { RoddyBamFile.isAssignableFrom(Hibernate.getClass(it)) })) {
            RoddyQualityAssessment bamFileDiseaseQualityAssessment = bamFileDisease.getOverallQualityAssessment() as RoddyQualityAssessment
            RoddyQualityAssessment bamFileControlQualityAssessment = bamFileControl.getOverallQualityAssessment() as RoddyQualityAssessment

            cValues.add("controlMedianIsize:${bamFileControlQualityAssessment.insertSizeMedian}")
            cValues.add("tumorMedianIsize:${bamFileDiseaseQualityAssessment.insertSizeMedian}")
            cValues.add("controlStdIsizePercentage:${bamFileControlQualityAssessment.insertSizeCV}")
            cValues.add("tumorStdIsizePercentage:${bamFileDiseaseQualityAssessment.insertSizeCV}")
            cValues.add("controlProperPairPercentage:${bamFileControlQualityAssessment.getPercentProperlyPaired()}")
            cValues.add("tumorProperPairPercentage:${bamFileDiseaseQualityAssessment.getPercentProperlyPaired()}")
        }

        cValues.add("bamfile_list:${bamFileControlPath};${bamFileDiseasePath}")
        cValues.add("sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}")
        cValues.add("insertsizesfile_list:${diseaseInsertSizeFile};${controlInsertSizeFile}")
        cValues.add("possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}")
        cValues.add("controlDefaultReadLength:${controlDefaultReadLength}")
        cValues.add("tumorDefaultReadLength:${tumorDefaultReadLength}")

        return cValues
    }

    @Override
    protected String prepareAndReturnWorkflowSpecificParameter(SophiaInstance sophiaInstance) {
        return ""
    }


    @Override
    protected void validate(SophiaInstance sophiaInstance) throws Throwable {
        assert sophiaInstance : "The input sophiaInstance must not be null"

        executeRoddyCommandService.correctPermissionsAndGroups(sophiaInstance, sophiaInstance.project.realm)

        List<File> directories = [
                sophiaInstance.workExecutionStoreDirectory
        ]
        directories.addAll(sophiaInstance.workExecutionDirectories)

        List<File> files = [
                sophiaInstance.getFinalAceseqInputFile(),
        ]

        directories.each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(it)
        }

        files.each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(it)
        }

        sophiaService.validateInputBamFiles(sophiaInstance)

        sophiaInstance.updateProcessingState(AnalysisProcessingStates.FINISHED)
    }
}