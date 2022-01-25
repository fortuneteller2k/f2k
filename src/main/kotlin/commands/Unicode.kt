package commands

import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import java.util.*

class Unicode: Command {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent) {
        log.info("/unicode loaded")

        event.jda.upsertCommand(
            "unicode",
            """
                Get unicode code point of characters in a string. 
                Maximum of 10 characters.
            """.trimIndent()
        ) {
            option<String>("string", "Input string", true)
        }.await()
    }

    override suspend fun execute(event: SlashCommandEvent) {
        event.getOption("string")?.let { option ->
            val input = option.asString

            event.reply("${StringJoiner("\n").apply {
                input.indices.forEach { i ->
                    add(input.toCharArray()[i].let {
                        "`${String.format("\\u%04x", it.code)}` ${Character.getName(it.code)} [$it]"
                    })
                }
            }}").await()
        }
    }
}