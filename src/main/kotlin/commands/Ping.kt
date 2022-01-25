package commands

import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent

class Ping: Command {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent) {
        log.info("/ping loaded")
        event.jda.upsertCommand("ping", "Display REST API and WebSocket ping.").await()
    }

    override suspend fun execute(event: SlashCommandEvent) {
        event.reply("API: ${event.jda.restPing.await()} ms\nWebSocket: ${event.jda.gatewayPing} ms").await()
    }
}