package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.utils.CreateFileHelper
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

class ReferenceGenomeServiceTests {

    ReferenceGenomeService referenceGenomeService

    File directory
    File statFile

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Before
    void setUp() {
        temporaryFolder.create() //Called directly because bug in junit
    }

    private MergingWorkPackage createDataForChromosomeStatSizeFile()  {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.build (
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
                workflow: DomainFactory.createPanCanWorkflow(),
        )
        Realm realm = Realm.build([
                operationType: Realm.OperationType.DATA_PROCESSING,
                name: mergingWorkPackage.project.realmName,
                processingRootPath: temporaryFolder.newFolder().path
        ])

        directory = new File(
                new File(
                        new File(
                                realm.processingRootPath, 'reference_genomes'),
                        mergingWorkPackage.referenceGenome.path),
                ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
        statFile = new File(directory, DomainFactory.DEFAULT_TAB_FILE_NAME)

        return mergingWorkPackage
    }


    @Test
    void testChromosomeStatSizeFile_AllFine() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeStatSizeFile()
        File pathExp = statFile
        File pathAct = referenceGenomeService.chromosomeStatSizeFile(mergingWorkPackage, false)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testChromosomeStatSizeFile_WithFileCheck_AllFine() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeStatSizeFile()
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
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeStatSizeFile()
        mergingWorkPackage.statSizeFileName = null
        mergingWorkPackage.workflow = DomainFactory.createDefaultOtpWorkflow()
        mergingWorkPackage.save(flush: true)
        assert TestCase.shouldFail(AssertionError) {
            referenceGenomeService.chromosomeStatSizeFile(mergingWorkPackage, false)
        }.contains('No stat file size name is defined')
    }

    @Test
    void testChromosomeStatSizeFile_StateSizeFileDoesNotExistAndExistenceIsChecked_ShouldFail() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeStatSizeFile()
        assert directory.mkdirs()
        assert TestCase.shouldFail(RuntimeException) {
            referenceGenomeService.chromosomeStatSizeFile(mergingWorkPackage, true)
        }.contains(DomainFactory.DEFAULT_TAB_FILE_NAME)
    }

}
