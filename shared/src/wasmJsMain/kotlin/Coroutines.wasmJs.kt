import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlin.js.Promise

actual val Dispatchers.PlatformIO: CoroutineDispatcher
    get() = Dispatchers.Main
