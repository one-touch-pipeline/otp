package de.dkfz.tbi.otp.administration

import grails.compiler.*
import groovy.transform.*
import org.springframework.validation.*

@GrailsCompileStatic
class DocumentController {
    static allowedMethods = [manage: "GET", upload: "POST", download: "GET"]

    DocumentService documentService

    def manage() {
        List<Document> availableDocuments = documentService.listDocuments()
        Map<Document.Name, Document> documents = Document.Name.values().collectEntries { Document.Name name ->
            [(name): availableDocuments.find { it -> name == it.name}]
        }
        [
                documents: documents,
        ]
    }


    @CompileDynamic
    def upload(UploadCommand cmd) {
        withForm {
            Errors errors = documentService.updateDocument(cmd.name, cmd.content, cmd.type)
            if (errors) {
                flash.message = g.message(code: "document.store.fail")
                flash.errors = errors
            } else {
                flash.message = g.message(code: "document.store.succ")
            }
        }.invalidToken {
            flash.message = g.message(code: "document.store.fail")
       }
        redirect(action: "manage")
    }


    def download(DownloadCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }
        Document document = documentService.getDocument(cmd.file)
        if (document) {
            String fileName = (cmd.to == Action.DOWNLOAD) ?
                "${document.name.name().toLowerCase()}.${document.type.extension}" :
                null
            render(file: document.content, contentType: document.type.mimeType, fileName: fileName)
        } else {
            render status: 404
        }
    }

    enum Action {
        VIEW,
        DOWNLOAD,
    }
}


class DownloadCommand {
    Document.Name file
    DocumentController.Action to = DocumentController.Action.VIEW
}


class UploadCommand {
    Document.Name name
    Document.Type type
    byte[] content
}
