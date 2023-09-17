package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.model.TextRuns
import okhttp3.Headers

abstract class YoutubeChannelCreationFormEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun getForm(
        headers: Headers,
        channel_creation_token: String,
    ): Result<YoutubeAccountCreationForm.ChannelCreationForm>

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
            val givenNameLabel: TextRuns?,
            val givenNameValue: String?,
            val collectFamilyName: Boolean,
            val familyNameLabel: TextRuns?,
            val familyNameValue: String?,
            val profilePhoto: ProfilePhoto,
            val missingNameErrorMessage: TextRuns
        ) {
            data class ProfilePhoto(val thumbnails: List<Thumbnail>)
            data class Thumbnail(val url: String)
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
        data class FormButtonRenderer(val serviceEndpoint: FormServiceEndpoint?)
        data class FormServiceEndpoint(val channelCreationServiceEndpoint: ChannelCreationServiceEndpoint)
        data class ChannelCreationServiceEndpoint(val channelCreationToken: String)
    }
}
