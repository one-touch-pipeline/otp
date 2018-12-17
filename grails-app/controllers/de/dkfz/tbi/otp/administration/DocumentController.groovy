package de.dkfz.tbi.otp.administration

import grails.compiler.GrailsCompileStatic
import groovy.transform.CompileDynamic
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage

@GrailsCompileStatic
class DocumentController {
    static allowedMethods = [manage: "GET", upload: "POST", download: "GET", delete: 'POST', createDocumentType: 'POST']

    DocumentService documentService

    def manage() {
        List<Document> availableDocuments = documentService.listDocuments()
        List<DocumentType> availableDocumentTypes = documentService.listDocumentTypes().sort {
            it.title
        }
        Map<DocumentType, Document> documents = availableDocumentTypes.collectEntries { DocumentType documentType ->
            [(documentType): availableDocuments.find { it -> documentType == it.documentType }]
        }
        return [
                documents: documents,
        ]
    }

    @CompileDynamic
    def upload(UploadCommand cmd) {
        withForm {
            Errors errors = documentService.updateDocument(cmd.documentType, cmd.content, cmd.formatType)
            if (errors) {
                flash.message = new FlashMessage(g.message(code: "document.store.fail") as String, errors)
            } else {
                flash.message = new FlashMessage(g.message(code: "document.store.succ") as String)
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "document.store.fail") as String)
        }
        redirect(action: "manage")
    }

    def download(DownloadCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }
        Document document = cmd.document
        if (document) {
            render(
                    file: document.content,
                    contentType: document.formatType.mimeType,
                    fileName: (cmd.to == Action.DOWNLOAD) ? document.getFileNameWithExtension() : null
            )
        }
    }

    @CompileDynamic
    def createDocumentType(CreateTypeCommand cmd) {
        withForm {
            Errors errors = documentService.createDocumentType(cmd.title, cmd.description)
            if (errors) {
                flash.message = new FlashMessage(g.message(code: "document.store.fail") as String, errors)
            } else {
                flash.message = new FlashMessage(g.message(code: "document.store.succ") as String)
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "document.store.fail") as String)
        }
        redirect(action: "manage")
    }

    @CompileDynamic
    def delete(DeleteCommand cmd) {
        withForm {
            Errors errors = documentService.deleteDocumentType(cmd.documentType)
            if (errors) {
                flash.message = new FlashMessage(g.message(code: "document.store.fail") as String, errors)
            } else {
                flash.message = new FlashMessage(g.message(code: "document.store.succ") as String)
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "document.store.fail") as String)
        }
        redirect(action: "manage")
    }

    enum Action {
        VIEW,
        DOWNLOAD,
    }
}

class DownloadCommand {
    Document document
    DocumentController.Action to = DocumentController.Action.VIEW
}

class UploadCommand {
    DocumentType documentType
    Document.FormatType formatType
    byte[] content
}

class CreateTypeCommand {
    String title
    String description
}

class DeleteCommand {
    DocumentType documentType
}
