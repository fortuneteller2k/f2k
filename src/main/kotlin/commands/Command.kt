package commands
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent

interface Command {
    suspend fun initialize(event: ReadyEvent)
    suspend fun execute(event: SlashCommandEvent)
}