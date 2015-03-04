import grails.util.Environment
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.ngsdata.*

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
            fileName = 'DataFileFileName.gz'
            vbpFileName = 'DataFileVbpFileName.gz'
        }
        'de.dkfz.tbi.otp.ngsdata.ExomeEnrichmentKit' {
            name = {'name_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.ExomeEnrichmentKitSynonym' {
            name = {'name_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.Individual' {
            pid = {'pid_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.Project' {
            name = {'projectName_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.Realm' {
            name = 'FakeRealm'
            env = Environment.current.name
            rootPath = '/tmp/otp-unit-test/root'
            processingRootPath = '/tmp/otp-unit-test/processing'
            loggingRootPath = '/tmp/otp-unit-test/log'
            programsRootPath = '/tmp/otp-unit-test/programs'
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
        'de.dkfz.tbi.otp.ngsdata.Run' {
            name = {'runName_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.SampleType' {
            name = {'sampleTypeName_' + (counter++)}
            specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        }
        'de.dkfz.tbi.otp.ngsdata.SeqCenter' {
            name = {'seqCenterName_' + (counter++)}
            dirName  = {'seqCenterDirName_' + (counter++)}
        }
        'de.dkfz.tbi.otp.ngsdata.SeqType' {
            name = {'seqTypeName_' + (counter++)}
            libraryLayout  = {'seqTypelibraryLayout_' + (counter++)}
            dirName  = {'seqTypeDirName_' + (counter++)}
        }
        'de.dkfz.tbi.otp.dataprocessing.AlignmentPass' {
            referenceGenome = {ReferenceGenome.build()}
        }
        'de.dkfz.tbi.otp.dataprocessing.MergingSetAssignment' {
            //Ensure to use this subclass of AbstractBamFile
            //Otherwise the plugin tries to create an ExternallyProcessedMergedBamFile, but it fails to create the FastqSet
            bamFile = {ProcessedBamFile.build()}
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
}


//disable plugin in production enviromant
environments {
    production {
        testDataConfig {
            enabled = false
        }
    }
}
