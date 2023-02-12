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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.Realm

import java.nio.file.FileSystem
import java.nio.file.Path

class ScriptBuilder {
    ConfigService configService
    FileService fileService
    FileSystemService fileSystemService

    List<String> metaInformation = []
    List<String> groovyCommands = []
    List<String> bashCommands = []
    boolean containsChanges = false

    ScriptBuilder(ConfigService configService, FileService fileService, FileSystemService fileSystemService) {
        this.configService = configService
        this.fileService = fileService
        this.fileSystemService = fileSystemService
    }

    ScriptBuilder addMetaInfo(String info) {
        metaInformation << info
        return this
    }

    ScriptBuilder addGroovyCommand(String command) {
        groovyCommands << command
        return this
    }

    ScriptBuilder addGroovyCommandWithChanges(String command) {
        groovyCommands << command
        containsChanges = true
        return this
    }

    ScriptBuilder addBashCommand(String command) {
        bashCommands << command
        return this
    }

    String build(String filename) {
        if (!bashCommands.empty) {
            writeBashScriptToFileSystem(filename, buildBashScript())
        }
        return buildConsoleOutput()
    }

    private String buildMetaInformation() {
        return metaInformation.join("\n")
    }

    private String buildGroovyScript() {
        return encloseInGroovyDescription(groovyCommands.join("\n"))
    }

    private String buildConsoleOutput() {
        return encloseInMetaDescription(buildMetaInformation() + "\n" + buildGroovyScript())
    }

    String buildBashScript() {
        return encloseInBashScriptDescription(bashCommands.join("\n"))
    }

    private String encloseInGroovyDescription(String enclosedContent) {
        return """
               |/****************************************************************
               | * DATABASE FIXING
               | *
               | * OTP console script to move the database-side of things
               | ****************************************************************/
               |
               |$enclosedContent
               |
               |""".stripMargin()
    }

    private String encloseInMetaDescription(String enclosedContent) {
        return """|
               |/****************************************************************
               | * META DESCRIPTION
               | *
               | * What will change?
               | ****************************************************************/
               |
               |$enclosedContent
               |
               |""".stripMargin()
    }

    private String encloseInBashScriptDescription(String enclosedContent) {
        return """|
                  |/****************************************************************
                  | * FILESYSTEM FIXING
                  | *
                  | * meta-Bash script; calls all generated bash-scripts to fix
                  | * the filesystem-side of things.
                  | *
                  | * execute this after the database-side of things has been updated
                  | ****************************************************************/
                  |/*
                  |$enclosedContent
                  |*/
                  |""".stripMargin()
    }

    private void writeBashScriptToFileSystem(String filename, String content) {
        Realm realm = configService.getDefaultRealm() // codenarc-disable-line
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)
        Path outDir = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve('meta-swap')

        try {
            Path bashScriptPath = fileService.createOrOverwriteScriptOutputFile(outDir, filename, realm)
            bashScriptPath << content
        } catch (IOException e) {
            println "Error while writing bash script: ${e}" // codenarc-disable-line
        }
    }
}
