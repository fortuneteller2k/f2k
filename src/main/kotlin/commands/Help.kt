package commands

import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent

class Help: Command {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent) {
        log.info("/help loaded")

        event.jda.upsertCommand("help", "Help for other slash commands.") {
            option<String>("command", "listeners.Command to get help information from.", true)
        }.await()
    }

    override suspend fun execute(event: SlashCommandEvent) {
        event.getOption("command")?.let {
            when (it.asString) {
                "latex" -> Latex.describe(event)
                else -> event.reply("literally don't know, don't care").await()
            }
        }
    }
}