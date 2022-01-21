
import dev.minn.jda.ktx.light
import io.github.cdimascio.dotenv.dotenv
import io.github.classgraph.ClassGraph
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.*

fun main() {
    val env = dotenv {
        directory = "./src/main/resources"
        filename = "env"
    }

    val listeners = LinkedList<ListenerAdapter>().apply {
        // Scan for ListenerAdapters in the `listener` package
        val listenerClasses = ClassGraph()
            .acceptPackages("listeners")
            .scan()
            .getSubclasses(ListenerAdapter::class.qualifiedName)
            .loadClasses(ListenerAdapter::class.java)

        for (l in listenerClasses) {
            // Construct a new instance, then add it in the list
            add(l.getDeclaredConstructor().newInstance())
        }
    }.toArray() // Then convert the list into an Array, so we can spread it

    light(token = env["TOKEN"], intents = emptyList(), enableCoroutines = false) {
        addEventListeners(*listeners)
        setBulkDeleteSplittingEnabled(false)
        setLargeThreshold(50)
    }.also { it.awaitReady() }
}