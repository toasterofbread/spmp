import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher

actual val Dispatchers.PlatformIO: CoroutineDispatcher
    get() = Dispatchers.IO
