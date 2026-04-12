package com.example.subway_alert_frontend.utils

object HangulUtils {
    private val CHOSUNG = listOf(
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    )

    private fun getChosung(c: Char): Char {
        if (c !in '가'..'힣') return c
        return CHOSUNG[(c - '가') / 588]
    }

    /**
     * 완성자/초성 혼합 매칭 로직
     * query: 사용자의 입력 (예: "돌", "돌ㄱ", "ㄷㄹㄱ")
     * target: 역 이름 (예: "돌곶이")
     */
    fun match(query: String, target: String): Boolean {
        if (query.isBlank()) return false
        val q = query.replace(" ", "")
        val t = target.replace(" ", "")

        if (q.length > t.length) return false

        // target의 어느 지점에서 시작하든 query와 매칭되는지 확인
        for (i in 0..t.length - q.length) {
            var isMatch = true
            for (j in q.indices) {
                val qc = q[j]
                val tc = t[i + j]

                if (qc in 'ㄱ'..'ㅎ') {
                    // 입력이 초성인 경우 -> 대상 글자의 초성과 일치해야 함
                    if (getChosung(tc) != qc) {
                        isMatch = false
                        break
                    }
                } else {
                    // 입력이 완성자인 경우 -> 대상 글자와 정확히 일치해야 함
                    if (qc != tc) {
                        isMatch = false
                        break
                    }
                }
            }
            if (isMatch) return true
        }
        return false
    }

    fun getChosungString(text: String): String {
        return text.map { getChosung(it) }.joinToString("")
    }
}
