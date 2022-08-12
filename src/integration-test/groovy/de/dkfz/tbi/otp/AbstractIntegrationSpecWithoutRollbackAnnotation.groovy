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
package de.dkfz.tbi.otp

import grails.testing.mixin.integration.Integration
import groovy.sql.Sql
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.utils.CreateFileHelper

import javax.sql.DataSource
import java.nio.file.Path

@Integration
abstract class AbstractIntegrationSpecWithoutRollbackAnnotation extends Specification {

    DataSource dataSource

    @TempDir
    Path tempDir

    void cleanup() {
        boolean usePostgresDocker = "TRUE".equalsIgnoreCase(System.properties['usePostgresDocker'])
        Sql sql = new Sql(dataSource)
        if (usePostgresDocker) {
            String query = """
                SELECT
                    schemaname,
                    tablename
                FROM
                    pg_tables
                WHERE
                    schemaname = 'public';
                """
            List<String> tables = []

            sql.eachRow(query) {
                tables << [
                        it.getString(1),
                        it.getString(2),
                ].join('.')
            }
            String truncate = "TRUNCATE TABLE ${tables.join(', ')} CASCADE;"
            sql.execute(truncate)
        } else { //h2 database
            File schemaDump = CreateFileHelper.createFile(tempDir.resolve("test-database-dump.sql")).toFile()
            sql.execute("SCRIPT NODATA DROP TO ?", [schemaDump.absolutePath])
            sql.execute("DROP ALL OBJECTS")
            sql.execute("RUNSCRIPT FROM ?", [schemaDump.absolutePath])
        }
    }
}
