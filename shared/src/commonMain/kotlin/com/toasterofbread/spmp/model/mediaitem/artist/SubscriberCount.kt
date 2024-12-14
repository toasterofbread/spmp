package com.toasterofbread.spmp.model.mediaitem.artist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.platform.observeUiLanguage
import dev.toastbits.composekit.util.model.Locale
import dev.toastbits.ytmkt.uistrings.amountToString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.artist_x_subscribers

fun Artist.getSubscriberCount(context: AppContext): Int? =
    context.database.artistQueries.subscriberCountById(id).executeAsOne().subscriber_count?.toInt()

@Composable
fun Int.toReadableSubscriberCount(context: AppContext): String {
    val ui_language: Locale by context.observeUiLanguage()
    return stringResource(Res.string.artist_x_subscribers).replace("\$x", amountToString(this, ui_language.toTag()))
}
