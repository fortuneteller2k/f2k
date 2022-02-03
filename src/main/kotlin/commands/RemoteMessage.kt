package commands

import commands.api.ClientCommand
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.choice
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class RemoteMessage : ClientCommand {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent) {
        event.jda.upsertCommand("remote", "Remote messaging.") {
            option<String>("id", "ID of destination", true)

            option<String>("type", "Type of destination", true) {
                choice("User", "user")
                choice("TextChannel", "channel")
            }

            option<String>("message", "Message to send", true)
        }.await()

        log.info("/remote loaded")
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        if (event.user.id != "175610330217447424") {
            event.hook.editOriginal("imma pretend na wala kay gi ingon").await()
            return
        }

        val id = event.getOption("id")!!.asString
        val type = event.getOption("type")!!.asString
        val message = event.getOption("message")!!.asString

        when (type) {
            "user" -> {
                event.jda.openPrivateChannelById(id).flatMap { it.sendMessage(message) }.await()
                event.hook.editOriginal("let's go").await()
            }
            "channel" -> if (event.channel.id == id) {
                event.hook.editOriginal(message).await()
            } else {
                val channel = event.jda.getGuildChannelById(id)

                if (!channel?.guild?.selfMember?.hasPermission(channel, Permission.MESSAGE_SEND)!!) {
                    event.hook.editOriginal("nah mate").await()
                    return
                } else {
                    (channel as TextChannel).sendMessage(message).await()
                    event.hook.editOriginal("let's go").await()
                }
            }

        }
    }
}