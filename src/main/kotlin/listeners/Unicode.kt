package listeners

import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.*

@Suppress("unused")
class Unicode: ListenerAdapter() {
    private val log by SLF4J

    override fun onReady(event: ReadyEvent) {
        log.info("/unicode loaded")

        event.jda.upsertCommand(
            "unicode",
            """
                Get unicode code point of characters in a string. 
                Maximum of 10 characters.
            """.trimIndent()
        ) {
            option<String>("string", "Input string", true)
        }.queue()
    }

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "unicode" || event.isAcknowledged) return

        event.getOption("string")?.let { opt ->
            event.reply("${StringJoiner("\n").apply {
                for (i in opt.asString.indices) add(opt.asString.toCharArray()[i].let {
                    "`${String.format("\\u%04x", it.code)}` ${Character.getName(it.code)} [$it]"
                })
            }}").queue()
        }
    }
}