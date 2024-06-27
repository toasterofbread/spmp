package com.toasterofbread.spmp.youtubeapi

import dev.toastbits.ytmkt.model.internal.MusicThumbnailRenderer
import kotlinx.serialization.Serializable

@Serializable
data class AccountSwitcherEndpoint(val data: Data) {
    fun getAccounts(): List<AccountItem> =
        data.actions.firstOrNull()
            ?.getMultiPageMenuAction?.menu?.multiPageMenuRenderer?.sections?.firstOrNull()
            ?.accountSectionListRenderer?.contents?.firstOrNull()
            ?.accountItemSectionRenderer?.contents?.mapNotNull {
                it.accountItem
            }
        ?: emptyList()

    @Serializable
    data class Data(val actions: List<Action>)
    @Serializable
    data class Action(val getMultiPageMenuAction: GetMultiPageMenuAction)
    @Serializable
    data class GetMultiPageMenuAction(val menu: Menu)
    @Serializable
    data class Menu(val multiPageMenuRenderer: MultiPageMenuRenderer)
    @Serializable
    data class MultiPageMenuRenderer(val sections: List<Section>)
    @Serializable
    data class Section(val accountSectionListRenderer: AccountSectionListRenderer)
    @Serializable
    data class AccountSectionListRenderer(val contents: List<Content>)
    @Serializable
    data class Content(val accountItemSectionRenderer: AccountItemSectionRenderer)
    @Serializable
    data class AccountItemSectionRenderer(val contents: List<Account>)
    @Serializable
    data class Account(val accountItem: AccountItem? = null)

    @Serializable
    data class AccountItem(
        val accountName: AccountItemText,
        val accountPhoto: MusicThumbnailRenderer.RendererThumbnail,
        val channelHandle: AccountItemText? = null,
        val accountByline: AccountItemText,
        val isDisabled: Boolean,
        val isSelected: Boolean,
        val serviceEndpoint: ServiceEndpoint
    )
    @Serializable
    data class AccountItemText(val simpleText: String)
    @Serializable
    data class ServiceEndpoint(val selectActiveIdentityEndpoint: SelectActiveIdentityEndpoint? = null)
    @Serializable
    data class SelectActiveIdentityEndpoint(val supportedTokens: List<Token>)
    @Serializable
    data class Token(val accountSigninToken: AccountSigninToken? = null, val offlineCacheKeyToken: OfflineCacheKeyToken? = null)
    @Serializable
    data class AccountSigninToken(val signinUrl: String)
    @Serializable
    data class OfflineCacheKeyToken(val clientCacheKey: String)
}
