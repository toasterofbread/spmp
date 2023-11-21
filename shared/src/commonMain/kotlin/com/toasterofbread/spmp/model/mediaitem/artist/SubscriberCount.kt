package com.toasterofbread.spmp.model.mediaitem.artist

import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.amountToString

fun Artist.getSubscriberCount(context: AppContext): Int? =
    context.database.artistQueries.subscriberCountById(id).executeAsOne().subscriber_count?.toInt()

fun Int.toReadableSubscriberCount(context: AppContext): String =
    getString("artist_x_subscribers").replace("\$x", amountToString(this, context.getUiLanguage()))
