package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.platform.composable.rememberImagePainter
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.endpoint.YoutubeChannelCreationFormEndpoint.YoutubeAccountCreationForm.ChannelCreationForm
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.utils.composable.LinkifyText
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.composable.WidthShrinkText
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import okhttp3.Headers

@Composable
fun YoutubeChannelCreateDialog(
    headers: Headers,
    form: ChannelCreationForm,
    api: YoutubeMusicApi,
    onFinished: (Result<Artist>?) -> Unit
) {
    val coroutine_scope = rememberCoroutineScope()

    val fields = remember(form) { form.contents.createCoreIdentityChannelContentRenderer.getInputFields() }
    val params = remember(fields) {
        mutableStateMapOf<String, String>().apply {
            for (field in fields) {
                put(field.key, field.default ?: "")
            }
        }
    }
    val can_create = params[fields[0].key]!!.isNotBlank()

    PlatformAlertDialog(
        onDismissRequest = { onFinished(null) },
        confirmButton = {
            var loading by remember { mutableStateOf(false) }

            Button(
                {
                    if (loading) {
                        return@Button
                    }

                    coroutine_scope.launch {
                        loading = true
                        coroutineContext.job.invokeOnCompletion {
                            loading = false
                        }

                        onFinished(runCatching {
                            api.CreateYoutubeChannel.createYoutubeChannel(headers, form.getChannelCreationToken()!!, params).getOrThrow()
                        })
                    }
                },
                enabled = can_create
            ) {
                Crossfade(loading) { loading ->
                    Box(contentAlignment = Alignment.Center) {
                        if (loading) {
                            SubtleLoadingIndicator()
                        }
                        Text(
                            getString("youtube_channel_creation_confirm"),
                            Modifier.alpha(if (loading) 0f else 1f)
                        )
                    }
                }
            }
        },
        dismissButton = {
            ShapedIconButton(
                { onFinished(null) }
            ) {
                Icon(Icons.Default.Close, null)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val thumbnail = form.contents.createCoreIdentityChannelContentRenderer.profilePhoto.thumbnails.firstOrNull()
                if (thumbnail != null) {
                    Image(rememberImagePainter(thumbnail.url), null, Modifier.size(30.dp).clip(CircleShape))
                }
                WidthShrinkText(getString("youtube_channel_creation_title"))
            }
        }
    ) {
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            with(form.contents.createCoreIdentityChannelContentRenderer) {
                for (item in fields.withIndex()) {
                    val field = item.value
                    val is_error = item.index == 0 && !can_create

                    TextField(
                        params[field.key]!!,
                        { params[field.key] = it },
                        label = {
                            Text(field.label ?: field.key)
                        },
                        supportingText = {
                            AnimatedVisibility(is_error) {
                                Text(missingNameErrorMessage.firstTextOrNull() ?: getString("error_message_generic"))
                            }
                        },
                        isError = is_error
                    )
                }
            }

            LinkifyText(
                getString("youtube_channel_creation_subtitle"),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
