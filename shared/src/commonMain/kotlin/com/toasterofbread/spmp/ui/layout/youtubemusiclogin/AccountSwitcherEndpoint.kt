package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import com.toasterofbread.spmp.youtubeapi.model.MusicThumbnailRenderer
import com.toasterofbread.spmp.youtubeapi.model.TextRuns

internal class AccountSwitcherEndpoint(val data: Data) {
    fun getAccounts(): List<AccountItem> =
        data.actions.firstOrNull()
            ?.getMultiPageMenuAction?.menu?.multiPageMenuRenderer?.sections?.firstOrNull()
            ?.accountSectionListRenderer?.contents?.firstOrNull()
            ?.accountItemSectionRenderer?.contents?.map {
                it.accountItem
            }
        ?: emptyList()

    class Data(val actions: List<Action>)
    class Action(val getMultiPageMenuAction: GetMultiPageMenuAction)
    class GetMultiPageMenuAction(val menu: Menu)
    class Menu(val multiPageMenuRenderer: MultiPageMenuRenderer)
    class MultiPageMenuRenderer(val sections: List<Section>)
    class Section(val accountSectionListRenderer: AccountSectionListRenderer)
    class AccountSectionListRenderer(val contents: List<Content>)
    class Content(val accountItemSectionRenderer: AccountItemSectionRenderer)
    class AccountItemSectionRenderer(val contents: List<Account>)
    class Account(val accountItem: AccountItem)

    data class AccountItem(
        val accountName: TextRuns,
        val accountPhoto: MusicThumbnailRenderer.Thumbnail,
        val channelHandle: TextRuns? = null,
        val accountByline: TextRuns,
        val isDisabled: Boolean,
        val isSelected: Boolean,
        val serviceEndpoint: ServiceEndpoint
    )
    data class ServiceEndpoint(val selectActiveIdentityEndpoint: SelectActiveIdentityEndpoint)
    data class SelectActiveIdentityEndpoint(val supportedTokens: List<Token>)
    data class Token(val accountSigninToken: AccountSigninToken? = null, val offlineCacheKeyToken: OfflineCacheKeyToken? = null)
    data class AccountSigninToken(val signinUrl: String)
    data class OfflineCacheKeyToken(val clientCacheKey: String)
}
