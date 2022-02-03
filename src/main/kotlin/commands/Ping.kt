package commands

import commands.api.ClientCommand
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class Ping : ClientCommand {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent) {
        event.jda.upsertCommand("ping", "Display REST API and WebSocket ping.").await()
        log.info("/ping loaded")
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.hook.editOriginal("API: ${event.jda.restPing.await()} ms\nWebSocket: ${event.jda.gatewayPing} ms").await()
    }
}