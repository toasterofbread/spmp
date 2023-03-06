package com.spectre7.utils

// TODO | Move to separate repository

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.MainActivity
import kotlinx.coroutines.delay
import java.util.regex.Pattern
import kotlin.concurrent.thread
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

fun Boolean.toInt() = if (this) 1 else 0
fun Boolean.toFloat() = if (this) 1f else 0f

fun vibrate(duration: Double, context: Context = MainActivity.context) {
	val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
	vibrator.vibrate(VibrationEffect.createOneShot((duration * 1000.0).toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
}

fun vibrateShort() {
	vibrate(0.01)
}

fun sendToast(text: String, length: Int = Toast.LENGTH_SHORT, context: Context = MainActivity.context) {
	try {
		Toast.makeText(context, text, length).show()
	}
	catch (_: NullPointerException) {
		Looper.prepare()
		Toast.makeText(context, text, length).show()
	}
}

fun getString(id: Int, context: Context = MainActivity.context): String {
	return context.resources.getString(id)
}

fun getString(temp_string: String): String {
	println("Unlocalised string used: '$temp_string'")
	// throw NotImplementedError(temp_string)
	return temp_string
}

fun getAppName(context: Context): String {
	val info = context.applicationInfo
	val string_id = info.labelRes
	return if (string_id == 0) info.nonLocalizedLabel.toString() else context.getString(string_id)
}

@SuppressLint("InternalInsetResource", "DiscouragedApi")
@Composable
fun getStatusBarHeight(context: Context = MainActivity.context): Dp {
	var height: Dp? by remember { mutableStateOf(null) }
	if (height != null) {
		return height!!
	}

	val resource_id: Int = context.resources.getIdentifier("status_bar_height", "dimen", "android")
	if (resource_id > 0) {
		with(LocalDensity.current) {
			height = context.resources.getDimensionPixelSize(resource_id).toDp()
			return height!!
		}
	}

	throw RuntimeException()
}

@Composable
fun NoRipple(content: @Composable () -> Unit) {
	CompositionLocalProvider(LocalRippleTheme provides object : RippleTheme {
		@Composable
		override fun defaultColor() = Color.Unspecified

		@Composable
		override fun rippleAlpha(): RippleAlpha = RippleAlpha(0f, 0f, 0f, 0f)
	}) {
		content()
	}
}

@Composable
fun OnChangedEffect(key: Any?, block: suspend () -> Unit) {
	var launched by remember { mutableStateOf(false) }
	LaunchedEffect(key) {
		if (!launched) {
			launched = true
		}
		else {
			block()
		}
	}
}

@Composable
fun OnChangedEffect(key1: Any?, key2: Any?, block: suspend () -> Unit) {
	var launched by remember { mutableStateOf(false) }
	LaunchedEffect(key1, key2) {
		if (!launched) {
			launched = true
		}
		else {
			block()
		}
	}
}

@Composable
fun MeasureUnconstrainedView(
	view_to_measure: @Composable () -> Unit,
	view_constraints: Constraints = Constraints(),
	content: @Composable (width: Int, height: Int) -> Unit,
) {
	SubcomposeLayout { constraints ->
		val measurement = subcompose("viewToMeasure", view_to_measure)[0].measure(view_constraints)

		val contentPlaceable = subcompose("content") {
			content(measurement.width, measurement.height)
		}[0].measure(constraints)

		layout(contentPlaceable.width, contentPlaceable.height) {
			contentPlaceable.place(0, 0)
		}
	}
}

@Composable
fun Marquee(autoscroll: Boolean = false, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
	MeasureUnconstrainedView(content) { content_width: Int, _ ->
		val scroll_state = rememberScrollState()

		if (autoscroll) {
			var animation_state by remember {
				mutableStateOf(true)
			}

			LaunchedEffect(key1 = animation_state){
				scroll_state.animateScrollTo(
					scroll_state.maxValue,
					animationSpec = tween(1000000 / content_width, 200, easing = CubicBezierEasing(0f,0f,0f,0f))
				)
				delay(2000)
				scroll_state.animateScrollTo(
					0,
					animationSpec = tween(500, 200, easing = CubicBezierEasing(0f,0f,0f,0f))
				)
				delay(2000)
				animation_state = !animation_state
			}

			Row(modifier.horizontalScroll(scroll_state, false)) {
				content()
			}
		}
		else {
			val density = LocalDensity.current
			var container_width by remember { mutableStateOf(0) }

			LaunchedEffect(scroll_state.isScrollInProgress) {
				val max_scroll = content_width - container_width
				if (scroll_state.value > max_scroll) {
					scroll_state.scrollTo(max_scroll)
				}
			}

			val scroll_value by remember { derivedStateOf { with (density) {
				if (container_width >= content_width) {
					0.dp
				}
				else {
					(-scroll_state.value).coerceIn(container_width - content_width, 0).toDp()
				}
			} } }

			Row(
				modifier
					.scrollable(
						scroll_state,
						Orientation.Horizontal,
						reverseDirection = true
					)
					.fillMaxWidth()
					.clipToBounds()
					.onSizeChanged {
						container_width = it.width
					}
			) {
				Row(
					Modifier
						.requiredWidth(with(density) { container_width.toDp() - scroll_value })
						.offset(scroll_value / 2)
				) {
					content()
				}
			}
		}
	}
}

@Composable
fun WidthShrinkText(text: String, style: MutableState<TextStyle>, modifier: Modifier = Modifier) {
	var ready_to_draw by remember { mutableStateOf(false) }

	Text(
		text,
		modifier.drawWithContent { if (ready_to_draw) drawContent() },
		maxLines = 1,
		softWrap = false,
		style = style.value,
		onTextLayout = { layout_result ->
			if (!layout_result.didOverflowWidth) {
				ready_to_draw = true
			}
			else {
				style.value = style.value.copy(fontSize = style.value.fontSize * 0.99)
			}
		}
	)
}

@Composable
fun WidthShrinkText(text: String, fontSize: TextUnit, modifier: Modifier = Modifier, fontWeight: FontWeight? = null) {
	WidthShrinkText(
		text,
		remember { mutableStateOf(TextStyle(
			fontSize = fontSize,
			fontWeight = fontWeight
		)) },
		modifier
	)
}

@Composable
fun WidthShrinkText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
	WidthShrinkText(text, remember(style) { mutableStateOf(style) }, modifier)
}

// https://stackoverflow.com/a/66235329
@Composable
fun LinkifyText(
	text: String,
	colour: Color,
	highlight_colour: Color,
	style: TextStyle,
	modifier: Modifier = Modifier
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

fun String.extractURLs(): List<Triple<String, Int, Int>> {
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

@Stable
fun Modifier.recomposeHighlighter(): Modifier = this.then(recomposeModifier)

// Use a single instance + @Stable to ensure that recompositions can enable skipping optimizations
// Modifier.composed will still remember unique data per call site.
private val recomposeModifier =
	Modifier.composed(inspectorInfo = debugInspectorInfo { name = "recomposeHighlighter" }) {
		// The total number of compositions that have occurred. We're not using a State<> here be
		// able to read/write the value without invalidating (which would cause infinite
		// recomposition).
		val totalCompositions = remember { arrayOf(0L) }
		totalCompositions[0]++

		// The value of totalCompositions at the last timeout.
		val totalCompositionsAtLastTimeout = remember { mutableStateOf(0L) }

		// Start the timeout, and reset everytime there's a recomposition. (Using totalCompositions
		// as the key is really just to cause the timer to restart every composition).
		LaunchedEffect(totalCompositions[0]) {
			delay(3000)
			totalCompositionsAtLastTimeout.value = totalCompositions[0]
		}

        return@composed Modifier.drawWithCache {
            onDrawWithContent {
                // Draw actual content.
                drawContent()

                // Below is to draw the highlight, if necessary. A lot of the logic is copied from
                // Modifier.border
                val numCompositionsSinceTimeout =
                    totalCompositions[0] - totalCompositionsAtLastTimeout.value

                val hasValidBorderParams = size.minDimension > 0f
                if (!hasValidBorderParams || numCompositionsSinceTimeout <= 0) {
                    return@onDrawWithContent
                }

                val (color, strokeWidthPx) =
                    when (numCompositionsSinceTimeout) {
                        // We need at least one composition to draw, so draw the smallest border
                        // color in blue.
                        1L -> Color.Blue to 1f
                        // 2 compositions is _probably_ okay.
                        2L -> Color.Green to 2.dp.toPx()
                        // 3 or more compositions before timeout may indicate an issue. lerp the
                        // color from yellow to red, and continually increase the border size.
                        else -> {
                            lerp(
                                Color.Yellow.copy(alpha = 0.8f),
                                Color.Red.copy(alpha = 0.5f),
                                min(1f, (numCompositionsSinceTimeout - 1).toFloat() / 100f)
                            ) to numCompositionsSinceTimeout.toInt().dp.toPx()
                        }
                    }

                val halfStroke = strokeWidthPx / 2
                val topLeft = Offset(halfStroke, halfStroke)
                val borderSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

                val fillArea = (strokeWidthPx * 2) > size.minDimension
                val rectTopLeft = if (fillArea) Offset.Zero else topLeft
                val size = if (fillArea) size else borderSize
                val style = if (fillArea) Fill else Stroke(strokeWidthPx)

                drawRect(
                    brush = SolidColor(color),
                    topLeft = rectTopLeft,
                    size = size,
                    style = style
                )
            }
        }
	}

fun getInnerSquareSizeOfCircle(radius: Float, corner_percent: Int): Float {
	val C = 1.0 - (corner_percent * 0.02)
	val E = (sqrt(8.0 * radius * radius) / 2.0) - radius
	val I = radius + (E * C)
	return sqrt(I * I * 0.5).toFloat()
}

@Suppress("UsePropertyAccessSyntax")
fun Throwable.createNotification(context: Context, notification_channel: String): Notification {
	return Notification.Builder(context, notification_channel)
		.setSmallIcon(android.R.drawable.stat_notify_error)
		.setContentTitle(this::class.simpleName)
		.setContentText(message)
		.setStyle(Notification.BigTextStyle().bigText("$message\nStack trace:\n${stackTraceToString()}"))
		.addAction(Notification.Action.Builder(
			Icon.createWithResource(context, android.R.drawable.ic_menu_share),
			"Share",
			PendingIntent.getActivity(
				context,
				0,
				Intent.createChooser(Intent().also { share ->
					share.action = Intent.ACTION_SEND
					share.putExtra(Intent.EXTRA_TITLE, this::class.simpleName)
					share.putExtra(Intent.EXTRA_TITLE, this::class.simpleName)
					share.putExtra(Intent.EXTRA_TEXT, stackTraceToString())
					share.type = "text/plain"
				}, null),
				PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
			)
		).build())
		.build()
}

fun lazyAssert(message: String? = "Assertion failed", conditionProvider: () -> Boolean) {
	if (Object::class.java.desiredAssertionStatus() && !conditionProvider()) {
		throw AssertionError(message)
	}
}

fun mainThread(block: () -> Unit) {
	val main_looper = Looper.getMainLooper()
	if (main_looper.thread == Thread.currentThread()) {
		block()
		return
	}
	Handler(main_looper).post(block)
}

fun networkThread(block: () -> Unit) {
	if (Looper.getMainLooper().thread != Thread.currentThread()) {
		block()
		return
	}
	thread(block = block)
}

fun printJson(data: String, klaxon: Klaxon? = null) {
	try {
		println((klaxon ?: Klaxon()).parseJsonObject(data.reader()).toJsonString(true))
	}
	catch (e: Exception) {
		println(data.substring(1790))
		throw e
	}
}

@Composable
fun SubtleLoadingIndicator(colour: Color, modifier: Modifier = Modifier) {
	val inf_transition = rememberInfiniteTransition()
	val anim by inf_transition.animateFloat(
		initialValue = 0f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(1500, easing = LinearOutSlowInEasing),
			repeatMode = RepeatMode.Restart
		)
	)

	val rand_offset = remember { Random.nextFloat() }

	Box(modifier, contentAlignment = Alignment.Center) {
		val current_anim = if (anim + rand_offset > 1f) anim + rand_offset - 1f else anim
		val size = if (current_anim < 0.5f) current_anim else 1f - current_anim
		Spacer(
			Modifier
				.background(colour, CircleShape)
				.size(20.dp * size)
		)
	}
}

@Composable
fun ButtonColors.toIconButtonColours(): IconButtonColors {
	return IconButtonDefaults.iconButtonColors(
		containerColor = containerColor(true).value,
		contentColor = contentColor(true).value,
		disabledContainerColor = containerColor(false).value,
		disabledContentColor = contentColor(false).value
	)
}

@Composable
fun RowOrColumn(
	row: Boolean,
	modifier: Modifier = Modifier,
	arrangement: Arrangement.HorizontalOrVertical = Arrangement.SpaceEvenly,
	content: @Composable (getWeightModifier: (Float) -> Modifier) -> Unit
) {
	if (row) {
		Row(modifier, horizontalArrangement = arrangement, verticalAlignment = Alignment.CenterVertically) { content { Modifier.weight(it) } }
	}
	else {
		Column(modifier, verticalArrangement = arrangement, horizontalAlignment = Alignment.CenterHorizontally) { content { Modifier.weight(it) } }
	}
}

@Composable
fun PaddingValues.copy(
	start: Dp? = null,
	top: Dp? = null,
	end: Dp? = null,
	bottom: Dp? = null
): PaddingValues {
	return PaddingValues(
		start ?: calculateStartPadding(LocalLayoutDirection.current),
		top ?: calculateTopPadding(),
		end ?: calculateEndPadding(LocalLayoutDirection.current),
		bottom ?: calculateBottomPadding()
	)
}

fun Modifier.crossOut(
	crossed_out: Boolean,
	colour: Color,
	width: Float = Stroke.HairlineWidth,
	getSize: ((IntSize) -> IntSize)? = null
): Modifier = composed {
	val line_visibility = remember { Animatable(crossed_out.toFloat()) }
	OnChangedEffect(crossed_out) {
		line_visibility.animateTo(crossed_out.toFloat())
	}

	var size by remember { mutableStateOf(IntSize.Zero) }
	var actual_size by remember { mutableStateOf(IntSize.Zero) }

	this
		.onSizeChanged {
			size = getSize?.invoke(it) ?: it
			actual_size = it
		}
		.drawBehind {
			val offset = Offset((actual_size.width - size.width) * 0.5f, (actual_size.height - size.height) * 0.5f)

			drawLine(
				colour,
				offset,
				Offset(
					size.width * line_visibility.value + offset.x,
					size.height * line_visibility.value + offset.y
				),
				width
			)
		}
}

@Composable
fun RecomposeOnInterval(interval_ms: Long, enabled: Boolean = true, content: @Composable (Boolean) -> Unit) {
    var recomposition_state by remember { mutableStateOf(false) }

	LaunchedEffect(enabled) {
		if (enabled) {
			while (true) {
				delay(interval_ms)
				recomposition_state = !recomposition_state
			}
		}
		else {
			recomposition_state = !recomposition_state
		}
    }

	content(recomposition_state)
}

fun <T> MutableList<T>.addUnique(item: T): Boolean {
	if (!contains(item)) {
		add(item)
		return true
	}
	return false
}
