package commands

import commands.api.ClientCommand
import commands.wishes.Wish
import dev.minn.jda.ktx.SLF4J
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData

class Help : ClientCommand {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent): CommandData {
        log.info("/help loaded")

        return Commands.slash("help", "Help for other slash commands.")
            .addOptions(
                OptionData(OptionType.STRING, "command", "Command to get help for")
                    .addChoices(
                        Command.Choice("LaTeX (/latex)", "latex"),
                        Command.Choice("Genshin Wish History", "wish")
                    )
            )
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