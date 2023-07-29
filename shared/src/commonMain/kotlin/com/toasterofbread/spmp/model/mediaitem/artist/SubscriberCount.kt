package com.toasterofbread.spmp.model.mediaitem.artist

import SpMp
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.amountToString

fun Artist.getSubscriberCount(db: Database = SpMp.context.database): Int? =
    db.artistQueries.subscriberCountById(id).executeAsOne().subscriber_count?.toInt()

fun Artist.getReadableSubscriberCount(db: Database = SpMp.context.database): String =
    getSubscriberCount(db)?.let { subscriber_count ->
        getString("artist_x_subscribers").replace("\$x", amountToString(subscriber_count, SpMp.ui_language))
    } ?: ""
