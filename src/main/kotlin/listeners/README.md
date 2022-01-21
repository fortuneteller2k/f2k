# Structure for Slash Commands

- Extends `ListenerAdapter`
- Implements `ListenerAdapter#onReady` and `ListenerAdapter#onSlashCommand`
- Has delegate property to SLF4J, from jda-ktx
- Optionally provides a `companion object` with method `fun describe(event: SlashCommandEvent)`

```kotlin
import dev.minn.jda.ktx.SLF4J
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class Example: ListenerAdapter() {
    private val log by SLF4J
    
    override fun onReady(event: ReadyEvent) {
        // call to JDA#upsertCommand
    }

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "<name>" || event.isAcknowledged) return

        // Optional, for long computational commands
        // event.deferReply(false).queue()
    }
    
    companion object {
        fun describe(event: SlashCommandEvent) {
            // Entry for /help
        }
    }
}
```