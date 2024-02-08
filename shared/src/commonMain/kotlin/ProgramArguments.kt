data class ProgramArguments(
    val bin_dir: String? = null,
    val no_auto_server: Boolean = false
) {
    companion object {
        fun parse(args: Array<String>): ProgramArguments? {
            fun onIllegalArgument(msg: String) {
                throw IllegalArgumentException("$msg Received arguments: ${args.toList()}")
            }
            
            var arguments: ProgramArguments = ProgramArguments()
            
            val iterator: Iterator<String> = args.iterator()
            while (iterator.hasNext()) {
                val split: List<String> = iterator.next().split('=', limit = 2)
                val name: String = split.first()
                val value: String? = split.getOrNull(1)
                
                when (name) {
                    "--bin-dir" -> {
                        if (value == null && !iterator.hasNext()) {
                            onIllegalArgument("No value passed for argument '$name'.")
                        }
                        arguments = arguments.copy(bin_dir = value ?: iterator.next())
                    }
                    "--disable-auto-server", "-ds" -> {
                        arguments = arguments.copy(no_auto_server = true)
                    }
                    "--help", "-h" -> {
                        outputHelpMessage()
                        return null
                    }
                    else -> onIllegalArgument("Unknown argument '$name'.")
                }
            }
            
            return arguments
        }
        
        fun outputHelpMessage() {
            println("TODO")
        }
    }
}
