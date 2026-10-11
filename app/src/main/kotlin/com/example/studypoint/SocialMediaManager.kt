package com.example.studypoint

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat

object SocialMediaManager {

    const val FACEBOOK_URL = "https://www.facebook.com/StudyPointOfficialPakistan"
    const val FACEBOOK_PAGE_ID = "StudyPointOfficialPakistan"
    const val WHATSAPP_NUMBER = "+923276131242"
    const val WHATSAPP_CLEAN_NUMBER = "923276131242"
    const val DEFAULT_SUPPORT_MESSAGE = "Hello! I need support regarding the live lectures / app."
    const val YOUTUBE_CHANNEL_URL = "https://www.youtube.com/@StudyPointLectures"

    /**
     * Opens Facebook Official Page / Group via deep link (Facebook App)
     * with smooth fallback to Chrome Custom Tabs or browser.
     */
    fun openFacebook(
        context: Context,
        pageUrl: String = FACEBOOK_URL,
        pageId: String = FACEBOOK_PAGE_ID
    ) {
        try {
            // Try Facebook App intent
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("fb://facewebmodal/f?href=$pageUrl")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to Chrome Custom Tabs
            openCustomTab(context, pageUrl)
        }
    }

    /**
     * Opens WhatsApp support chat directly to +923276131242
     * Gracefully falls back to browser if WhatsApp is not installed.
     */
    fun openWhatsApp(
        context: Context,
        phoneNumber: String = WHATSAPP_NUMBER,
        message: String = DEFAULT_SUPPORT_MESSAGE
    ) {
        val cleanNumber = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
        val encodedMsg = Uri.encode(message)
        val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedMsg"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                openCustomTab(context, url)
            }
        }
    }

    /**
     * Dedicated WhatsApp Live Lectures Support launcher
     */
    fun openWhatsAppLectureSupport(context: Context) {
        openWhatsApp(context, WHATSAPP_NUMBER, DEFAULT_SUPPORT_MESSAGE)
    }

    /**
     * Opens YouTube Official Channel
     */
    fun openYouTubeChannel(context: Context, channelUrl: String = YOUTUBE_CHANNEL_URL) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(channelUrl)).apply {
                setPackage("com.google.android.youtube")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openCustomTab(context, channelUrl)
        }
    }

    /**
     * Opens any link in a high performance, styled Chrome Custom Tab
     */
    fun openCustomTab(context: Context, url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(ContextCompat.getColor(context, R.color.primaryColor))
                .build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not open link: $url", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
