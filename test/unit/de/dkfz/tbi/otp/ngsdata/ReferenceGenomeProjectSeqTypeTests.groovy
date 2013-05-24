package de.dkfz.tbi.otp.ngsdata

import org.junit.Assert;

/*
 * the test is split into 2 tests because of a bug in the
 * current version of grails: queries still return an object even if
 * there is no objects in the db matching the query-condition
 */
@TestFor(ReferenceGenomeProjectSeqType)
@Mock([ReferenceGenomeProjectSeqType, ReferenceGenome, Project, SeqType])
class ReferenceGenomeProjectSeqTypeTests {

    void testUniqueHasDuplication() {
        // create the related objects
        Project project = new Project(
            name: "project",
            dirName: "dirName",
            realmName: "DKFZ")
        project.save(flush: true)
        SeqType seqType = new SeqType(
            name: "wgs",
            libraryLayout: "paired",
            dirName: "dirName")
        seqType.save(flush: true)
        ReferenceGenome refGenome = new ReferenceGenome(
            name: "refGen",
            filePath: "filePath")
        refGenome.save(flush: true)

        // create not deprecated domain
        ReferenceGenomeProjectSeqType currentDomain = new ReferenceGenomeProjectSeqType(
            project: project,
            seqType: seqType,
            referenceGenome: refGenome)
        currentDomain.save(flush: true)
        // create deprecated domain
        ReferenceGenomeProjectSeqType deprecatedDomain = new ReferenceGenomeProjectSeqType(
            project: project,
            seqType: seqType,
            referenceGenome: refGenome,
            deprecatedDate: new Date())
        deprecatedDomain.save(flush: true)
        // create new domain for the same project and seqType
        ReferenceGenomeProjectSeqType newDomain = new ReferenceGenomeProjectSeqType(
            project: project,
            seqType: seqType,
            referenceGenome: refGenome)
        Assert.assertTrue !newDomain.validate()
    }

    void testUniqueNoDuplications() {
        // create the related objects
        Project project = new Project(
            name: "project",
            dirName: "dirName",
            realmName: "DKFZ")
        project.save(flush: true)
        SeqType seqType = new SeqType(
            name: "wgs",
            libraryLayout: "paired",
            dirName: "dirName")
        seqType.save(flush: true)
        ReferenceGenome refGenome = new ReferenceGenome(
            name: "refGen",
            filePath: "filePath")
        refGenome.save(flush: true)

        // create deprecated domain
        ReferenceGenomeProjectSeqType deprDomain1 = new ReferenceGenomeProjectSeqType(
            project: project,
            seqType: seqType,
            referenceGenome: refGenome,
            deprecatedDate: new Date())
        deprDomain1.save(flush: true)
        // create second deprecated domain
        ReferenceGenomeProjectSeqType deprDomain2 = new ReferenceGenomeProjectSeqType(
            project: project,
            seqType: seqType,
            referenceGenome: refGenome,
            deprecatedDate: new Date())
        deprDomain2.save(flush: true)
        // create new domain for the same project and seqType
        ReferenceGenomeProjectSeqType newDomain = new ReferenceGenomeProjectSeqType(
            project: project,
            seqType: seqType,
            referenceGenome: refGenome)
        Assert.assertTrue newDomain.validate()
    }
}
