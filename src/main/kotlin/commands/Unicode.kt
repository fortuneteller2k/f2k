package commands

import commands.api.ClientCommand
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import java.util.*

class Unicode : ClientCommand {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent): CommandData {
        log.info("/unicode loaded")

        return Commands.slash(
            "unicode",
            """
                Get unicode code point of characters in a string. 
                Maximum of 10 characters.
            """.trimIndent()
        ).addOption(OptionType.STRING, "string", "Input string", true)
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
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