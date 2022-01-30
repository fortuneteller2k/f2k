package commands.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import java.util.concurrent.Executors

interface ClientCommand {
    val executor: CoroutineScope
        get() = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    suspend fun initialize(event: ReadyEvent): CommandData? = null
    suspend fun execute(event: SlashCommandInteractionEvent) {}
    suspend fun handleButtons(event: ButtonInteractionEvent) {}
}