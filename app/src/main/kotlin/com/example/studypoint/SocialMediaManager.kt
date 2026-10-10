package com.example.studypoint

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Central place for all social / external links.
 * WhatsApp opens the official StudyPoint WhatsApp CHANNEL
 * (not a number chat — support is handled by the AI Assistant).
 */
object SocialMediaManager {

    private const val WHATSAPP_CHANNEL_URL =
        "https://whatsapp.com/channel/0029Vah4pV3Au3aVHGzJL805"

    private const val YOUTUBE_CHANNEL_URL =
        "https://www.youtube.com/@theknowledgehub63"

    // TODO: replace with the official Facebook page URL
    private const val FACEBOOK_PAGE_URL =
        "https://www.facebook.com/alsamadacademy"

    private fun open(context: Context, url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun openWhatsApp(context: Context) = open(context, WHATSAPP_CHANNEL_URL)

    fun openYouTubeChannel(context: Context) = open(context, YOUTUBE_CHANNEL_URL)

    fun openFacebook(context: Context) = open(context, FACEBOOK_PAGE_URL)
}
