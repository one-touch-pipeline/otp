package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.utils.CreateFileHelper
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

class ReferenceGenomeServiceTests {

    ReferenceGenomeService referenceGenomeService

    File directory
    File statFile
    File chromosomeLengthFile

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    private MergingWorkPackage createDataForChromosomeSizeInformationFiles()  {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
                referenceGenome: DomainFactory.createReferenceGenome(chromosomeLengthFilePath: DomainFactory.DEFAULT_CHROMOSOME_LENGTH_FILE_NAME),
                pipeline: DomainFactory.createPanCanPipeline(),
        ])

        Realm realm = DomainFactory.createRealmDataProcessing([
                name: mergingWorkPackage.project.realmName,
                processingRootPath: temporaryFolder.newFolder().path
        ])

        File referenceGenomeDirectory = new File(realm.processingRootPath, 'reference_genomes')
        DomainFactory.createProcessingOptionBasePathReferenceGenome(referenceGenomeDirectory.path)
        directory = new File(
                new File(referenceGenomeDirectory,
                        mergingWorkPackage.referenceGenome.path),
                ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
        statFile = new File(directory, DomainFactory.DEFAULT_TAB_FILE_NAME)
        chromosomeLengthFile = new File(directory, DomainFactory.DEFAULT_CHROMOSOME_LENGTH_FILE_NAME)

        return mergingWorkPackage
    }


    @Test
    void testChromosomeStatSizeFile_AllFine() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        File pathExp = statFile
        File pathAct = referenceGenomeService.chromosomeStatSizeFile(mergingWorkPackage, false)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testChromosomeStatSizeFile_WithFileCheck_AllFine() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        File pathExp = statFile
        CreateFileHelper.createFile(pathExp)
        File pathAct = referenceGenomeService.chromosomeStatSizeFile(mergingWorkPackage, true)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testChromosomeStatSizeFile_MergingWorkPackageIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            referenceGenomeService.chromosomeStatSizeFile(null)
        }.contains('mergingWorkPackage')
    }

    @Test
    void testChromosomeStatSizeFile_NoStatSizeFileIsDefined_ShouldFail() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        mergingWorkPackage.pipeline = DomainFactory.createDefaultOtpPipeline()
        mergingWorkPackage.statSizeFileName = null
        mergingWorkPackage.save(flush: true)
        assert TestCase.shouldFail(AssertionError) {
            referenceGenomeService.chromosomeStatSizeFile(mergingWorkPackage, false)
        }.contains('No stat file size name is defined')
    }

    @Test
    void testChromosomeStatSizeFile_StateSizeFileDoesNotExistAndExistenceIsChecked_ShouldFail() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        assert directory.mkdirs()
        assert TestCase.shouldFail(RuntimeException) {
            referenceGenomeService.chromosomeStatSizeFile(mergingWorkPackage, true)
        }.contains(DomainFactory.DEFAULT_TAB_FILE_NAME)
    }

    @Test
    void testChromosomeLengthFile_AllFine() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        File pathExp = chromosomeLengthFile
        File pathAct = referenceGenomeService.chromosomeLengthFile(mergingWorkPackage, false)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testChromosomeLengthFile_WithFileCheck_AllFine() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        File pathExp = chromosomeLengthFile
        CreateFileHelper.createFile(pathExp)
        File pathAct = referenceGenomeService.chromosomeLengthFile(mergingWorkPackage, true)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testChromosomeLengthFile_MergingWorkPackageIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            referenceGenomeService.chromosomeLengthFile(null)
        }.contains('mergingWorkPackage')
    }

    @Test
    void testChromosomeLengthFile_ChromosomeLengthFileFileDoesNotExistAndExistenceIsChecked_ShouldFail() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        assert directory.mkdirs()
        assert TestCase.shouldFail(RuntimeException) {
            referenceGenomeService.chromosomeLengthFile(mergingWorkPackage, true)
        }.contains(DomainFactory.DEFAULT_CHROMOSOME_LENGTH_FILE_NAME)
    }
}
