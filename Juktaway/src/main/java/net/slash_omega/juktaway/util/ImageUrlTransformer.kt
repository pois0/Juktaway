package net.slash_omega.juktaway.util

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created on 2018/10/28.
 */
class ImageUrlTransformer(regex: String, val urlGenerator: ((String, Matcher) -> String)) {
    companion object {
        val list by lazy { listOf(
            ImageUrlTransformer("^http://twitpic\\.com/(\\w+)$") { _, m ->
                "http://twitpic.com/show/full/" + m.group(1)
            },
            ImageUrlTransformer("^https?://(?:www\\.)?instagram\\.com/p/([^/]+)/$") { url, _ -> url + "media?size=l" },
            ImageUrlTransformer("^http://photozou\\.jp/photo/show/\\d+/(\\d+)$") { _, m ->
                "http://photozou.jp/p/img/" + m.group(1)
            },
            ImageUrlTransformer("^https?://.*\\.(png|gif|jpeg|jpg)$") { url, _ -> url },
            ImageUrlTransformer("^https?://(?:www\\.youtube\\.com/watch\\?.*v=|youtu\\.be/)([\\w-]+)") { _, m ->
                "http://i.ytimg.com/vi/" + m.group(1) + "/hqdefault.jpg"
            },
            ImageUrlTransformer("^http://(?:www\\.nicovideo\\.jp/watch|nico\\.ms)/sm(\\d+)$") { _, m ->
                val id = Integer.valueOf(m.group(1))
                val host = id % 4 + 1
                "http://tn-skr$host.smilevideo.jp/smile?i=$id.L"
            },
            ImageUrlTransformer("^http://www\\.pixiv\\.net/member_illust\\.php.*illust_id=(\\d+)") { _, m ->
                "http://embed.pixiv.net/decorate.php?illust_id=" + m.group(1)
            },
            ImageUrlTransformer("^https?://gyazo\\.com/(\\w+)") { _, m -> "https://i.gyazo.com/" + m.group(1) + ".png" }
        )}
    }

    val pattern: Pattern by lazy { Pattern.compile(regex) }
}