package workflows.snv

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*

abstract class AbstractRoddySnvWorkflowTests extends AbstractSnvWorkflowTests {

    static final String PLUGIN_NAME = 'COWorkflows'
    static final String PLUGIN_VERSION = '1.0.132-3'
    static final String PLUGIN = "${PLUGIN_NAME}:${PLUGIN_VERSION}"
    static final String CONFIG_VERSION = 'v1_0'
    static final String ANALYSIS = 'snvCallingAnalysis'
    static final String IMPORT = 'otpSnvCallingWorkflow-1.0'


    File createXml() {
        String name = RoddyWorkflowConfig.getNameUsedInConfig(
                Pipeline.Name.RODDY_SNV,
                seqType,
                PLUGIN,
                CONFIG_VERSION
        )

        String xml = """
<configuration
        configurationType='project'
        name='${name}'
        imports='${IMPORT}'
        description='All parameters specific for the SNV Calling Workflow.'>

    <subconfigurations>
        <configuration name='config' usedresourcessize='xl'>
            <availableAnalyses>
                <analysis id='${seqType.roddyName}' configuration='${ANALYSIS}' killswitches='FilenameSection'/>
            </availableAnalyses>
            <configurationvalues>
                <cvalue name='mpileupOutputDirectory' value='/' type="path" description="The output path must be exactly as defined on the command line in 'outputAnalysisBaseDirectory'"/>

                <cvalue name='disableAutoBAMHeaderAnalysis' value='true' type="boolean"
                        description="This variable defines if the reference genome and the chromosome file lenght are parsed from the bam files (false) or not (true).
                        We want to provide these values via the command line which is why we want to disable this function. "/>

                <!-- Software versions -->
                <cvalue name="BCFTOOLS_BIN" value="bcftools-0.1.19" type="filename"/>
                <cvalue name="BGZIP_BIN" value="" type="filename"/>
                <cvalue name="TABIX_BIN" value="" type="filename"/>
                <cvalue name="SAMTOOLS_BIN" value="samtools-0.1.19" type="filename"/>

            </configurationvalues>
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

        config = RoddyWorkflowConfig.importProjectConfigFile(
                project,
                seqType,
                PLUGIN,
                DomainFactory.createRoddySnvPipelineLazy(),
                xmlFilePath.absolutePath,
                CONFIG_VERSION,
        )
    }

    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/RoddySnvWorkflow.groovy"]
    }

    @Override
    void checkSpecific() {
        SnvCallingInstance createdInstance = SnvCallingInstance.listOrderById().last()
        File instancePath = createdInstance.getSnvInstancePath().absoluteDataManagementPath

        [
                SnvCallingStep.CALLING,
                SnvCallingStep.SNV_DEEPANNOTATION
        ].each {
            File file = new File(instancePath, it.getResultFileName(individual))
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
        }
    }


}
