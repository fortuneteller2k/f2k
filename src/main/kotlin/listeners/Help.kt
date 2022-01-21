package listeners

import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.time.Instant

class Help: ListenerAdapter() {
    private val log by SLF4J

    override fun onReady(event: ReadyEvent) {
        log.info("/help loaded")

        event.jda.upsertCommand("help", "Help for other slash commands.") {
            option<String>("command", "Command to get help information from.", true)
        }.queue()
    }

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "help") return

        event.getOption("command")?.let {
            when (it.asString) {
                "latex" -> {
                    event.replyEmbeds(
                        Embed {
                            title = "/latex [expression] [size?]"
                            description = "Render LaTeX expressions."
                            color = 0x000000
                            timestamp = Instant.now()
                            thumbnail = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/LaTeX_logo.svg/1280px-LaTeX_logo.svg.png"

                            field {
                                name = "Example"
                                value = """
                                        `/latex expression: \frac{x}{y}`
                                        `/latex expression: \lim_{x \to 0} f(x) size: 60`
                                        """.trimIndent()
                                inline = true
                            }

                            field {
                                name = "LaTeX resources"
                                value = """
                                        [LaTeX/Mathematics (Wikibooks)](https://en.wikibooks.org/wiki/LaTeX/Mathematics)
                                        [List of LaTeX mathematical symbols](https://oeis.org/wiki/List_of_LaTeX_mathematical_symbols)
                                        [Subscripts and superscripts](https://www.overleaf.com/learn/latex/Subscripts_and_superscripts)
                                        [Spacing in math mode](https://www.overleaf.com/learn/latex/Spacing_in_math_mode)
                                    """.trimIndent()
                                inline = false
                            }
                        }
                    ).queue()
                }
                else -> event.reply("literally don't know").queue()
            }
        }
    }
}