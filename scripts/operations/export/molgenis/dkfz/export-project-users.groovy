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


import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.Entity
import java.text.SimpleDateFormat
import java.nio.file.*;

class ExportHelper {
    private Closure<String> header
    private Closure<String> body

    static Map<Class, ExportHelper> exportHelperPerEntity = [
            (User)           : new ExportHelper(
                    { return ["id", "username", "realName", "email", "enabled"] },
                    { User user -> return ["id", "username", "realName", "email", "enabled"].collect { user."$it" } },
            ),
            (Project)        : new ExportHelper(
                    { return ["id", "name", "individualPrefix", "dirName", "unixGroup", "costCenter", "organizationalUnit", "description"] },
                    { Project project -> return ["id", "name", "individualPrefix", "dirName", "unixGroup", "costCenter", "organizationalUnit", "description"].collect { project."$it" } },
            ),
            (UserProjectRole): new ExportHelper(
                    { return ["id", "project id", "project name", "user id", "user name", "role id", "role name"] },
                    { UserProjectRole upr ->
                        return [
                                upr.id,
                                upr.project.id, upr.project.name,
                                upr.user.id, upr.user.username,
                                upr.projectRole.id, upr.projectRole.name,
                        ]
                    },
            ),
    ]

    ExportHelper(Closure<String> header, Closure<String> body) {
        this.header = header
        this.body = body
    }

    static String csvJoin(List<String> columns) {
        return columns.collect { "\"${(it as String)?.replaceAll("\"", "\"\"")}\"" }.join(MolgenisGlobal.SEPARATOR_COLUMN)
    }

    String buildHeader() {
        return csvJoin(this.header())
    }

    String buildBody(Entity entity) {
        return csvJoin(this.body(entity))
    }

    static List<String> getFullyExportedEntity(Class clazz) {
        ExportHelper exportHelper = exportHelperPerEntity[clazz]
        return [exportHelper.buildHeader()] + clazz.list().collect { Entity entity -> return exportHelper.buildBody(entity) }
    }
}

class MolgenisGlobal {
    static String SEPARATOR_COLUMN = ","
    static String SEPARATOR_LINE = "\n"
}


String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())
final Path outputDirectory = ctx.configService.getScriptOutputPath().toPath().resolve("export").resolve("molgenis").resolve("${timestamp}-projects-and-users")
ctx.fileService.createDirectoryRecursively(outputDirectory)
ctx.fileService.setPermission(outputDirectory, ctx.fileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)

println "Writing to: ${outputDirectory}"

[
        [User, "users"],
        [Project, "projects"],
        [UserProjectRole, "project-users"],
].each { List it ->
    String output = ExportHelper.getFullyExportedEntity(it[0]).join(MolgenisGlobal.SEPARATOR_LINE)
    Path path = outputDirectory.resolve("${it[1]}.csv")
    println "    - ${path}"
    Files.write(path, output.getBytes())
}

''
