import kotlin.contracts.*

@OptIn(kotlin.contracts.ExperimentalContracts::class)
actual inline fun <R> synchronizedImpl(lock: Any, block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}
