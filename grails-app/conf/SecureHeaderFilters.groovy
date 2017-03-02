/**
 * Set HTTP headers improving security
 * Based on the recommendations of <a href="https://observatory.mozilla.org/analyze.html?host=otp.dkfz.de&third-party=false">
 *     Mozilla Observatory test results</a>
 */
class SecureHeaderFilters {
    def filters = {
        addHeaders(uri: '/**') {
            after = {
                response.setHeader("Content-Security-Policy",
                        "default-src https: 'self'; script-src https: 'unsafe-inline' 'self'; style-src https: 'unsafe-inline' 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'; plugin-types application/pdf application/x-shockwave-flash;")
                response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin")
                response.setHeader("X-Content-Type-Options", "nosniff")
                response.setHeader("X-Frame-Options", "DENY")
                response.setHeader("X-XSS-Protection", "1; mode=block")
            }
        }
    }
}
