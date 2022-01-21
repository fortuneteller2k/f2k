package listeners

import dev.minn.jda.ktx.SLF4J
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

@Suppress("unused")
class Ping: ListenerAdapter() {
    private val log by SLF4J

    override fun onReady(event: ReadyEvent) {
        log.info("/ping loaded")
        event.jda.upsertCommand("ping", "Display REST API and WebSocket ping.").queue()
    }

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "ping" || event.isAcknowledged) return

        event.jda.restPing.queue {
            event.reply("REST API: $it ms\nWebSocket: ${event.jda.gatewayPing} ms").queue()
        }
    }
}