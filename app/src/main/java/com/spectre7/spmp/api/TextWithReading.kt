package net.zerotask.libraries.android.compose.furigana

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*

class TermInfo(val text: String, val data: Any?) {
    var rect: Rect = Rect.Zero
}

@Composable
fun MeasureUnconstrainedView(
    viewToMeasure: @Composable () -> Unit,
    content: @Composable (width: Int, height: Int) -> Unit,
) {
    SubcomposeLayout { constraints ->
        val measured = subcompose("viewToMeasure", viewToMeasure)[0]
            .measure(Constraints())

        val contentPlaceable = subcompose("content") {
            content(measured.width, measured.height)
        }[0].measure(constraints)

        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.place(0, 0)
        }
    }
}

@Composable
fun TextWithReading(
    textContent: List<TextData>,
    showReadings: Boolean = false,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    text_positions: MutableList<TermInfo>? = null
) {

    val _fontSize = if (fontSize == TextUnit.Unspecified) style.fontSize else fontSize

    fun calculateAnnotatedString(showReadings: Boolean): Pair<AnnotatedString, Map<String, InlineTextContent>> {
        val inlineContent = mutableMapOf<String, InlineTextContent>()

        return buildAnnotatedString {

            var child_index = 0

            for (elem in textContent) {
                val text = elem.text
                val reading = elem.reading

                // // If there is not reading available, simply add the text and move to the next element.
                // if (reading == null && modifier_provider == null) {
                //     append(text)
                //     continue
                // }

                // Words larger than one character/kanji need a small amount of additional space in their
                // x-dimension.
                val width = (text.length.toDouble() + (text.length - 1) * 0.05).em
                appendInlineContent(text, text)
                inlineContent[text] = InlineTextContent(
                    // TODO: find out why height and width need magic numbers.
                    placeholder = Placeholder(
                        width = width,
                        height = 1.97.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Bottom,
                    ),
                    children = {
                        val readingFontSize = _fontSize / 2
                        val boxHeight = with(LocalDensity.current) { readingFontSize.toDp() }
                        val index = remember { child_index++ }

                        val textElement = @Composable {
                            Text(text = text, fontSize = _fontSize, color = color)
                        }

                        MeasureUnconstrainedView({
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                textElement()
                            }
                        }) { width: Int, _height: Int ->
                            val column_modifier = remember(text_positions == null) {
                                Modifier.fillMaxHeight().run {
                                    if (text_positions != null) {
                                        onPlaced { coords ->
                                            if (text_positions[index].rect.isEmpty) {
                                                text_positions[index].rect = Rect(coords.positionInRoot() - Offset(115f, 165f), Size(width.toFloat(), 70f))
                                            }
                                        }
                                    }
                                    else {
                                        this
                                    }
                                }
                            }

                            Column(
                                modifier = column_modifier,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                Box(modifier = Modifier.requiredHeight(boxHeight + 3.dp)) {
                                    if (showReadings && reading != null) {
                                        Text(
                                            modifier = Modifier.wrapContentWidth(unbounded = true),
                                            text = reading,
                                            style = TextStyle.Default.copy(fontSize = readingFontSize),
                                            color = color
                                        )
                                    }
                                }
                                textElement()
                            }
                        }

                    }
                )
            }
        } to inlineContent
    }

    val dataWithReadings = remember(textContent, style) {
        for (elem in textContent) {
            text_positions?.add(TermInfo(elem.text, elem.data))
        }
        calculateAnnotatedString(true)
    }

    val dataWithoutReadings = remember(textContent, style) {
        calculateAnnotatedString(false)
    }

    val data = if (showReadings) dataWithReadings else dataWithoutReadings
    Text(
        data.first,
        modifier,
        color,
        _fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        letterSpacing,
        textDecoration,
        textAlign,
        lineHeight,
        overflow,
        softWrap,
        maxLines,
        data.second,
        onTextLayout,
        style
    )
}

@Preview
@Composable
internal fun PreviewTextWithReading() {
    val textContent = listOf(
        TextData(text = "このルールを"),
        TextData(text = "守", reading = "まも"),
        TextData(text = "らない"),
        TextData(text = "人", reading = "ひと"),
        TextData(text = "は"),
        TextData(text = "旅行", reading = "りょこう"),
        TextData(text = "ができなくなることもあります。"),
    )

    MaterialTheme {
        TextWithReading(textContent = textContent, showReadings = true)
    }
}

@Preview
@Composable
internal fun PreviewTextWithoutReading() {
    val textContent = listOf(
        TextData(text = "このルールを"),
        TextData(text = "守", reading = "まも"),
        TextData(text = "らない"),
        TextData(text = "人", reading = "ひと"),
        TextData(text = "は"),
        TextData(text = "旅行", reading = "りょこう"),
        TextData(text = "ができなくなることもあります。"),
    )

    MaterialTheme {
        TextWithReading(textContent = textContent, showReadings = true)
    }
}
