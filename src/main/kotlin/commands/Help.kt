package commands

import commands.wishs.Wish
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.subcommand
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent

class Help: Command {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent) {
        log.info("/help loaded")

        event.jda.upsertCommand("help", "Help for other slash commands.") {
            subcommand("latex", "Display help for /latex")
            subcommand("wish", "Display help for /wish")
        }.await()
    }

    override suspend fun execute(event: SlashCommandEvent) {
        when (event.subcommandName) {
            "latex" -> Latex.describe(event)
            "wish" -> Wish.describe(event)
        }
    }
}