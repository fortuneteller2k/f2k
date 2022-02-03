
import commands.*
import commands.wishes.Wish
import commands.wishes.WishHistory
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.light
import dev.minn.jda.ktx.listener
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.ResumedEvent
import net.dv8tion.jda.api.events.ShutdownEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

suspend fun main(): Unit = runBlocking {
    val log = LoggerFactory.getLogger(this::class.java)

    val env = dotenv {
        directory = "./src/main/resources"
        filename = "env"
    }

    log.info("Starting SQLite database...")

    Database.connect("jdbc:sqlite:src/main/resources/f2k.sqlite", "org.sqlite.JDBC", "f2k")

    light(token = env["TOKEN"], intents = emptyList(), enableCoroutines = true) {
        setActivity(Activity.watching("for SlashCommandEvents"))
        setBulkDeleteSplittingEnabled(false)
        setLargeThreshold(50)
    }.also { it.initialize() }
}

suspend fun JDA.initialize() {
    val log = LoggerFactory.getLogger(this::class.java)
    val commands = listOf(About(), Help(), Latex(), Ping(), RemoteMessage(), Unicode(), Wish(), WishHistory())
    var commandsInitialized = false

    listener<ReadyEvent> {
        try {
            // Initialize commands for the first ReadyEvent, don't re-initialize after reconnecting
            if (!commandsInitialized) {
                log.info("Initializing commands...")
                commands.forEach { command -> command.initialize(it) }

                log.info("Finished loading commands.")
                commandsInitialized = true
            }
        } catch (e: ErrorResponseException) {
            log.error(e.meaning)
            log.error("HTTP: ${e.response.code}, JSON: ${e.errorCode}")
            exitProcess(if (e.errorCode == 0) e.response.code else e.errorCode)
        }
    }

    listener<ResumedEvent> {
        log.info("Resumed session.")
    }

    listener<ShutdownEvent> {
        log.info("Shutting down...")
        exitProcess(it.code)
    }

    listener<SlashCommandInteractionEvent> {
        if (it.isAcknowledged) return@listener
        it.deferReply(false).await()

        when (it.name) {
            "about" -> commands[0].execute(it)
            "help" -> commands[1].execute(it)
            "latex" -> commands[2].execute(it)
            "ping" -> commands[3].execute(it)
            "remote" -> commands[4].execute(it)
            "unicode" -> commands[5].execute(it)
            "wish" -> {
                when (it.subcommandName) {
                    "get", "set", "invalidate" -> commands[6].execute(it)
                    "history" -> commands[7].execute(it)
                }
            }
        }
    }

    listener<ButtonInteractionEvent> {
        if (it.isAcknowledged) return@listener
        it.deferEdit().await()
        commands.forEach { command -> command.handleButtons(it) }
    }
}