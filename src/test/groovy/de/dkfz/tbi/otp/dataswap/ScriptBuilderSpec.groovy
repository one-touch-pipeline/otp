/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataswap

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.Realm

import java.nio.file.*

class ScriptBuilderSpec extends Specification implements DataTest, RoddyPancanFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [Realm]
    }

    @TempDir
    Path tempDir

    ScriptBuilder builder = new ScriptBuilder(null, null, null, null)

    void "addMetaInfo, should add new list entry in metaInformation"() {
        when:
        metaInfos.forEach {
            builder.addMetaInfo(it)
        }

        then:
        builder.metaInformation == metaInfos

        where:
        metaInfos << [["some meta info"], ["meta info 1", "meta info 2", "meta info 3"]]
    }

    void "addBashCommand, should add new list entry in bashCommands"() {
        when:
        bashCommands.forEach {
            builder.addBashCommand(it)
        }

        then:
        builder.bashCommands == bashCommands

        where:
        bashCommands << [["some bash command"], ["bash command 1", "bash command 2", "bash command 3"]]
    }

    void "addGroovyCommand, should add new list entry in groovyCommands"() {
        when:
        groovyCommands.forEach {
            builder.addGroovyCommand(it)
        }

        then:
        builder.groovyCommands == groovyCommands
        !builder.containsChanges

        where:
        groovyCommands << [["some groovy command"], ["groovy command 1", "groovy command 2", "groovy command 3"]]
    }

    void "addGroovyCommandWithChanges, should add new list entry in groovyCommands and set changes flag true"() {
        when:
        groovyCommands.forEach {
            builder.addGroovyCommandWithChanges(it)
        }

        then:
        builder.groovyCommands == groovyCommands
        builder.containsChanges

        where:
        groovyCommands << [["some groovy command"], ["groovy command 1", "groovy command 2", "groovy command 3"]]
    }

    void "buildBashScript, should concat bash commands and header text to bash script"() {
        given:
        final String expectedBashScript = """|
                  |${bashCommands.join("\n")}
                  |""".stripMargin()

        when:
        bashCommands.forEach {
            builder.addBashCommand(it)
        }
        String result = builder.buildBashScript()

        then:
        result == expectedBashScript

        where:
        bashCommands << [["some bash command"], ["bash command 1", "bash command 2", "bash command 3"]]
    }

    void "build, should create meta and groovy output and create bash script on filesystem"() {
        given:
        final String filename = "some_file_name"
        final String expectedMetadataOutput = """|
               |/****************************************************************
               | * META DESCRIPTION
               | *
               | * What will change?
               | ****************************************************************/
               |
               |${metaInformations.join("\n")}""".stripMargin()
        final String expectedGroovyScript = """
               |/****************************************************************
               | * DATABASE FIXING
               | *
               | * OTP console script to move the database-side of things
               | ****************************************************************/
               |
               |${groovyCommands.join("\n")}
               |
               |
               |
               |""".stripMargin()
        final String expectedOutput = [expectedMetadataOutput, expectedGroovyScript].join("\n")

        ConfigService configService = Mock(ConfigService)
        FileService fileService = Mock(FileService)
        FileSystemService fileSystemService = Mock(FileSystemService)

        builder.configService = configService
        builder.fileService = fileService
        builder.fileSystemService = fileSystemService
        builder.relativeOutputDir = Paths.get("subDir")

        when:
        metaInformations.forEach {
            builder.addMetaInfo(it)
        }
        groovyCommands.forEach {
            builder.addGroovyCommand(it)
        }
        bashCommands.forEach {
            builder.addBashCommand(it)
        }
        String result = builder.build(filename)

        then:
        result == expectedOutput

        and:
        1 * configService.defaultRealm
        1 * fileSystemService.getRemoteFileSystem(_) >> FileSystems.default
        1 * fileService.toPath(_, _) >> Paths.get("/")
        1 * configService.scriptOutputPath
        1 * fileService.createOrOverwriteScriptOutputFile(_, _, _) >> tempDir.resolve("testFile")
        0 * _

        where:
        metaInformations   | groovyCommands          | bashCommands
        ["some meta info"] | ["some groovy command"] | ["some bash command"]
        ["meta info 1",
         "meta info 2",
         "meta info 3"]    | ["groovy command 1",
                              "groovy command 2",
                              "groovy command 3"]    | ["bash command 1",
                                                        "bash command 2",
                                                        "bash command 3"]
    }
}
