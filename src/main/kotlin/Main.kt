
import commands.*
import commands.wishs.Wish
import commands.wishs.WishHistory
import dev.minn.jda.ktx.light
import dev.minn.jda.ktx.listener
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes

suspend fun main(): Unit = runBlocking {
    val env = dotenv {
        directory = "./src/main/resources"
        filename = "env"
    }

    light(token = env["TOKEN"], intents = emptyList(), enableCoroutines = true) {
        setActivity(Activity.watching("for SlashCommandEvents"))
        setBulkDeleteSplittingEnabled(false)
        setLargeThreshold(50)
    }.also { it.listenForCommands() }
}

suspend fun JDA.listenForCommands() {
    val log = LoggerFactory.getLogger(this::class.java)
    val commands = listOf(About(), Help(), Latex(), Ping(), Unicode(), Wish(), WishHistory())

    listener<ReadyEvent> {
        log.info("Initializing commands...")
        commands.forEach { command -> command.initialize(it) }
    }

    listener<SlashCommandEvent> {
        if (it.isAcknowledged) return@listener

        when (it.name) {
            "about" -> commands[0].execute(it)
            "help" -> commands[1].execute(it)
            "latex" -> commands[2].execute(it)
            "ping" -> commands[3].execute(it)
            "unicode" -> commands[4].execute(it)
            "wish" -> {
                when (it.subcommandName) {
                    "get", "set" -> commands[5].execute(it)
                    "history" -> commands[6].execute(it)
                }
            }
        }
    }

    listener<ButtonClickEvent>(timeout = 1.minutes) {
        if (it.isAcknowledged) return@listener

        commands.forEach { command -> command.handleButtons(it) }
    }
}