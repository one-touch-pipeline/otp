import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.cellRanger.GroupedMwp
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

/*
 * Copyright 2011-2020 The OTP authors
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


/*
Input Area
*/
def projectNames = [

]


/*
Script area
 */

File createFile(File file, String folderName) {
    File f = new File(file, folderName)
    f.delete()
    f.createNewFile()
    assert f.exists()
    assert f.size() == 0
    return f
}



def alignmentQualityOverview(Project p, File file, SeqType seqType) {
    def header = []
    header << "PROJECT\tOTP_PID\tOTP_SAMPLE_TYPE\tSEQUENCING_TYPE\tBAM_FILE\tCOVERAGE"
    boolean first = true

    List<AbstractQualityAssessment> dataOverall = ctx.overallQualityAssessmentMergedService.findAllByProjectAndSeqType(p, seqType)
    dataOverall.each { AbstractQualityAssessment aqa ->
        if (first) {
            header << aqa.properties.sort()*.key*.toString()*.toUpperCase().join("\t")
            file << "${header.flatten().join('\t')}\n"
            first = false
        }
        def outString = [p.name]
        QualityAssessmentMergedPass qualityAssessmentMergedPass = aqa.qualityAssessmentMergedPass
        AbstractMergedBamFile bam = qualityAssessmentMergedPass.abstractMergedBamFile
        outString << bam.individual.pid
        outString << bam.sampleType.name
        outString << bam.seqType.nameWithLibraryLayout
        outString << bam.getPathForFurtherProcessingNoCheck()
        outString << bam.coverage
        aqa.properties.sort().each {
            outString << it.value
        }
        file << "${outString.join('\t')}\n"
    }
}

def wgsAlignmentQualityOverview(Project p, File file) {
    File f = createFile(file, "${p.name}_WGS_alignmentQualityOverview.tsv")
    alignmentQualityOverview(p, f, ctx.seqTypeService.getWholeGenomePairedSeqType())
}

def wesAlignmentQualityOverview(Project p, File file) {
    File f = createFile(file, "${p.name}_WES_alignmentQualityOverview.tsv")
    alignmentQualityOverview(p, f, ctx.seqTypeService.getExomePairedSeqType())
}

def wgbsAlignmentQualityOverview(Project p, File file) {
    File f = createFile(file, "${p.name}_WGBS_alignmentQualityOverview.tsv")
    alignmentQualityOverview(p, f, ctx.seqTypeService.getWholeGenomeBisulfitePairedSeqType())
}

def wgbsTagmentationAlignmentQualityOverview(Project p, File file) {
    File f = createFile(file, "${p.name}_WGBS_tagmentation_alignmentQualityOverview.tsv")
    alignmentQualityOverview(p, f, ctx.seqTypeService.getWholeGenomeBisulfiteTagmentationPairedSeqType())
}

def rnaPairedAlignmentQualityOverview(Project p, File file) {
    File f = createFile(file, "${p.name}_RNA_PAIRED_alignmentQualityOverview.tsv")
    alignmentQualityOverview(p, f, ctx.seqTypeService.getRnaPairedSeqType())
}

def rnaSingleAlignmentQualityOverview(Project p, File file) {
    File f = createFile(file, "${p.name}_RNA_SINGLE_alignmentQualityOverview.tsv")
    alignmentQualityOverview(p, f, ctx.seqTypeService.getRnaSingleSeqType())
}


def cellRangerFinalSelection(Project p, File file) {

    File f = createFile(file, "${p.name}_cellRangerFinalSelection.tsv")
    f << "PROJECT\tOTP_PID\tOTP_SAMPLE_TYPE\tSEQUENCING_TYPE\tCELL_RANGER_VERSION\tREFERENCE\tRUNS\n"

    List<CellRangerMergingWorkPackage> mwps = CellRangerMergingWorkPackage.createCriteria().list {
        sample {
            individual {
                eq("project", p)
            }
        }
    } as List<CellRangerMergingWorkPackage>

    Map<GroupedMwp, List<CellRangerMergingWorkPackage>> grouped = mwps.groupBy({
        new GroupedMwp(it.sample, it.seqType, it.config.programVersion, it.referenceGenomeIndex)
    })

    List<GroupedMwp> groupedMwps = grouped.collect { k, v ->
        k.mwps = v
        return k
    }
    groupedMwps.each { mwpGroup ->
        def cells = mwpGroup.mwps.collect { mwp ->
            def exp = mwp.expectedCells ? "${mwp.expectedCells} expected cells" : "no expected cells"
            def enf = mwp.enforcedCells ? "${mwp.enforcedCells} enforced cells" : "no enforced cells"
            return "${exp} - ${enf}"
        }.join(", ")

        def config = CollectionUtils.exactlyOneElement(mwpGroup.mwps*.config.unique())

        f << "${mwpGroup.sample.individual.project.name}\t${mwpGroup.sample.individual.pid}\t${mwpGroup.sample.sampleType.name}\t${mwpGroup.seqType}\t${config.programVersion}\t${mwpGroup.reference}\t${cells}\n"
    }
}


def snvResults(Project p, File file) {

    File f = createFile(file, "${p.name}_snvResults.tsv")
    f << "PROJECT\tOTP_PID\tOTP_SAMPLE_TYPE\tSEQUENCING_TYPE\tLIBRARY_PREPARATION_KIT(S)\tCREATED_WITH_VERSION\tPROCESSING_DATE\tPROGRESS\n"

    ctx.snvResultsService.getCallingInstancesForProject(p.name).each { snvResult ->
        f << "${p.name}\t${snvResult.individualPid}\t${snvResult.sampleTypes}\t${snvResult.seqType}\t${snvResult.libPrepKits}\t${snvResult.version}\t${snvResult.dateCreated}\t${snvResult.processingState}\n"
    }
}


def indelResults(Project p, File file) {

    File f = createFile(file, "${p.name}_indelResults.tsv")
    f << "PROJECT\tOTP_PID\tOTP_SAMPLE_TYPE\tSEQUENCING_TYPE\tLIBRARY_PREPARATION_KIT(S)\t#INDELS\t#INSERTION\t#DELETIONS\t#SIZE_1_3\t#SIZE_4_10\t" +
            "ALL_SOMATIC_VARIANTS (T)\tALL_SOMATIC_VARIANTS (C)\tSOMATIC_COMMON_IN_gnomAD (T)\tSOMATIC_COMMON_IN_gnomAD (C)\tSOMATIC_PASS (T)\t" +
            "SOMATIC_PASS (C)\t#GERMLINE_VARIANTS\tSOMATIC_RESCUE\tCONTROL_VAF (MEDIAN)\tCREATED_WITH_VERSION\tPROCESSING_DATE\tPROGRESS\n"

    ctx.indelResultsService.getCallingInstancesForProject(p.name).each { indelResults ->
        f << "${p.name}\t${indelResults.individualPid}\t${indelResults.sampleTypes}\t${indelResults.seqType}\t${indelResults.libPrepKits}\t" +
                "${indelResults.numIndels}\t${indelResults.numIns}\t${indelResults.numDels}\t${indelResults.numSize1_3}\t${indelResults.numSize4_10}\t" +
                "${indelResults.somaticSmallVarsInTumor}\t${indelResults.somaticSmallVarsInControl}\t${indelResults.somaticSmallVarsInTumorCommonInGnomad}\t" +
                "${indelResults.somaticSmallVarsInControlCommonInGnomad}\t${indelResults.somaticSmallVarsInTumorPass}\t${indelResults.somaticSmallVarsInControlPass}\t"+
                "${indelResults.germlineSmallVarsInBothRare}\t${indelResults.tindaSomaticAfterRescue}\t${indelResults.tindaSomaticAfterRescueMedianAlleleFreqInControl}\t" +
                "${indelResults.version}\t${indelResults.dateCreated}\t${indelResults.processingState}\n"
    }
}


def aceseqResults(Project p, File file) {

    File f = createFile(file, "${p.name}_aceseqResults.tsv")
    f << "PROJECT\tOTP_PID\tOTP_SAMPLE_TYPE\tSEQUENCING_TYPE\tTUMOR CELL CONTENT\tPLOIDY\tGENDER\tCREATED_WITH_VERSION\tPROCESSING_DATE\tPROGRESS\n"

    ctx.aceseqResultsService.getCallingInstancesForProject(p.name).each { aceseqResult ->
        f << "${p.name}\t${aceseqResult.individualPid}\t${aceseqResult.sampleTypes}\t${aceseqResult.seqType}\t${aceseqResult.tcc}\t${aceseqResult.ploidy}\t" +
                "${aceseqResult.gender}\t${aceseqResult.version}\t${aceseqResult.dateCreated}\t${aceseqResult.processingState}\n"
    }
}



def sophiaResults(Project p, File file) {

    File f = createFile(file, "${p.name}_sophiaResults.tsv")
    f << "PROJECT\tOTP_PID\tOTP_SAMPLE_TYPE\tSEQUENCING_TYPE}\tCONTROL_MASSIVE_INV_PREFILTERING_LEVEL\tTUMOR_MASSIVE_INV_PREFILTERING_LEVEL\t" +
            "RNA_CONTAMINATED_GENES_COUNT\tRNA_DECONTAMINATION_APPLIED\tCREATED_WITH_VERSION\tPROCESSING_DATE\tPROGRESS\n"

    ctx.sophiaResultsService.getCallingInstancesForProject(p.name).each {sophiaResult ->
        f << "${p.name}\t${sophiaResult.individualPid}\t${sophiaResult.sampleTypes}\t${sophiaResult.seqType}\t" +
                "${sophiaResult.controlMassiveInvPrefilteringLevel}\t${sophiaResult.tumorMassiveInvFilteringLevel}\t" +
                "${sophiaResult.rnaContaminatedGenesCount}\t${sophiaResult.rnaDecontaminationApplied}\t${sophiaResult.version}\t" +
                "${sophiaResult.dateCreated}\t${sophiaResult.processingState}\n"
    }
}



def runYapsaResults(Project p, File file) {

    File f = createFile(file, "${p.name}_runYapsaResults.tsv")
    f << "PROJECT\tOTP_PID\tOTP_SAMPLE_TYPE\tSEQUENCING_TYPE\tLIBRARY_PREPARATION_KIT(S)\tCREATED_WITH_VERSION\tPROCESSING_DATE\tPROGRESS\n"

    ctx.runYapsaResultsService.getCallingInstancesForProject(p.name).each { runYapsaResult ->
        f << "${p.name}\t${runYapsaResult.individualPid}\t${runYapsaResult.sampleTypes}\t${runYapsaResult.seqType}\t${runYapsaResult.libPrepKits}\t" +
                "${runYapsaResult.version}\t${runYapsaResult.dateCreated}\t${runYapsaResult.processingState}\n"
    }
}

File outputFolder = new File(ctx.configService.getScriptOutputPath(), "export/UNITE/output")
assert outputFolder.exists() : "the output folder ${outputFolder} does not exist"

File f = createFile(outputFolder, "status.tsv")

f << new Date()

projectNames.each { projectName ->

    File outputFolderPerProject = new File(outputFolder, projectName)
    outputFolderPerProject.mkdir()
    assert outputFolderPerProject.exists() : "the output folder ${outputFolderPerProject} does not exist"


    Project project = Project.findByName(projectName)
    assert project : "There is not project with the name ${projectName}"

    f << "\n${projectName}\n"

    f << "cell ranger selection started\n"
    cellRangerFinalSelection(project, outputFolderPerProject)
    f << "cell ranger selection done\n"

    f << "snv results started\n"
    snvResults(project, outputFolderPerProject)
    f << "snv results done\n"

    f << "indel results started\n"
    indelResults(project, outputFolderPerProject)
    f << "indel results done\n"

    f << "aceseq results started\n"
    aceseqResults(project, outputFolderPerProject)
    f << "aceseq results done\n"

    f << "sophia results started\n"
    sophiaResults(project, outputFolderPerProject)
    f << "sophia results done\n"

    f << "runYAPSA results started\n"
    runYapsaResults(project, outputFolderPerProject)
    f << "runYAPSA results done\n"

    f << "wgsAlignmentQualityOverview started\n"
    wgsAlignmentQualityOverview(project, outputFolderPerProject)
    f << "wgsAlignmentQualityOverview done\n"

    f << "wesAlignmentQualityOverview started\n"
    wesAlignmentQualityOverview(project, outputFolderPerProject)
    f << "wesAlignmentQualityOverview done\n"

    f << "wgbsAlignmentQualityOverview started\n"
    wgbsAlignmentQualityOverview(project, outputFolderPerProject)
    f << "wgbsAlignmentQualityOverview done\n"

    f << "rnaPairedAlignmentQualityOverview started\n"
    rnaPairedAlignmentQualityOverview(project, outputFolderPerProject)
    f << "rnaPairedAlignmentQualityOverview done\n"

    f << "rnaSingleAlignmentQualityOverview started\n"
    rnaSingleAlignmentQualityOverview(project, outputFolderPerProject)
    f << "rnaSingleAlignmentQualityOverview done\n"

    f << "wgbsTagmentationAlignmentQualityOverview started\n"
    wgbsTagmentationAlignmentQualityOverview(project, outputFolderPerProject)
    f << "wgbsTagmentationAlignmentQualityOverview done\n"


}

f << new Date()
f << "\neverything executed"

println ""