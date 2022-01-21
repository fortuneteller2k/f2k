import dev.minn.jda.ktx.light
import io.github.cdimascio.dotenv.dotenv
import listeners.About
import listeners.Help
import listeners.Latex

fun main() {
    val env = dotenv {
        directory = "./src/main/resources"
        filename = "env"
    }

    light(token = env["TOKEN"], intents = emptyList(), enableCoroutines = false) {
        addEventListeners(About(), Help(), Latex())
        setBulkDeleteSplittingEnabled(false)
        setLargeThreshold(50)
    }.also { it.awaitReady() }
}