package commands

import commands.api.ClientCommand
import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.time.Instant

class About : ClientCommand {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent) {
        event.jda.upsertCommand("about", "Meta description.").await()
        log.info("/about loaded")
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.hook.editOriginalEmbeds(
            Embed {
                title = "f2k"
                description = "literally <@175610330217447424> pero discord bot (slash commands only)"
                color = 0x2921ff
                timestamp = Instant.now()
                thumbnail = event.jda.selfUser.avatarUrl

                field {
                    name = "github"
                    value = "https://github.com/fortuneteller2k/f2k"
                    inline = true
                }

                field {
                    name = "JDA version"
                    value = JDA::class.java.`package`.implementationVersion!!
                    inline = true
                }
            }
        ).await()
    }
}