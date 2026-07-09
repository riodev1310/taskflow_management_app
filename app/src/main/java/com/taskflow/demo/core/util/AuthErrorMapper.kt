package com.taskflow.demo.core.util

object AuthErrorMapper {
    fun isEmailNotConfirmed(message: String?): Boolean {
        val raw = message.orEmpty().lowercase()
        return raw.contains("email_not_confirmed") || raw.contains("email not confirmed")
    }

    fun map(message: String?): String {
        val raw = message.orEmpty().lowercase()
        return when {
            isEmailNotConfirmed(message) -> {
                "Email chưa xác thực. Hãy kiểm tra hộp thư hoặc tắt Email Confirmation trong Supabase Auth khi demo."
            }
            raw.contains("invalid login credentials") -> "Email hoặc mật khẩu không đúng."
            raw.contains("user already registered") -> "Email này đã được đăng ký."
            raw.contains("signup is disabled") -> "Đăng ký đang bị tắt trong Supabase Auth."
            raw.contains("network") || raw.contains("timeout") -> "Kết nối mạng không ổn định. Vui lòng thử lại."
            raw.contains("supabase_url") || raw.contains("supabase_anon_key") -> {
                "Thiếu cấu hình Supabase URL hoặc anon key trong local.properties."
            }
            else -> message?.takeIf { it.isNotBlank() } ?: "Có lỗi xảy ra, vui lòng thử lại."
        }
    }
}
