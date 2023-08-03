package com.toasterofbread.spmp.api.model

data class ItemSectionRenderer(val contents: List<ItemSectionRendererContent>)
data class ItemSectionRendererContent(val didYouMeanRenderer: DidYouMeanRenderer? = null)
data class DidYouMeanRenderer(val correctedQuery: TextRuns)
