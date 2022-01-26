package commands.wishs.api

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.sql.Table
import kotlin.properties.Delegates

class History {
    var retcode by Delegates.notNull<Int>()
    lateinit var message: String
    lateinit var data: WishData
}

class WishData {
    lateinit var page: String
    lateinit var size: String
    lateinit var total: String
    lateinit var list: List<WishInstance>
    lateinit var region: String
}

class WishInstance {
    lateinit var uid: String
    @JsonProperty("gacha_type") lateinit var gachaType: String
    @JsonProperty("item_id") lateinit var itemId: String
    lateinit var count: String
    lateinit var time: String
    lateinit var name: String
    lateinit var lang: String
    @JsonProperty("item_type") lateinit var itemType: String
    @JsonProperty("rank_type") lateinit var rankType: String
    lateinit var id: String
}

object WishTable: Table() {
    val userId = varchar("user_id", 18).check { it.isNotNull() }
    val authKey = varchar("authkey", 1100).check { it.isNotNull() }
}

enum class BannerType(val value: Int) {
    BEGINNER(100),
    PERMANENT(200),
    CHARACTER(301),
    WEAPON(302),
    CHARACTER2(400)
}

class HistoryRequest(
    var authKey: String,
    var bannerType: BannerType = BannerType.CHARACTER,
    var pageNumber: Int = 1,
)