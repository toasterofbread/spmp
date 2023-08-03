package com.toasterofbread.spmp.api.model

data class SubscriptionButton(val subscribeButtonRenderer: SubscribeButtonRenderer)
data class SubscribeButtonRenderer(val subscribed: Boolean, val subscriberCountText: TextRuns, val channelId: String)

data class MoreContentButton(val buttonRenderer: ButtonRenderer)
data class ButtonRenderer(val navigationEndpoint: NavigationEndpoint)
