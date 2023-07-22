package com.toasterofbread.spmp.api

import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request

suspend fun getYoutubeAccountCreationForm(cookie: String, headers: Map<String, String>, channel_creation_token: String): Result<YoutubeAccountCreationForm.ChannelCreationForm> =
    withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .ytUrl("/youtubei/v1/channel/get_channel_creation_form")
            .headers(headers.toHeaders())
            .header("COOKIE", cookie)
            .post(
                Api.getYoutubeiRequestBody(
                    mapOf(
                        "source" to "MY_CHANNEL_CHANNEL_CREATION_SOURCE",
                        "channelCreationToken" to channel_creation_token
                    ),
                    context = Api.Companion.YoutubeiContextType.UI_LANGUAGE
                )
            )
            .build()

        val result = Api.request(request)
        val stream = result.getOrNull()?.getStream(false) ?: return@withContext result.cast()

        try {
            val creation_form: YoutubeAccountCreationForm = Api.klaxon.parse(stream)!!
            return@withContext Result.success(creation_form.channelCreation.channelCreationForm)
        } catch (e: Throwable) {
            return@withContext Result.failure(e)
        } finally {
            stream.close()
        }
    }

data class YoutubeAccountCreationForm(
    val channelCreation: ChannelCreation
) {
    data class ChannelCreation(val channelCreationForm: ChannelCreationForm)
    data class ChannelCreationForm(val contents: Contents, val buttons: List<Button>) {
        fun getChannelCreationToken(): String? {
            for (button in buttons) {
                val token = button.buttonRenderer.serviceEndpoint?.channelCreationServiceEndpoint?.channelCreationToken
                if (token != null) {
                    return token
                }
            }
            return null
        }
    }
    data class Contents(val createCoreIdentityChannelContentRenderer: CreateCoreIdentityChannelContentRenderer)
    data class CreateCoreIdentityChannelContentRenderer(
        val collectGivenName: Boolean,
        val givenNameLabel: TextRuns? = null,
        val givenNameValue: String? = null,
        val collectFamilyName: Boolean,
        val familyNameLabel: TextRuns? = null,
        val familyNameValue: String? = null,
        val profilePhoto: MusicThumbnailRenderer.Thumbnail,
        val missingNameErrorMessage: TextRuns
    ) {
        data class InputField(val label: String?, val default: String?, val key: String)

        fun getInputFields(): List<InputField> {
            val ret: MutableList<InputField> = mutableListOf()
            if (collectGivenName) {
                ret.add(InputField(givenNameLabel?.firstTextOrNull(), givenNameValue, "givenName"))
            }
            if (collectFamilyName) {
                ret.add(InputField(familyNameLabel?.firstTextOrNull(), familyNameValue, "familyName"))
            }
            return ret
        }
    }
    data class Button(val buttonRenderer: FormButtonRenderer)
    data class FormButtonRenderer(val serviceEndpoint: FormServiceEndpoint? = null)
    data class FormServiceEndpoint(val channelCreationServiceEndpoint: ChannelCreationServiceEndpoint)
    data class ChannelCreationServiceEndpoint(val channelCreationToken: String)
}
