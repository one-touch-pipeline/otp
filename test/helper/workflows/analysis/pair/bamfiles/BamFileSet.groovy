package workflows.analysis.pair.bamfiles

@SuppressWarnings('JavaIoPackageAccess')
class BamFileSet {

    final File diseaseBamFile
    final File diseaseBaiFile
    final File controlBamFile
    final File controlBaiFile

    BamFileSet(File bamFileDirectory,
               String diseaseBamFileName,
               String diseaseBaiFileName,
               String controlBamFileName,
               String controlBaiFileName
    ) {
        diseaseBamFile = new File(bamFileDirectory, diseaseBamFileName)
        diseaseBaiFile = new File(bamFileDirectory, diseaseBaiFileName)
        controlBamFile = new File(bamFileDirectory, controlBamFileName)
        controlBaiFile = new File(bamFileDirectory, controlBaiFileName)
    }
}
