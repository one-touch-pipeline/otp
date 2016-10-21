class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(controller:"info", action: "about")
        /* TODO: OTP-2282: uncomment and delete line above
        "/"(controller:"root", action: "intro") //*/
        "403"(controller: "errors", action: "error403")
        "404"(controller: "errors", action: "error404")
        "500"(controller: "errors", action: "error500")
        "500"(controller: "errors", action: "error403", exception: org.springframework.security.access.AccessDeniedException)
    }
}
