package commands.api

class CommandException : RuntimeException {
    val reason: String
    val opcode: Int

    constructor(reason: String, opcode: Int) : super("Command failed execution: $reason") {
        this.reason = reason
        this.opcode = opcode
    }

    constructor(reason: String, opcode: Int, cause: Throwable) : super("Command failed execution: $reason", cause) {
        this.reason = reason
        this.opcode = opcode
    }

    fun responseMessage(): String = when (OpCode.fromValue(opcode)) {
        OpCode.PASSTHROUGH -> reason
        OpCode.WISH_AUTHKEY_TIMEOUT -> "The API key has expired, `/wish set` a new one."
        OpCode.WISH_RATE_LIMIT -> "Rate-limited for excessive API calls. <@175610330217447424>"
        null -> "i broke"
    }
}

enum class OpCode(val code: Int) {
    PASSTHROUGH(1),
    WISH_AUTHKEY_TIMEOUT(-101),
    WISH_RATE_LIMIT(-110);

    companion object {
        private val map = values().associateBy(OpCode::code)
        fun fromValue(code: Int) = map[code]
    }
}