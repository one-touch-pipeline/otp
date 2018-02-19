package workflows.analysis.pair.snv

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*

abstract class AbstractRoddySnvWorkflowTests extends AbstractSnvWorkflowTests {

    RoddyWorkflowConfigService roddyWorkflowConfigService

    static final String PLUGIN_NAME = 'SNVCallingWorkflow'
    static final String PLUGIN_VERSION = '1.0.166-1'
    static final String PLUGIN = "${PLUGIN_NAME}:${PLUGIN_VERSION}"
    static final String CONFIG_VERSION = 'v1_0'
    static final String ANALYSIS = 'snvCallingAnalysis'
    static final String IMPORT = 'otpSNVCallingWorkflowWGS-1.0'


    File createXml() {
        String name = RoddyWorkflowConfig.getNameUsedInConfig(
                Pipeline.Name.RODDY_SNV,
                seqType,
                PLUGIN,
                CONFIG_VERSION
        )

        String xml = """
<configuration configurationType='project'
            name='${name}'
            imports="${IMPORT}"
            description='SNV project configuration for ${seqType.roddyName} in OTP.'>
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
                Pipeline.Name.RODDY_SNV,
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

        config = roddyWorkflowConfigService.importProjectConfigFile(
                project,
                seqType,
                PLUGIN,
                DomainFactory.createRoddySnvPipelineLazy(),
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
                "scripts/workflows/RoddySnvWorkflow.groovy",
                "scripts/initializations/AddPathToConfigFilesToProcessingOptions.groovy",
                "scripts/initializations/AddRoddyPathAndVersionToProcessingOptions.groovy",
        ]
    }

    @Override
    void checkSpecific() {
        SnvCallingInstance createdInstance = SnvCallingInstance.listOrderById().last()
        File instancePath = createdInstance.getInstancePath().absoluteDataManagementPath

        [
                SnvCallingStep.CALLING,
                SnvCallingStep.SNV_DEEPANNOTATION
        ].each {
            File file = new File(instancePath, it.getResultFileName(individual))
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
        }
    }


}
