package commands

import commands.api.Command
import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import java.awt.Color
import java.awt.Insets
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.imageio.ImageIO
import javax.swing.JLabel


class Latex : Command {
    private val log by SLF4J

    companion object {
        suspend fun describe(event: SlashCommandEvent) {
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
                        inline = false
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
            ).await()
        }
    }

    override suspend fun initialize(event: ReadyEvent) {
        log.info("/latex loaded")

        event.jda.upsertCommand("latex", "Render LaTeX expression.") {
            option<String>("expression", "LaTeX expression to render", true)
            option<Float>("size", "Image size")
        }.await()
    }

    override suspend fun execute(event: SlashCommandEvent) {
        event.deferReply(false).await()

        event.getOption("expression")?.let {
            // Check if `expression` is wrapped in backticks, if so, strip them.
            val latex = when (it.asString.first() == '`' && it.asString.last() == '`') {
                true -> it.asString.drop(1).dropLast(1)
                else -> it.asString
            }

            val imageSize = when {
                event.getOption("size") == null -> 50.0
                else -> event.getOption("size")!!.asDouble
            }

            val icon = TeXFormula(latex).createTeXIcon(TeXConstants.STYLE_DISPLAY, imageSize.toFloat())
            icon.insets = Insets(5, 5, 5, 5)

            val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)

            val g2d = image.createGraphics()
            g2d.color = Color.white
            g2d.fillRect(0, 0, icon.iconWidth, icon.iconHeight)

            val label = JLabel()

            label.foreground = Color(0, 0, 0)
            icon.paintIcon(label, g2d, 0, 0)

            val byteStream = ByteArrayOutputStream()
            val writer = ImageIO.getImageWritersByFormatName("png").next()
            val imageStream = ImageIO.createImageOutputStream(byteStream)

            writer.output = imageStream
            writer.write(image)

            imageStream.close()

            event.hook
                .editOriginal("Rendered: `$latex` with size `$imageSize`")
                .addFile(byteStream.toByteArray(), "output.png")
                .await()
        }
    }
}