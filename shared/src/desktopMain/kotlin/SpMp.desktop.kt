import org.jetbrains.skiko.hostOs
import org.jetbrains.skiko.OS

actual fun isWindowTransparencySupported(): Boolean =
    when (hostOs) {
        OS.Linux -> true
        else -> false
    }
