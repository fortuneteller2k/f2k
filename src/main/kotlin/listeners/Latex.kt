package listeners

import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.apache.commons.lang3.RandomStringUtils
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import java.awt.Color
import java.awt.Insets
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JLabel

class Latex: ListenerAdapter() {
    private val log by SLF4J

    override fun onReady(event: ReadyEvent) {
        log.info("/latex loaded")

        event.jda.upsertCommand("latex", "Render LaTeX expression.") {
            option<String>("expression", "LaTeX expression to render", true)
            option<Float>("size", "Image size")
        }.queue()
    }

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "latex") return

        event.deferReply(false).queue()

        event.getOption("expression")?.let {
            // Check if `expression` is wrapped in backticks, if so, strip them.
            val latex = when (it.asString.first() == '`' || it.asString.last() == '`') {
                true -> it.asString.drop(1).dropLast(1)
                else -> it.asString
            }

            val file = File("./src/main/resources/${RandomStringUtils.randomAlphabetic(7)}.png")

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

            ImageIO.write(image, "png", file)

            event.hook.editOriginal("Rendered: `$latex` with size `$imageSize`")
                .addFile(file)
                .queue { file.delete() }
        }
    }
}