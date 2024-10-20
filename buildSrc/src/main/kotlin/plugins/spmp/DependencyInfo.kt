package plugin.spmp

data class DependencyInfo(
    val version: String?,
    val name: String,
    val author: String,
    val url: String,
    val license: String,
    val license_url: String,
    val fork_url: String? = null,
    val redirect: String? = null
)
