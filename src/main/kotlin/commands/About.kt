package commands

import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import java.time.Instant

class About: Command {
    private val log by SLF4J

    override suspend fun initialize(event: ReadyEvent) {
        log.info("/about loaded")
        event.jda.upsertCommand("about", "Meta description.").await()
    }

    override suspend fun execute(event: SlashCommandEvent) {
        event.replyEmbeds(
            Embed {
                title = "f2k"
                description = "literally <@175610330217447424> pero discord bot (slash commands only)"
                color = 0x2921ff
                timestamp = Instant.now()
                thumbnail = event.jda.selfUser.avatarUrl

                field {
                    name = "github"
                    value = "https://github.com/fortuneteller2k/f2k"
                    inline = false
                }
            }
        ).await()
    }
}