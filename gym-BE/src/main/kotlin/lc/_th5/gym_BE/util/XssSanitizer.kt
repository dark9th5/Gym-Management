package lc._th5.gym_BE.util

import org.springframework.stereotype.Component

/**
 * Utility để sanitize input chống XSS (Cross-Site Scripting)
 * 
 * SECURITY FEATURES:
 * - Escape các ký tự HTML đặc biệt
 * - Loại bỏ các script tags và event handlers
 * - Chuẩn hóa input để tránh injection attacks
 * 
 * SỬ DỤNG:
 * - sanitize(): Dùng cho input text thông thường
 * - sanitizeHtml(): Dùng khi cần giữ lại một số HTML tags an toàn
 */
@Component
object XssSanitizer {

    // Các ký tự HTML cần escape
    private val HTML_ESCAPE_MAP = mapOf(
        '&' to "&amp;",
        '<' to "&lt;",
        '>' to "&gt;",
        '"' to "&quot;",
        '\'' to "&#x27;",
        '/' to "&#x2F;",
        '`' to "&#x60;",
        '=' to "&#x3D;"
    )

    // Pattern để phát hiện script injection
    private val SCRIPT_PATTERNS = listOf(
        Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE),
        Regex("<script[^>]*>", RegexOption.IGNORE_CASE),
        Regex("</script>", RegexOption.IGNORE_CASE),
        Regex("javascript:", RegexOption.IGNORE_CASE),
        Regex("vbscript:", RegexOption.IGNORE_CASE),
        Regex("on\\w+\\s*=", RegexOption.IGNORE_CASE), // onclick=, onload=, etc.
        Regex("expression\\s*\\(", RegexOption.IGNORE_CASE),
        Regex("eval\\s*\\(", RegexOption.IGNORE_CASE),
        Regex("document\\.cookie", RegexOption.IGNORE_CASE),
        Regex("document\\.write", RegexOption.IGNORE_CASE),
        Regex("window\\.location", RegexOption.IGNORE_CASE),
        Regex("<iframe[^>]*>", RegexOption.IGNORE_CASE),
        Regex("<object[^>]*>", RegexOption.IGNORE_CASE),
        Regex("<embed[^>]*>", RegexOption.IGNORE_CASE),
        Regex("<svg[^>]*onload", RegexOption.IGNORE_CASE),
        Regex("<img[^>]*onerror", RegexOption.IGNORE_CASE)
    )

    // Các tags HTML an toàn (whitelist)
    private val SAFE_HTML_TAGS = setOf(
        "b", "i", "u", "strong", "em", "br", "p", "ul", "ol", "li",
        "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "code", "pre"
    )

    /**
     * Sanitize input text - escape tất cả HTML
     * Dùng cho tin nhắn, comment, input thông thường
     * 
     * @param input Chuỗi input từ user
     * @return Chuỗi đã được escape an toàn
     */
    fun sanitize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        
        // Step 1: Remove null bytes
        var result = input.replace("\u0000", "")
        
        // Step 2: Remove dangerous patterns first
        SCRIPT_PATTERNS.forEach { pattern ->
            result = result.replace(pattern, "[removed]")
        }
        
        // Step 3: HTML escape remaining content
        val escaped = StringBuilder()
        for (char in result) {
            escaped.append(HTML_ESCAPE_MAP[char] ?: char)
        }
        
        // Step 4: Normalize whitespace
        return escaped.toString()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Sanitize nhưng giữ lại một số HTML tags an toàn
     * Dùng cho content có format như markdown/rich text
     * 
     * @param input Chuỗi HTML input
     * @return Chuỗi HTML đã được làm sạch
     */
    fun sanitizeHtml(input: String?): String {
        if (input.isNullOrBlank()) return ""
        
        var result = input.replace("\u0000", "")
        
        // Remove dangerous patterns
        SCRIPT_PATTERNS.forEach { pattern ->
            result = result.replace(pattern, "")
        }
        
        // Remove attributes from remaining tags (except safe ones)
        result = Regex("<(\\w+)([^>]*)>").replace(result) { match ->
            val tagName = match.groupValues[1].lowercase()
            if (tagName in SAFE_HTML_TAGS) {
                "<$tagName>" // Keep tag but remove all attributes
            } else {
                "&lt;$tagName&gt;" // Escape unsafe tags
            }
        }
        
        // Handle closing tags
        result = Regex("</(\\w+)>").replace(result) { match ->
            val tagName = match.groupValues[1].lowercase()
            if (tagName in SAFE_HTML_TAGS) {
                "</$tagName>"
            } else {
                "&lt;/$tagName&gt;"
            }
        }
        
        return result.trim()
    }

    /**
     * Kiểm tra xem input có chứa nội dung nguy hiểm không
     * Dùng để validate trước khi xử lý
     * 
     * @param input Chuỗi cần kiểm tra
     * @return true nếu phát hiện nội dung nguy hiểm
     */
    fun containsDangerousContent(input: String?): Boolean {
        if (input.isNullOrBlank()) return false
        
        return SCRIPT_PATTERNS.any { pattern ->
            pattern.containsMatchIn(input)
        }
    }

    /**
     * Sanitize cho SQL (phòng ngừa bổ sung, JPA đã handle)
     */
    fun sanitizeForSql(input: String?): String {
        if (input.isNullOrBlank()) return ""
        
        return input
            .replace("'", "''")
            .replace("\\", "\\\\")
            .replace("\u0000", "")
            .trim()
    }

    /**
     * Sanitize cho JSON output
     */
    fun sanitizeForJson(input: String?): String {
        if (input.isNullOrBlank()) return ""
        
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\u0000", "")
    }

    /**
     * Extension function cho String
     */
    fun String?.xssSafe(): String = sanitize(this)
}
