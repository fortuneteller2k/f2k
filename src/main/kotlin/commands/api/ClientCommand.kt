package commands.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.util.concurrent.Executors

interface ClientCommand {
    val executor: CoroutineScope
        get() = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    suspend fun initialize(event: ReadyEvent) {}
    suspend fun execute(event: SlashCommandInteractionEvent) {}
    suspend fun handleButtons(event: ButtonInteractionEvent) {}
}