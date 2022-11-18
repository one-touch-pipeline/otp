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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty

Properties otpProperties = ConfigService.parsePropertiesFile()
Boolean consoleEnabled = Boolean.parseBoolean(otpProperties.getProperty(OtpProperty.GRAILS_CONSOLE.key))

spring {
    jmx {
        uniqueNames = true
    }
    devtools {
        restart {
            exclude = [
                    '**/*.gsp',
                    '**/*.less',
                    '**/*.css',
                    '**/*.js',
                    '**/*.json',
                    '**/*.sh',
            ]
        }
    }
}
management {
    endpoints {
        enabledByDefault = false
    }
}

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [
        html         : [
                'text/html',
                'application/xhtml+xml',
        ],
        xml          : [
                'text/xml',
                'application/xml',
        ],
        text         : 'text/plain',
        js           : 'text/javascript',
        rss          : 'application/rss+xml',
        atom         : 'application/atom+xml',
        css          : 'text/css',
        csv          : 'text/csv',
        all          : '*/*',
        json         : [
                'application/json',
                'text/json',
        ],
        form         : 'application/x-www-form-urlencoded',
        multipartForm: 'multipart/form-data'
]

// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside ${}
                scriptlet = 'html' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        // filteringCodecForContentType.'text/html' = 'html'
    }
}

grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

//max upload size
grails.controllers.upload.maxFileSize = 10*1024*1024
grails.controllers.upload.maxRequestSize = 10*1024*1024

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

//databasemigration configuration
grails.plugin.databasemigration.changelogLocation = 'migrations'
grails.plugin.databasemigration.changelogFileName = 'migration-wrapper.groovy'
grails.plugin.databasemigration.updateOnStart = true
grails.plugin.databasemigration.updateOnStartFileName = "migration-wrapper.groovy"
grails.plugin.databasemigration.excludeObjects = [
        'aggregate_sequences',
        'meta_data_key',
        'sequences',
]
environments {
    WORKFLOW_TEST {
        grails.plugin.databasemigration.updateOnStart = false
    }
    test {
        grails.plugin.databasemigration.updateOnStart = false
    }
}

// Restore old data-binding behaviour (before 2.3)
grails.databinding.convertEmptyStringsToNull = false
grails.databinding.autoGrowCollectionLimit = 65536

grails.databinding.trimStrings = true

//configure mail sending: disable mail sending for tests
environments {
    WORKFLOW_TEST {
        grails.mail.disabled=true
    }
    test {
        grails.mail.disabled=true
    }
}

//configure groovy web console
grails.plugin.console.enabled = consoleEnabled
environments {
    production {
        grails.plugin.console.baseUrl="/otp/console"
    }
    development {
        grails.plugin.console.baseUrl="/console"
    }
}
grails.plugin.console.fileStore.remote.defaultPath=System.getenv("CONSOLE_REMOTE_DEFAULTPATH")


System.setProperty("javax.net.ssl.trustStore", otpProperties.getProperty(OtpProperty.TRUSTSTORE_PATH.key) ?: OtpProperty.TRUSTSTORE_PATH.defaultValue)
System.setProperty("javax.net.ssl.trustStorePassword", otpProperties.getProperty(OtpProperty.TRUSTSTORE_PASSWORD.key) ?: OtpProperty.TRUSTSTORE_PASSWORD.defaultValue)
System.setProperty("javax.net.ssl.trustStoreType", otpProperties.getProperty(OtpProperty.TRUSTSTORE_TYPE.key) ?: OtpProperty.TRUSTSTORE_TYPE.defaultValue)
