package example.grails

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?" {
        }

        "/"(controller: 'jobDemo', action: 'index')
        "500"(view: '/error')
        "404"(view: '/notFound')
    }
}
