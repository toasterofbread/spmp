package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.toasterofbread.spmp.youtubeapi.AccountSwitcherEndpoint
import io.ktor.http.Headers
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.youtube_account_selection_title

internal data class AccountSelectionData(val accounts: List<AccountSwitcherEndpoint.AccountItem>, val headers: Headers)

@Composable
internal fun AccountSelectionPage(data: AccountSelectionData, modifier: Modifier = Modifier, onAccountSelected: (AccountSwitcherEndpoint.AccountItem) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(25.dp)) {
        Text(stringResource(Res.string.youtube_account_selection_title), style = MaterialTheme.typography.headlineMedium)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(data.accounts) { account ->
                if (account.isDisabled) {
                    return@items
                }

                account.Item {
                    onAccountSelected(account)
                }
            }
        }
    }
}

@Composable
private fun AccountSwitcherEndpoint.AccountItem.Item(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val thumbnail_url = accountPhoto.thumbnails.firstOrNull()?.url
            if (thumbnail_url != null) {
                AsyncImage(thumbnail_url, null, Modifier.size(40.dp).clip(CircleShape))
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(accountName.simpleText, style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    val handle = channelHandle?.simpleText
                    val byline = accountByline.simpleText

                    if (handle != null) {
                        Text(handle)
                    }

                    if (byline != null) {
                        if (handle != null) {
                            Text("\u2022")
                        }
                        Text(byline)
                    }
                }
            }
        }
    }
}
