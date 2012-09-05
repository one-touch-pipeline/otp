package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class ScriptFilesService {

    def configService

    public enum ScriptFileID {
        Alignment,
        AlignmentSummary,
        ChromosomeDiff,
        Fastqc,
        Fastx,
        GenomeCoverage,
        InsertSizes,
        MergeBamFiles,
        PairedEndReadAberrations,
        QualityDetermination,
        SampeSort,
        SamtoolsIndex,
        SamtoolsFlagstat
    }

    public static final HashMap<ScriptFileID, String> scriptFiles = [
        (ScriptFileID.Alignment) : "align_bwa_new.sh",
        (ScriptFileID.AlignmentSummary) : "writeQCsummary_newFlagstat.py",
        (ScriptFileID.ChromosomeDiff) : "postalignDiffChrom.sh",
        (ScriptFileID.Fastqc) : "fastqc_check.sh",
        (ScriptFileID.Fastx) : "fastx_qc_new.sh",
        (ScriptFileID.GenomeCoverage) : "genome_coverage.sh",
        (ScriptFileID.InsertSizes) : "postalignInsertSize.sh",
        (ScriptFileID.MergeBamFiles) : "merge_new.sh",
        (ScriptFileID.PairedEndReadAberrations) : "pairedEndReadAberrations.py",
        (ScriptFileID.QualityDetermination) : "PhredOrIllumina.pl",
        (ScriptFileID.SampeSort) : "postalignSampeSort.sh",
        (ScriptFileID.SamtoolsIndex) : "postalignSamtoolsIndex.sh",
        (ScriptFileID.SamtoolsFlagstat) : "postalignSamtoolsFlagstat.sh"
    ]

    String getScriptFile(ScriptFileID index, Project project) {
        String scriptDir = getScriptDir(index)
        if (scriptFiles.containsKey(index)) {
            String file = "${scriptDir}/" + scriptFiles[index]
            if ( !(new File(file)).exists() ) {
                throw new IOException("Script ${index} cannot be found at ${file}, scriptDir is ${scriptDir}")
            }
            return file
        }
        throw new IOException("Script ${index} is not specified")
    }

    String getScriptDir(ScriptFileID index, Project project) {
        Realm realm = configService.getRealm(project, Realm.OperationType.DATA_PROCESSING)
        return realm.programsRootPath + "/analysis_tools"
    }
}
