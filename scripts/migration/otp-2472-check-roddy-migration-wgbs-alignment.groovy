/*
 * Copyright 2011-2026 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package migration

import groovy.json.JsonOutput
import groovy.transform.Field
import groovyx.gpars.GParsPool

import de.dkfz.tbi.otp.FileNotFoundException
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.exceptions.NotSupportedException
import de.dkfz.tbi.otp.workflow.alignment.roddy.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.concurrent.Semaphore
import java.util.regex.Matcher

/**
 * script to check migration for WGBS alignment.
 *
 * In case more config files are needed, add content of file to new variable and adapt creation at place of creation of config files.
 */

// ----------------------------------
// input

// the seqtypes to check for workflow
@Field
List<SeqType> seqTypes = [
        SeqTypeService.wholeGenomeBisulfitePairedSeqType,
        SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
]

// the workflow to check
List<String> workflows = [
        WgbsWorkflow.WORKFLOW,
]

// name of the roddy plugin
@Field
String roddyWorkflowPlugin = "AlignmentAndQCWorkflows"

// name of the roddy analysis
@Field
String roddyAnalysisConfiguration = 'bisulfiteCoreAnalysis'

// run the script first with the old config, then with the new config and give the path to the old config here
@Field
String resultOldTimestampDir = ""

// limit the list of checked configuration, used for testing. for the final, use a very big number, so all are taken
@Field
int limit = 5

// ----------------------------------
// script

@Field
ConfigService configService = ctx.configService
@Field
ConfigSelectorService configSelectorService = ctx.configSelectorService
@Field
ConfigFragmentService configFragmentService = ctx.configFragmentService
@Field
WorkflowService workflowService = ctx.workflowService
@Field
FileService fileService = ctx.fileService
@Field
FileSystemService fileSystemService = ctx.fileSystemService
@Field
RoddyConfigService roddyConfigService = ctx.roddyConfigService
@Field
RemoteShellHelper remoteShellHelper = ctx.remoteShellHelper
@Field
ProcessingOptionService processingOptionService = ctx.processingOptionService

@Field
FileSystem fileSystem = fileSystemService.remoteFileSystem

@Field
Semaphore semaphore = new Semaphore(10)

@Field
int parallel = 20

@Field
String coAppAndRef = ("""
            |<configuration name='coAppAndRef'
            |               description='This file is a workaround since Roddy plugin imports this config.'>
            |</configuration>
            |""".stripMargin())

String loadModule = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)
String activateJava = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_JAVA)
String activateGroovy = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY)
String roddyPath = processingOptionService.findOptionAsString(ProcessingOption.OptionName.RODDY_PATH)
String applicationIniPath = processingOptionService.findOptionAsString(ProcessingOption.OptionName.RODDY_APPLICATION_INI)
String featureTogglesConfigPath = processingOptionService.findOptionAsString(ProcessingOption.OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH)

@Field
Path base = fileSystem.getPath(configService.scriptOutputPath.toString()).resolve('checkXmlMigration').
        resolve(TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date()))
@Field
Path baseOld = resultOldTimestampDir ? fileSystem.getPath(configService.scriptOutputPath.toString()).resolve('checkXmlMigration').
        resolve(resultOldTimestampDir) : null

String unixGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_USER_LINUX_GROUP)
fileService.createDirectoryRecursivelyAndSetPermissions(base, unixGroup)
println "Base Directory:\n${base}"

List<String> handleRoddyCall(String cmd, Path commandOutput, String nameUsedInConfig, Path extractedOutput, String unixGroup) {
    ProcessOutput output
    semaphore.acquire()
    try {
        output = remoteShellHelper.executeCommandReturnProcessOutput(cmd)
    } finally {
        semaphore.release()
    }

    fileService.createFileWithContent(commandOutput, """
cmd:
${cmd}

----------------------------------------
${output.exitCode}

----------------------------------------
${output.stdout}

----------------------------------------
${output.stderr}
""", unixGroup)

    if (output.exitCode != 0) {
        throw new NotSupportedException("Roddy call fail, see ${commandOutput}")
    }

    if (output.stderr.contains("The project configuration \"${nameUsedInConfig}.config\" could not be found")) {
        throw new FileNotFoundException("Roddy could not find the configuration '${nameUsedInConfig}'. Probably some access problem.")
    }

    Map<String, String> res = [:]
    output.stdout.eachLine { String line ->
        Matcher matcher = line =~ /(?:declare +-x +(?:-i +)?)?([^ =]*)=(.*)/
        if (matcher.matches()) {
            String key = matcher.group(1)
            String value = matcher.group(2)
            res[key] = value.startsWith("\"") && value.length() > 2 ? value.substring(1, value.length() - 1) : value
        }
    }

    if (res.isEmpty()) {
        throw new ParsingException("Could not extract any configuration value from the roddy output")
    }

    List<String> result = res.collect {
        [
                it.key,
                it.value,
        ].join(' = ')
    }.findAll {
        /**
         * remove two false positives:
         * - remove empty lines
         * - workflowEnvironmentScript: is set in old workflow system via resource set, not used by printidlessruntimeconfig,
         *   therefore the value for old are always wrong. Remove it reduce wrong output
         * - USERGROUP: is getting from system, but do it for so often cause many values. Therefore remove it to reduce output.
         */
        it && !it.startsWith('workflowEnvironmentScript') &&
                !it.startsWith('USERGROUP =')
    }.sort()

    fileService.createFileWithContent(extractedOutput, result.join('\n') + '\n', unixGroup)
    return result
}

List<WorkflowVersion> workflowVersions = workflows.collectMany {
    WorkflowVersion.findAllByApiVersionInList(WorkflowApiVersion.findAllByWorkflow(workflowService.getExactlyOneWorkflow(it)))
}
List<WorkflowVersionSelector> versionSelectors = WorkflowVersionSelector.findAllByDeprecationDateIsNullAndWorkflowVersionInListAndSeqTypeInList(workflowVersions, seqTypes)
        .sort {
            it.id
        }
        .take(limit)
        .each { WorkflowVersionSelector selector ->
            println([
                    selector.id,
                    selector.project,
                    selector.seqType.displayNameWithLibraryLayout,
                    selector.workflowVersion.workflow.name,
                    selector.workflowVersion.workflowVersion,
            ].join(' \t'))
        }

GParsPool.withPool(parallel) {
    String output = versionSelectors.collectParallel { WorkflowVersionSelector workflowVersionSelector ->
        List<?> out = []
        Path work = null
        Path workOld = null
        try {
            SessionUtils.withTransaction {
                workflowVersionSelector = WorkflowVersionSelector.get(workflowVersionSelector.id)
                out.addAll([
                        '\n======',
                        workflowVersionSelector,
                        workflowVersionSelector.project,
                        workflowVersionSelector.seqType.displayNameWithLibraryLayout,
                        workflowVersionSelector.workflowVersion.workflow.name,
                        workflowVersionSelector.workflowVersion.workflowVersion,
                ])

                // base directory
                String projectName = "${workflowVersionSelector.project.name} ${workflowVersionSelector.seqType.displayNameWithLibraryLayout}".replaceAll(
                        '[^a-zA-Z0-9_]', '-')
                String plugin = workflowVersionSelector.workflowVersion.workflow.name.replaceAll('[^a-zA-Z0-9_]', '-')
                String version = workflowVersionSelector.workflowVersion.workflowVersion
                work = base.resolve(plugin).resolve(version).resolve(projectName)
                workOld = baseOld ? baseOld.resolve(plugin).resolve(version).resolve(projectName) : null
                fileService.createDirectoryRecursivelyAndSetPermissions(work, unixGroup)

                SingleSelectSelectorExtendedCriteria extendedCriteria = new SingleSelectSelectorExtendedCriteria(
                        workflowVersionSelector.workflowVersion.workflow,
                        workflowVersionSelector.workflowVersion,
                        workflowVersionSelector.project,
                        workflowVersionSelector.seqType,
                        null,
                        null,
                )

                List<ExternalWorkflowConfigSelector> selectors = configSelectorService.findAllSelectorsSortedByPriority(extendedCriteria)
                List<ExternalWorkflowConfigFragment> fragments = selectors*.externalWorkflowConfigFragment
                fileService.createFileWithContent(work.resolve('21-selectors'), selectors*.name.join('\n') + '\n', unixGroup)

                String fragmentJson = configFragmentService.mergeSortedFragments(fragments)
                fileService.createFileWithContent(work.resolve('22-json'), JsonOutput.prettyPrint(fragmentJson) + '\n', unixGroup)

                // parameter
                String combinedConfig = fragmentJson
                Map<String, String> specificConfig = [:]
                Path inputDir = fileSystem.getPath('$USERHOME/temp/testproject/vbp')// value taken from roddy base config to have no difference
                Path outputDir = fileSystem.getPath('$USERHOME/temp/testproject/rpp')// value taken from roddy base config to have no difference
                String queue = 'devel'
                boolean filenameSectionKillSwitch = true

                String newXml = roddyConfigService.createRoddyXmlConfig(
                        combinedConfig,
                        specificConfig,
                        roddyWorkflowPlugin,
                        workflowVersionSelector.workflowVersion,
                        roddyAnalysisConfiguration,
                        inputDir,
                        outputDir,
                        queue,
                        filenameSectionKillSwitch
                )
                fileService.createFileWithContent(work.resolve('23-xml'), newXml + '\n', unixGroup)

                Path configDir = work.resolve('config')
                fileService.createFileWithContent(configDir.resolve('config.xml'), newXml, unixGroup)
                fileService.createFileWithContent(configDir.resolve('coAppAndRef.xml'), coAppAndRef, unixGroup)

                String cmdNew = [
                        loadModule,
                        activateGroovy,
                        activateJava,
                        [
                                "${roddyPath}/roddy.sh",
                                "printidlessruntimeconfig",
                                "${RoddyConfigService.CONFIGURATION_NAME}@${RoddyConfigService.ANALYSIS_ID}",
                                "--useconfig=${applicationIniPath}",
                                "--usefeaturetoggleconfig=${featureTogglesConfigPath}",
                                "--configurationDirectories=${configDir}",
                        ].join(' '),
                ].join('\n')

                List<String> resultNew = handleRoddyCall(cmdNew,
                        work.resolve('24-roddyCall'),
                        RoddyConfigService.CONFIGURATION_NAME,
                        work.resolve('25-resultExtracted'),
                        unixGroup)

                if (workOld) {
                    List<String> resultOld = workOld.resolve('25-resultExtracted').readLines()

                    // ------------------
                    // checkForEquals
                    Set c1Set = resultOld.toSet()
                    Set c2Set = resultNew.toSet()
                    Set c3Set = resultOld.toSet()
                    c1Set.removeAll(resultNew)
                    c2Set.removeAll(resultOld)
                    c3Set.removeAll(c1Set)
                    String compared = """
In resultOld, but not in resultNew (${c1Set.size()}):
${c1Set*.toString().sort().join('\n')}

In resultNew, but not in resultOld  (${c2Set.size()}):
${c2Set*.toString().sort().join('\n')}

in both (${c3Set.size()}):
${c3Set*.toString().sort().join('\n')}
"""
                    fileService.createFileWithContent(work.resolve('31-onlyInOld'), c1Set*.toString().sort().join('\n') + '\n', unixGroup)
                    fileService.createFileWithContent(work.resolve('32-onlyInNew'), c2Set*.toString().sort().join('\n') + '\n', unixGroup)
                    fileService.createFileWithContent(work.resolve('33-inBoth'), c3Set*.toString().sort().join('\n') + '\n', unixGroup)
                    fileService.createFileWithContent(work.resolve('34-compared'), compared, unixGroup)

                    if (c1Set || c2Set) {
                        out << """
In resultOld, but not in resultNew (${c1Set.size()}):
${c1Set*.toString().sort().join('\n')}

In resultNew, but not in resultOld  (${c2Set.size()}):
${c2Set*.toString().sort().join('\n')}
"""
                    }
                }
            }
        } catch (Throwable t) {
            println t
            String stacktrace = StackTraceUtils.getStackTrace(t)
            out << "\n Exception:"
            out << stacktrace
            Path exceptionOut = work ? work.resolve('44-exception') : base.resolve("exceptionOut-${workflowVersionSelector.id}")
            fileService.createFileWithContent(exceptionOut, stacktrace, unixGroup)
        }
        return out.join('\n')
    }.join('\n\n')
    println output
}
''
