package commands.fates.api

import com.fasterxml.jackson.annotation.JsonProperty
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
    lateinit var list: List<Wish>
    lateinit var region: String
}

class Wish {
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

class HistoryRequest(
    var authKey: String = "",
    var bannerType: Int = 301,
    var currentPage: Int = 0,
    var lastWishId: String = "0"
) {
    lateinit var instance: HistoryRequest
}