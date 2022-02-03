package commands

import commands.api.ClientCommand
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.util.*

class Unicode : ClientCommand {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent) {
        event.jda.upsertCommand(
            "unicode",
            "Get unicode code point of characters in a string. Maximum of 10 characters."
        ) {
            option<String>("string", "Input string", true)
        }.await()

        log.info("/unicode loaded")
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.getOption("string")?.let { option ->
            val input = option.asString

            event.hook.editOriginal("${StringJoiner("\n").apply {
                input.indices.forEach { i ->
                    add(input.toCharArray()[i].let {
                        "`${String.format("\\u%04x", it.code)}` ${Character.getName(it.code)} [$it]"
                    })
                }
            }}").await()
        }
    }
}