package com.example.submate.domain

data class ServiceTemplate(val key: String, val name: String, val mark: String, val category: String, val color: Long, val aliases: List<String> = emptyList())

object ServiceCatalog {
    val categories = listOf("영상", "음악", "AI·생산성", "클라우드", "쇼핑", "게임", "생활", "기타")
    val currencies = listOf("KRW", "USD", "JPY", "EUR", "HKD", "CNY")
    val services = listOf(
        ServiceTemplate("netflix", "넷플릭스", "N", "영상", 0xFFE50914, listOf("netflix")),
        ServiceTemplate("youtube", "YouTube Premium", "▶", "영상", 0xFFFF0033, listOf("유튜브", "youtube")),
        ServiceTemplate("disney", "디즈니+", "D+", "영상", 0xFF193BA8, listOf("disney")),
        ServiceTemplate("tving", "티빙", "T", "영상", 0xFFFF2445, listOf("tving")),
        ServiceTemplate("wavve", "웨이브", "W", "영상", 0xFF1365F5, listOf("wavve")),
        ServiceTemplate("coupangplay", "쿠팡플레이", "CP", "영상", 0xFF1676EB, listOf("coupang play")),
        ServiceTemplate("spotify", "Spotify", "S", "음악", 0xFF14843D, listOf("스포티파이")),
        ServiceTemplate("applemusic", "Apple Music", "♪", "음악", 0xFFE84262, listOf("애플뮤직")),
        ServiceTemplate("melon", "멜론", "M", "음악", 0xFF138E47, listOf("melon")),
        ServiceTemplate("chatgpt", "ChatGPT", "AI", "AI·생산성", 0xFF178571, listOf("openai", "챗지피티")),
        ServiceTemplate("claude", "Claude", "C", "AI·생산성", 0xFFB96C4E, listOf("클로드", "anthropic")),
        ServiceTemplate("notion", "Notion", "N", "AI·생산성", 0xFF252525, listOf("노션")),
        ServiceTemplate("microsoft", "Microsoft 365", "365", "AI·생산성", 0xFF2563B4, listOf("마이크로소프트")),
        ServiceTemplate("icloud", "iCloud+", "iC", "클라우드", 0xFF1683E4, listOf("아이클라우드")),
        ServiceTemplate("googleone", "Google One", "G", "클라우드", 0xFF3774C7, listOf("구글 원", "google one")),
        ServiceTemplate("coupang", "쿠팡 와우", "W", "쇼핑", 0xFFAC5934, listOf("쿠팡와우")),
        ServiceTemplate("naver", "네이버플러스", "N+", "쇼핑", 0xFF07924C, listOf("네이버 플러스")),
        ServiceTemplate("gamepass", "Xbox Game Pass", "X", "게임", 0xFF147534, listOf("게임패스", "game pass")),
        ServiceTemplate("custom", "직접 입력", "+", "기타", 0xFF60786E)
    )
    fun get(key: String) = services.firstOrNull { it.key == key } ?: services.last()
    fun match(text: String) = services.dropLast(1).firstOrNull { service ->
        (service.aliases + service.name).any { text.contains(it, ignoreCase = true) }
    }
}
