package commands

import commands.api.ClientCommand
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands

class Ping : ClientCommand {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent): CommandData {
        log.info("/ping loaded")
        return Commands.slash("ping", "Display REST API and WebSocket ping.")
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply(false).await()
        event.hook.editOriginal("API: ${event.jda.restPing.await()} ms\nWebSocket: ${event.jda.gatewayPing} ms").await()
    }
}