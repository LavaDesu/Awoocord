package com.discord.api.botuikit

data class UnfurledMediaItem(
    val url: String,
    @b.i.d.p.b("proxy_url") val proxyUrl: String,
    val height: Int,
    val width: Int,
    @b.i.d.p.b("content_type") val contentType: String?,
    @b.i.d.p.b("attachment_id") val attachmentId: Long?,
)
