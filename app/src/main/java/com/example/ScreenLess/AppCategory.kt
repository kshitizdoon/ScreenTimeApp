package com.example.ScreenLess

enum class AppCategory(
    val label: String
) {

    PRODUCTIVE("Productive"),

    MESSAGING("Messaging"),

    SOCIAL_MEDIA("Social Media"),

    VIDEO_AND_REELS("Video & Reels"),

    SHOPPING("Shopping / Ecommerce"),

    LOCAL_MEDIA("Local Media"),

    NEWS("News"),

    BROWSING("Web Browsing"),

    GAMES("Games"),

    UNCATEGORIZED("Uncategorized");


    companion object {

        val assignable = listOf(
            PRODUCTIVE,
            MESSAGING,
            SOCIAL_MEDIA,
            VIDEO_AND_REELS,
            SHOPPING,
            LOCAL_MEDIA,
            NEWS,
            BROWSING,
            GAMES
        )
    }
}