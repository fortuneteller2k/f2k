package listeners

import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

@Suppress("unused")
class Help: ListenerAdapter() {
    private val log by SLF4J

    override fun onReady(event: ReadyEvent) {
        log.info("/help loaded")

        event.jda.upsertCommand("help", "Help for other slash commands.") {
            option<String>("command", "Command to get help information from.", true)
        }.queue()
    }

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "help" || event.isAcknowledged) return

        event.getOption("command")?.let {
            when (it.asString) {
                "latex" -> Latex.describe(event)
                else -> event.reply("literally don't know, don't care").queue()
            }
        }
    }
}