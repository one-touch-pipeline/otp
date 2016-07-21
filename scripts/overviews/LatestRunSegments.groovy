import de.dkfz.tbi.otp.ngsdata.*
import org.codehaus.groovy.grails.plugins.web.taglib.*

RunSegment.list(sort: 'id', order: 'desc', max: 20).each {
    println ctx.getBean(ApplicationTagLib).createLink(
            controller: 'metadataImport', action: 'details', id: it.id, absolute: 'true')
}
''
