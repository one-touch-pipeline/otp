package de.dkfz.tbi.otp.dataprocessing.snvcalling

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType

class SnvConfigTests {

    @Test
    void testGetLatest() {
        Project project = new Project(
                name: "projectName",
                dirName: "projectDirName",
                realmName: "realmName"
                )
        assert project.save()

        Project project2 = new Project(
                name: "project2Name",
                dirName: "project2DirName",
                realmName: "realmName"
                )
        assert project2.save()

        SeqType seqType = new SeqType(
                name: "seqTypeName",
                libraryLayout: "seqTypeLibraryLayout",
                dirName: "seqTypeDirName"
                )
        assert seqType.save()

        SeqType seqType2 = new SeqType(
                name: "seqType2Name",
                libraryLayout: "seqType2LibraryLayout",
                dirName: "seqType2DirName"
                )
        assert seqType2.save()

        SnvConfig config = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: "testConfig",
                )
        assert config.save()

        assertEquals(config, SnvConfig.getLatest(project, seqType))

        SnvConfig config2 = new SnvConfig(
                project: project2,
                seqType: seqType2,
                configuration: "testConfig",
                )
        assert config2.save()
        assertEquals(config, SnvConfig.getLatest(project, seqType))
        assertEquals(config2, SnvConfig.getLatest(project2, seqType2))

        config.obsoleteDate = new Date()
        assert config.save()

        config2.project = project
        config2.seqType = seqType
        config2.previousConfig = config
        assert config2.save()

        assertEquals(config2, SnvConfig.getLatest(project, seqType))
    }
}
