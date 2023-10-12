package com.toasterofbread.spmp.model.mediaitem.artist

import SpMp
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.amountToString

fun Artist.getSubscriberCount(context: PlatformContext): Int? =
    context.database.artistQueries.subscriberCountById(id).executeAsOne().subscriber_count?.toInt()

fun Int.toReadableSubscriberCount(context: PlatformContext): String =
    getString("artist_x_subscribers").replace("\$x", amountToString(this, context.getUiLanguage()))
