package commands

import commands.api.ClientCommand
import commands.wishes.Wish
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.choice
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class Help : ClientCommand {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent) {
        event.jda.upsertCommand("help", "Help for other slash commands.") {
            option<String>("command", "Command to get help for") {
                choice("LaTeX (/latex)", "latex")
                choice("Genshin Wish History (/wish)", "wish")
            }
        }.await()

        log.info("/help loaded")
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.getOption("command")!!.asString.let {
            when (it) {
                "latex" -> Latex.describe(event)
                "wish" -> Wish.describe(event)
            }
        }
    }
}