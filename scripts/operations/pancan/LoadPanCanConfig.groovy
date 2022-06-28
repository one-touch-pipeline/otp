/*
 * Copyright 2011-2019 The OTP authors
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

/**
 * A script to load a the roddy pancan project config into the database.
 */

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*

String projectName = ''

// The PID is optional. It must only be set if the config is individual specific.
// If the PID is set, alignment will automatically triggered for that PID.
String individualPid = ''

//The roddy plugin version. It needs to be a valid value for Roddy.
//For example: AlignmentAndQCWorkflows:1.1.39
String programVersionToUse = ''

//The config version used. Has to be match the expression: '^v\d+_\d+$'
//The first version of a file has 'v1_0
String configVersion = 'v1_0'

/**
 * The seq type for which roddy should be configured, could be:
 * * WGS
 * * WES
 * * WGBS
 * * WGBSTAG
 * * RNA
 */
String seqTypeRoddyName = ''

String libraryLayout = SequencingReadType.PAIRED

/**
 * the complete path to the config file.
 * The file should be located in: $OtpProperty#PATH_PROJECT_ROOT/$PROJECT/configFiles/$Workflow/
 * The file should be named as: ${Pipeline}_${seqType.roddyName}_${seqType.libraryLayout}_${WorkflowVersion}_v${fileVersion}.xml
 *
 * for example: $OtpProperty#PATH_PROJECT_ROOT/$PROJECT/configFiles/PANCAN_ALIGNMENT/PANCAN_ALIGNMENT_WES_1.1.39_v1_0.xml
 */
String configFilePath = ''

boolean adapterTrimmingNeeded = false

//-----------------------
RoddyWorkflowConfigService roddyWorkflowConfigService = ctx.roddyWorkflowConfigService

LogThreadLocal.withThreadLog(System.out, { Project.withTransaction {
    assert projectName
    assert programVersionToUse
    assert configFilePath
    assert seqTypeRoddyName
    assert libraryLayout

    assert configFilePath.endsWith('xml')
    assert new File(configFilePath).exists()

    SeqType seqType = CollectionUtils.exactlyOneElement(SeqType.findAllByRoddyNameAndLibraryLayout(seqTypeRoddyName, libraryLayout))

    Pipeline pipeline = Pipeline.Name.forSeqType(seqType).pipeline

    String individualPidPath = individualPid ? "${individualPid}/" : ''
    assert configFilePath ==~ ".*/configFiles/${pipeline.name.name()}/${individualPidPath}${pipeline.name.name()}_${seqType.roddyName}_${seqType.libraryLayout}_\\d+.\\d+.\\d+_v\\d+_\\d+.xml"

    Project project = CollectionUtils.exactlyOneElement(Project.findAllByName(projectName))

    if (!individualPid) {
        roddyWorkflowConfigService.importProjectConfigFile(
                project,
                seqType,
                programVersionToUse,
                pipeline,
                configFilePath,
                configVersion,
                adapterTrimmingNeeded,
        )
    } else {
        Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(individualPid))
        roddyWorkflowConfigService.loadPanCanConfigAndTriggerAlignment(
                project,
                seqType,
                programVersionToUse,
                pipeline,
                configFilePath,
                configVersion,
                adapterTrimmingNeeded,
                individual,
        )
    }

    project.alignmentDeciderBeanName = AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT
    project.save(flush: true)

    println "Config file loaded."
    println "Don't forget to also configure the reference genome (ConfigureReferenceGenome.groovy)."
}})
