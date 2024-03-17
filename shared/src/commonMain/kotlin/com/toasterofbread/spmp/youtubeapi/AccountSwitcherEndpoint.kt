package com.toasterofbread.spmp.youtubeapi

import dev.toastbits.ytmkt.model.internal.MusicThumbnailRenderer

data class AccountSwitcherEndpoint(val data: Data) {
    fun getAccounts(): List<AccountItem> =
        data.actions.firstOrNull()
            ?.getMultiPageMenuAction?.menu?.multiPageMenuRenderer?.sections?.firstOrNull()
            ?.accountSectionListRenderer?.contents?.firstOrNull()
            ?.accountItemSectionRenderer?.contents?.mapNotNull {
                it.accountItem
            }
        ?: emptyList()

    data class Data(val actions: List<Action>)
    data class Action(val getMultiPageMenuAction: GetMultiPageMenuAction)
    data class GetMultiPageMenuAction(val menu: Menu)
    data class Menu(val multiPageMenuRenderer: MultiPageMenuRenderer)
    data class MultiPageMenuRenderer(val sections: List<Section>)
    data class Section(val accountSectionListRenderer: AccountSectionListRenderer)
    data class AccountSectionListRenderer(val contents: List<Content>)
    data class Content(val accountItemSectionRenderer: AccountItemSectionRenderer)
    data class AccountItemSectionRenderer(val contents: List<Account>)
    data class Account(val accountItem: AccountItem?)

    data class AccountItem(
        val accountName: AccountItemText,
        val accountPhoto: MusicThumbnailRenderer.RendererThumbnail,
        val channelHandle: AccountItemText?,
        val accountByline: AccountItemText,
        val isDisabled: Boolean,
        val isSelected: Boolean,
        val serviceEndpoint: ServiceEndpoint
    )
    data class AccountItemText(val simpleText: String)
    data class ServiceEndpoint(val selectActiveIdentityEndpoint: SelectActiveIdentityEndpoint)
    data class SelectActiveIdentityEndpoint(val supportedTokens: List<Token>)
    data class Token(val accountSigninToken: AccountSigninToken?, val offlineCacheKeyToken: OfflineCacheKeyToken?)
    data class AccountSigninToken(val signinUrl: String)
    data class OfflineCacheKeyToken(val clientCacheKey: String)
}
