package com.toasterofbread.utils.composable

import LocalPlayerState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.toasterofbread.spmp.ui.theme.Theme
import java.util.regex.Pattern

// https://stackoverflow.com/a/66235329
@Composable
fun LinkifyText(
    text: String,
    modifier: Modifier = Modifier,
    colour: Color = LocalContentColor.current,
    highlight_colour: Color = LocalPlayerState.current.theme.accent,
    style: TextStyle = LocalTextStyle.current,
    font_size: TextUnit = TextUnit.Unspecified
) {
	val annotated_string = buildAnnotatedString {
        append(text)
        text.extractURLs().forEach { link ->
            addStyle(
                style = SpanStyle(
                    color = highlight_colour,
                    textDecoration = TextDecoration.Underline
                ),
                start = link.second,
                end = link.third
            )
            addStringAnnotation(
                tag = "URL",
                annotation = link.first,
                start = link.second,
                end = link.third
            )
        }
    }

	val uri_handler = LocalUriHandler.current
	val layout_result = remember {
        mutableStateOf<TextLayoutResult?>(null)
    }

    Text(
        text = annotated_string,
        color = colour,
        style = style,
        fontSize = font_size,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offsetPosition ->
                layout_result.value?.let {
                    val position = it.getOffsetForPosition(offsetPosition)
                    annotated_string.getStringAnnotations(position, position).firstOrNull()
                        ?.let { result ->
                            if (result.tag == "URL") {
                                uri_handler.openUri(result.item)
                            }
                        }
                }
            }
        },
        onTextLayout = { layout_result.value = it }
    )
}

private val URL_PATTERN: Pattern = Pattern.compile(
    "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
    Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL
)

private fun String.extractURLs(): List<Triple<String, Int, Int>> {
    val matcher = URL_PATTERN.matcher(this)
    var start: Int
    var end: Int
    val links = arrayListOf<Triple<String, Int, Int>>()

    while (matcher.find()) {
        start = matcher.start(1)
        end = matcher.end()

        var url = substring(start, end)
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        links.add(Triple(url, start, end))
    }
    return links
}
