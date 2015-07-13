import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.job.processing.ExecutionState
import grails.util.Environment
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile

/**
 * A counter used to handle unique constraints. You can use it in a closure to produce a unique value for a property.
 * The class Individual show therefore an example.
 * Attention: The value is not reset between test. If you need specific values in a test, you need to reset the counter with:
 * <code>grails.buildtestdata.TestDataConfigurationHolder.reset() <code>
 */
def counter = 0

testDataConfig {
    sampleData {
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
            webHost = 'localhost'
            host = 'localhost'
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
        'de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType' {
            statSizeFileName = DomainFactory.DEFAULT_TAB_FILE_NAME
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
        'de.dkfz.tbi.otp.dataprocessing.MergingSetAssignment' {
            //Ensure to use this subclass of AbstractBamFile
            //Otherwise the plugin tries to create an ExternallyProcessedMergedBamFile, but it fails to create the FastqSet
            bamFile = {ProcessedBamFile.build()}
        }
        'de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage' {
            workflow = { Workflow.buildLazy() }
            statSizeFileName = DomainFactory.DEFAULT_TAB_FILE_NAME
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
        'de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig' {
            configFilePath = {
                File config = new File("${TestCase.TEST_DIRECTORY}/default-roddy-worflow-config")
                if (!config.exists()) {
                    config.parentFile.mkdirs()
                    config.parentFile.deleteOnExit()
                    config.deleteOnExit()
                    config << "configuration"
                }
                return config.absolutePath
            }
            workflow = { Workflow.buildLazy(name: Workflow.Name.PANCAN_ALIGNMENT, type: Workflow.Type.ALIGNMENT) }
        }
        'de.dkfz.tbi.otp.utils.ExternalScript' {
            filePath = {"${TestCase.TEST_DIRECTORY}/ExternalScript_${counter++}"}
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
