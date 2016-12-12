package workflows.analysis.pair

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import org.junit.*

@Ignore
class IndelWorkflowTests extends AbstractBamFilePairAnalysisWorkflowTests {

    static final String PLUGIN_NAME = 'IndelCallingWorkflow'
    static final String PLUGIN_VERSION = '1.0.167'
    static final String PLUGIN = "${PLUGIN_NAME}:${PLUGIN_VERSION}"
    static final String CONFIG_VERSION = 'v1_0'
    static final String ANALYSIS = 'indelCallingAnalysis'
    static final String IMPORT = 'otpIndelCallingWorkflow-1.0'


    @Test
    void testWholeWorkflowWithRoddyBamFile() {
        setupRoddyBamFile()
        createConfig()

        execute()
        check()
    }

    @Test
    void testWholeWorkflowWithProcessedMergedBamFile() {
        setupProcessMergedBamFile()
        createConfig()

        execute()
        check()
    }

    File createXml() {
        String name = RoddyWorkflowConfig.getNameUsedInConfig(
                Pipeline.Name.RODDY_INDEL,
                seqType,
                PLUGIN,
                CONFIG_VERSION
        )

        String xml = """
<configuration configurationType='project'
            name='${name}'
            imports="${IMPORT}"
            description='Indel project configuration for WGS in OTP.'>
    <subconfigurations>
        <configuration name='config' usedresourcessize='t'>
            <availableAnalyses>
                <analysis id='${seqType.roddyName}' configuration='${ANALYSIS}'/>
            </availableAnalyses>
        </configuration>
    </subconfigurations>
</configuration>
"""

        File configFilePath = RoddyWorkflowConfig.getStandardConfigFile(
                project,
                Pipeline.Name.RODDY_INDEL,
                seqType,
                PLUGIN_VERSION,
                CONFIG_VERSION
        )

        File configDirectory = configFilePath.parentFile
        String md5 = HelperUtils.getRandomMd5sum()

        String script = """\
#!/bin/bash
set -evx

umask 0027

mkdir -p -m 2750 ${configDirectory}

cat <<${md5} > ${configFilePath}
${xml}
${md5}

chmod 0440 ${configFilePath}

echo 'OK'
"""

        LogThreadLocal.withThreadLog(System.out) {
            assert executionService.executeCommand(realm, script).trim() == "OK"
        }

        return configFilePath
    }


    @Override
    ConfigPerProject createConfig() {
        File xmlFilePath = createXml()

        config = RoddyWorkflowConfig.importProjectConfigFile(
                project,
                seqType,
                PLUGIN,
                DomainFactory.createIndelPipelineLazy(),
                xmlFilePath.absolutePath,
                CONFIG_VERSION,
        )
    }

    @Override
    ReferenceGenome createReferenceGenome() {
        return createAndSetup_Bwa06_1K_ReferenceGenome()
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RoddyIndelWorkflow.groovy",
                "scripts/initializations/AddPathToConfigFilesToProcessingOptions.groovy",
                "scripts/initializations/AddRoddyPathAndVersionToProcessingOptions.groovy",
        ]
    }

    void check() {
        IndelCallingInstance createdInstance = IndelCallingInstance.listOrderById().last()
        assert createdInstance.processingState == AnalysisProcessingStates.FINISHED
        assert createdInstance.config == config
        assert createdInstance.sampleType1BamFile == bamFileTumor
        assert createdInstance.sampleType2BamFile == bamFileControl

        [
                createdInstance.getResultFilePathsToValidate(),
                createdInstance.getCombinedPlotPath(),
        ].flatten().each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(it)
        }
    }

    @Override
    File getWorkflowData() {
        new File(getDataDirectory(), 'indel')
    }
}
