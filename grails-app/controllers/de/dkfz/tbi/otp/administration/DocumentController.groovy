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
package de.dkfz.tbi.otp.administration

import grails.plugin.springsecurity.annotation.Secured
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage

@Secured("hasRole('ROLE_OPERATOR')")
class DocumentController {

    static allowedMethods = [
            manage            : "GET",
            upload            : "POST",
            updateSortOrder   : "POST",
            updateDescription : "POST",
            download          : "GET",
            delete            : "POST",
            createDocumentType: "POST",
    ]

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

    def updateSortOrder(UpdateSortOrderCommand cmd) {
        withForm {
            Errors errors = documentService.updateSortOrder(cmd.documentType, cmd.sortOrder)
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

    def updateDescription(UpdateDescriptionCommand cmd) {
        withForm {
            Errors errors = documentService.updateDescription(cmd.documentType, cmd.description)
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

    @Secured('permitAll')
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
                    fileName: (cmd.to == Action.DOWNLOAD) ? documentService.getDocumentFileNameWithExtension(document) : null
            )
        }
    }

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

class UpdateSortOrderCommand {
    DocumentType documentType
    int sortOrder
}

class UpdateDescriptionCommand {
    DocumentType documentType
    String description
}

class CreateTypeCommand {
    String title
    String description
}

class DeleteCommand {
    DocumentType documentType
}
