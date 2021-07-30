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
package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.project.Project

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * @Deprecated use the domain factories instead
 */
@Deprecated
class TestData {

    final static ARBITRARY_LENGTH_FOR_REFERENCE_GENOME = 100

    @Deprecated
    String referenceGenomePath
    @Deprecated
    File directory
    @Deprecated
    File file
    @Deprecated
    Realm realm
    @Deprecated
    Project project
    @Deprecated
    Individual individual
    @Deprecated
    SampleType sampleType
    @Deprecated
    Sample sample
    @Deprecated
    SeqType seqType
    @Deprecated
    SeqType exomeSeqType
    @Deprecated
    SeqCenter seqCenter
    @Deprecated
    SeqPlatform seqPlatform
    @Deprecated
    Run run
    @Deprecated
    FastqImportInstance fastqImportInstance
    @Deprecated
    SoftwareTool softwareTool
    @Deprecated
    SeqTrack seqTrack
    @Deprecated
    FileType fileType
    @Deprecated
    DataFile dataFile
    @Deprecated
    ReferenceGenome referenceGenome
    @Deprecated
    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType

    /**
     * @deprecated Use the <code>build()</code> method from the test data plugin or the static methods in this class or
     * in {@link DomainFactory}.
     */
    @Deprecated
    void createObjects() {
        File testDir = new File(TestCase.createEmptyTestDirectory(), "referenceGenome")

        referenceGenomePath = testDir.absolutePath

        directory = new File(referenceGenomePath)
        if (!directory.exists()) {
            assertTrue(directory.mkdirs())
        }

        file = new File("${referenceGenomePath}prefixName.fa")
        if (!file.exists()) {
            file.createNewFile()
            file << "test"
        }

        realm = DomainFactory.createRealm()

        project = createProject([
            name : "otp_test_project",
            dirName : "otp_test_project",
            realm : realm,
        ])
        assertNotNull(project.save(flush: true))

        individual = createIndividual()
        assertNotNull(individual.save(flush: true))

        sampleType = createSampleType()
        assertNotNull(sampleType.save(flush: true))

        sample = createSample()
        assertNotNull(sample.save(flush: true))

        seqType = DomainFactory.createWholeGenomeSeqType()
        exomeSeqType = DomainFactory.createExomeSeqType()

        seqCenter = DomainFactory.createSeqCenter()

        SeqPlatformGroup seqPlatformGroup = DomainFactory.createSeqPlatformGroup()
        assert seqPlatformGroup.save(flush: true)

        seqPlatform = DomainFactory.createSeqPlatform(name: 'seqPlatform')
        seqPlatform.addToSeqPlatformGroups(seqPlatformGroup)
        assert seqPlatform.save(flush: true)

        run = createRun("testname1")
        assertNotNull(run.save(flush: true))

        fastqImportInstance = DomainFactory.createFastqImportInstance()

        softwareTool = DomainFactory.createSoftwareTool()
        softwareTool.programName = "SOLID"
        softwareTool.programVersion = "0.4.8"
        softwareTool.type = SoftwareTool.Type.ALIGNMENT
        assertNotNull(softwareTool.save(flush: true))

        seqTrack = createSeqTrack()
        assertNotNull(seqTrack.save(flush: true))

        fileType = createFileType(FileType.Type.SEQUENCE)

        dataFile = createDataFile(seqTrack, fastqImportInstance)
        assertNotNull(dataFile.save(flush: true))

        referenceGenome = createReferenceGenome()
        assertNotNull(referenceGenome.save(flush: true))

        referenceGenomeProjectSeqType = createReferenceGenomeProjectSeqType()
        assertNotNull(referenceGenomeProjectSeqType.save(flush: true))
    }

    @Deprecated
    private Project createProject(Map properties = [:]) {
        return DomainFactory.createProject([
            name: "project",
            dirName: "dirName",
            qcThresholdHandling: QcThresholdHandling.NO_CHECK,
        ] + properties)
    }

    @Deprecated
    private Individual createIndividual(Map properties = [:]) {
        return DomainFactory.createIndividual([
            pid: "654321",
            mockPid: "PID",
            mockFullName: "PID",
            type: Individual.Type.REAL,
            project: project,
        ] + properties)
    }

    @Deprecated
    private SampleType createSampleType(Map properties = [:]) {
        return DomainFactory.createSampleType([
            name: "tumor",
            specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
        ] + properties)
    }

    @Deprecated
    private Sample createSample(Map properties = [:]) {
        return DomainFactory.createSample([
            individual: individual,
            sampleType: sampleType,
        ] + properties)
    }

    @Deprecated
    SeqTrack createSeqTrack(Map properties = [:]) {
        return DomainFactory.createSeqTrack([
            seqType: seqType,
            sample: sample,
            run: run,
            pipelineVersion: softwareTool,
        ] + properties)
    }

    @Deprecated
    DataFile createDataFile(SeqTrack seqTrack, FastqImportInstance fastqImportInstance, FileType fileType = this.fileType) {
        return createDataFile(
            seqTrack           : seqTrack,
            fastqImportInstance: fastqImportInstance,
            fileType           : fileType,
        )
    }

    @Deprecated
    DataFile createDataFile(Map properties = [:]) {
        return DomainFactory.createDataFile([
            fileName           : "datafile",
            initialDirectory   : TestCase.getUniqueNonExistentPath().path,
            fileExists         : true,
            fileSize           : 1,
            fileType           : fileType,
            seqTrack           : seqTrack,
            fastqImportInstance: fastqImportInstance,
            run                : run,
            fileWithdrawn      : false,
        ] + properties)
    }

    @Deprecated
    static ReferenceGenome createReferenceGenome(Map properties = [:]) {
        return DomainFactory.createReferenceGenome([
                name                        : "hg19_1_24",
                path                        : "referenceGenome",
                fileNamePrefix              : "prefixName",
                length                      : ARBITRARY_LENGTH_FOR_REFERENCE_GENOME,
                lengthWithoutN              : ARBITRARY_LENGTH_FOR_REFERENCE_GENOME,
                lengthRefChromosomes        : ARBITRARY_LENGTH_FOR_REFERENCE_GENOME,
                lengthRefChromosomesWithoutN: ARBITRARY_LENGTH_FOR_REFERENCE_GENOME,
                chromosomeSuffix            : "",
                chromosomePrefix            : "",
        ] + properties, false)
    }

    @Deprecated
    private ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqType(Map properties = [:]) {
        return DomainFactory.createReferenceGenomeProjectSeqType([
            project: project,
            seqType: seqType,
            referenceGenome: referenceGenome,
            statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
        ] + properties)
    }

    @Deprecated
    private Run createRun(String name) {
        return createRun(name: name)
    }

    @Deprecated
    private Run createRun(Map properties = [:]) {
        return DomainFactory.createRun([
            name: "TestRun",
            seqCenter: seqCenter,
            seqPlatform: seqPlatform,
        ] + properties)
    }

    @Deprecated
    SeqTrack createExomeSeqTrack(Run run) {
        return DomainFactory.createExomeSeqTrack(
            run: run,
            sample: sample,
            seqType: exomeSeqType,
            pipelineVersion: softwareTool,
            kitInfoReliability: InformationReliability.KNOWN,
            libraryPreparationKit: DomainFactory.createLibraryPreparationKit(),
        )
    }

    @Deprecated
    private FileType createFileType(FileType.Type type) {
        fileType = DomainFactory.createFileType(
                type: type
                )
        assertNotNull(fileType.save(flush: true))
        return fileType
    }
}
