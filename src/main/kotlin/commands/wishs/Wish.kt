package commands.wishs

import commands.Command
import commands.wishs.api.WishTable
import commands.wishs.api.WishTable.authKey
import commands.wishs.api.WishTable.userId
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.subcommand
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL

class Wish: Command {
    private val log by SLF4J

    companion object {
        internal val entries: MutableMap<String, String> = HashMap()

        suspend fun describe(event: SlashCommandEvent) {
            event.reply("todo").await()
        }
    }

    override suspend fun initialize(event: ReadyEvent) {
        log.info("/wish loaded")

        event.jda.upsertCommand("wish", "Wish history related commands.") {
            subcommand("get", "Retrieve API key.")

            subcommand("set", "Save API key from URL.") {
                option<String>("url", "URL containing API key", true)
            }

            subcommand("history", "Retrieve wish history from API.") {
                option<String>("banner", "Banner to retrieve history from", true)
            }
        }.await()

        Database.connect("jdbc:sqlite:src/main/resources/f2k.sqlite", "org.sqlite.JDBC", "f2k")

        transaction {
            SchemaUtils.createMissingTablesAndColumns(WishTable)
            WishTable.selectAll().forEach { entries[it[userId]] = it[authKey] }
        }
    }

    override suspend fun execute(event: SlashCommandEvent) {
        event.deferReply(false).await()

        Database.connect("jdbc:sqlite:src/main/resources/f2k.sqlite", "org.sqlite.JDBC", "f2k")

        when (event.subcommandName) {
            "get" -> {
                val userKey = entries[event.user.id]

                if (userKey == null)
                    event.hook.editOriginal("No key found in database.").await()
                else
                    event.hook.editOriginal(userKey).await()
            }
            "set" -> {
                println(event.options.toString())

                val key = HashMap<String, String>().apply {
                    event.getOption("url")?.let {
                        URL(it.asString).query.split("&").forEach { param ->
                            put(param.split("=")[0], param.split("=")[1])
                        }
                    }
                }["authkey"]!!

                if (entries[event.user.id] != null) {
                    entries[event.user.id] = key

                    transaction {
                        WishTable.update({ userId eq event.user.id }) {
                            it[authKey] = key
                        }
                    }

                    event.hook.editOriginal("Key updated successfully.").await()
                } else {
                    entries[event.user.id] = key

                    transaction {
                        WishTable.insert {
                            it[userId] = event.user.id
                            it[authKey] = key
                        }
                    }

                    event.hook.editOriginal("Key stored successfully.").await()
                }
            }
        }
    }
}