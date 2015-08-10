import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.util.Environment

/**
 * A counter used to handle unique constraints. You can use it in a closure to produce a unique value for a property.
 * The class Individual show therefore an example.
 * Attention: The value is not reset between test. If you need specific values in a test, you need to reset the counter with:
 * <code>grails.buildtestdata.TestDataConfigurationHolder.reset() <code>
 */
def counter = 0

testDataConfig {
    sampleData {
        'de.dkfz.tbi.otp.infrastructure.ClusterJob' {
            jobClass = 'testJobClass'
            clusterJobName = {"clusterJob${counter++}_testJobClass"}
        }
        'de.dkfz.tbi.otp.ngsdata.DataFile' {
            fileName = 'DataFileFileName_R1.gz'
            vbpFileName = 'DataFileFileName_R1.gz'
            pathName = 'path'
            fileType = {FileType.buildLazy(type: FileType.Type.SEQUENCE)}
        }
        'de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit' {
            name = {'name_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.LibraryPreparationKitSynonym' {
            name = {'name_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.Individual' {
            pid = {'pid_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.Project' {
            name = {'projectName_' + (counter++)}
            dirName = {'projectDir_' + (counter++)}
            realmName = DomainFactory.DEFAULT_REALM_NAME

        }
        'de.dkfz.tbi.otp.ngsdata.Realm' {
            name = DomainFactory.DEFAULT_REALM_NAME
            env = Environment.current.name
            rootPath = {new File(TestCase.uniqueNonExistentPath, 'root').path}
            processingRootPath = {new File(TestCase.uniqueNonExistentPath, 'processing').path}
            loggingRootPath = {new File(TestCase.uniqueNonExistentPath, 'log').path}
            programsRootPath = {new File(TestCase.uniqueNonExistentPath, 'programs').path}
            stagingRootPath = {new File(TestCase.uniqueNonExistentPath, 'staging').path}
            webHost = 'host.invalid'
            host = 'host.invalid'
            port = 22
            unixUser = '!fakeuser'
            timeout = 60
            pbsOptions = ''
            name = 'DKFZ'
            cluster = Realm.Cluster.DKFZ
        }
        'de.dkfz.tbi.otp.ngsdata.ReferenceGenome' {
            name = {'referenceGenomeName_' + (counter++)}
            path = {'referenceGenomePath_' + (counter++)}
            length = 789
            lengthWithoutN = 567
            lengthRefChromosomes = 345
            lengthRefChromosomesWithoutN = 123
        }
        'de.dkfz.tbi.otp.ngsdata.Run' {
            name = {'runName_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.RunSegment' {
            dataPath = {TestCase.getUniqueNonExistentPath()}
            mdPath = {TestCase.getUniqueNonExistentPath()}
        }
        'de.dkfz.tbi.otp.ngsdata.SampleType' {
            name = {'sampleTypeName_' + (counter++)}
            specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        }
        'de.dkfz.tbi.otp.ngsdata.SeqCenter' {
            name = {'seqCenterName_' + (counter++)}
            dirName  = {'seqCenterDirName_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.SeqPlatform' {
            seqPlatformGroup = {SeqPlatformGroup.build()}
            seqPlatformModelLabel = {SeqPlatformModelLabel.build()}
        }
        'de.dkfz.tbi.otp.ngsdata.SeqPlatformGroup' {
            name = {'testSeqPlatformGroup_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.SeqPlatformModelLabel' {
            name = {'platformModelLabelName_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.SequencingKitLabel' {
            name = {'sequencingKitLabel_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.SeqType' {
            name = {'seqTypeName_' + (counter++)}
            libraryLayout  = {'seqTypelibraryLayout_' + (counter++)}
            dirName  = {'seqTypeDirName_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.SoftwareTool' {
            programName = {'softwareTool_' + (counter++)}
            type = SoftwareTool.Type.BASECALLING
        }
        'de.dkfz.tbi.otp.ngsdata.SoftwareToolIdentifier' {
            name = {'softwareToolIdentifier_' + (counter++)}
        }
        'de.dkfz.tbi.otp.dataprocessing.MergingSet' {
            mergingWorkPackage = { MergingWorkPackage.build(workflow: DomainFactory.createDefaultOtpWorkflow()) }
        }
        'de.dkfz.tbi.otp.dataprocessing.MergingSetAssignment' {
            //Ensure to use this subclass of AbstractBamFile
            //Otherwise the plugin tries to create an ExternallyProcessedMergedBamFile, but it fails to create the FastqSet
            bamFile = {ProcessedBamFile.build()}
        }
        'de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage' {
            workflow = { DomainFactory.createDefaultOtpWorkflow() }
        }
        'de.dkfz.tbi.otp.dataprocessing.MockAbstractBamFile' {
            type = AbstractBamFile.BamType.SORTED
        }
        'de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile' {
            alignmentPass = {TestData.createAndSaveAlignmentPass()}
            type = AbstractBamFile.BamType.SORTED
        }
        'de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile' {
            type = AbstractBamFile.BamType.MDUP
        }
        'de.dkfz.tbi.otp.dataprocessing.ProcessedSaiFile' {
            alignmentPass = {TestData.createAndSaveAlignmentPass()}
        }
        'de.dkfz.tbi.otp.dataprocessing.QualityAssessmentMergedPass' {
            processedMergedBamFile = { TestData.createProcessedMergedBamFile(mergingPass: MergingPass.build()) }
        }
        RoddyQualityAssessment {
            qualityAssessmentMergedPass = { QualityAssessmentMergedPass.build(processedMergedBamFile: RoddyBamFile.build()) }
        }
        'de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig' {
            configFilePath = { new File(TestCase.uniqueNonExistentPath, 'roddy-workflow-config').path }
            workflow = { DomainFactory.createPanCanWorkflow() }
        }
        'de.dkfz.tbi.otp.utils.ExternalScript' {
            filePath = { new File(TestCase.uniqueNonExistentPath, 'ExternalScript').path }
        }
        'de.dkfz.tbi.otp.job.plan.JobExecutionPlan' {
            name = {'plan_' + (counter++)}
        }
        'de.dkfz.tbi.otp.job.processing.ParameterType' {
            name = {'parameterTypeName_' + (counter++)}
        }
        'de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate' {
            state: ExecutionState.CREATED
        }
    }
    unitAdditionalBuild = [
        'de.dkfz.tbi.otp.ngsdata.DataFile': [de.dkfz.tbi.otp.ngsdata.FileType],
        'de.dkfz.tbi.otp.ngsdata.ReferenceGenome': [de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType],
        'de.dkfz.tbi.otp.ngsdata.SeqPlatform': [de.dkfz.tbi.otp.ngsdata.SeqPlatformGroup, de.dkfz.tbi.otp.ngsdata.SeqPlatformModelLabel],
        'de.dkfz.tbi.otp.ngsdata.SeqTrack': [de.dkfz.tbi.otp.ngsdata.ChipSeqSeqTrack, de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack, de.dkfz.tbi.otp.ngsdata.DataFile],
        'de.dkfz.tbi.otp.dataprocessing.AbstractBamFile': [de.dkfz.tbi.otp.ngsdata.SeqTrack],
        'de.dkfz.tbi.otp.dataprocessing.QualityAssessmentMergedPass': [de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile],
        'de.dkfz.tbi.otp.dataprocessing.MergingSet': [de.dkfz.tbi.otp.dataprocessing.MergingSetAssignment],
        'de.dkfz.tbi.otp.dataprocessing.ConfigPerProject': [de.dkfz.tbi.otp.utils.ExternalScript],
    ]
}


//disable plugin in production environment
environments {
    production {
        testDataConfig {
            enabled = false
        }
    }
}
