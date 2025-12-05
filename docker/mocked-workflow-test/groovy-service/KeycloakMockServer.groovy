/*
 * Copyright 2011-2026 The OTP authors
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

import com.sun.net.httpserver.*
import groovy.json.JsonOutput

int port = 8100
HttpServer server = HttpServer.create(new InetSocketAddress(port), 0)

List<String> paths = [
        "/realms/test/protocol/openid-connect/token",
]

HttpHandler tokenHandler = new HttpHandler() {
    void handle(HttpExchange exchange) {
        if (exchange.getRequestMethod() == 'POST') {
            def dummyToken = [
                    access_token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy.payload.signature",
                    token_type  : "bearer",
                    expires_in  : 3600
            ]
            def response = JsonOutput.toJson(dummyToken)
            exchange.getResponseHeaders().add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.bytes.length)
            exchange.getResponseBody().write(response.bytes)
            exchange.getResponseBody().close()
        } else {
            exchange.sendResponseHeaders(404, 0)
            exchange.getResponseBody().close()
        }
    }
}

paths.each {
    server.createContext(it, tokenHandler)
}

server.start()
paths.each {
    println "MockKeycloakServer running on http://localhost:${port}${it} (POST)"
}
