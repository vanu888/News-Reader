package com.newsreader.app.data.rss

import com.newsreader.app.data.db.entity.ArticleEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class RssParser(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()
) {

    data class RssResult(
        val title: String,
        val description: String?,
        val imageUrl: String?,
        val articles: List<RssArticle>
    )

    data class RssArticle(
        val title: String,
        val link: String,
        val description: String?,
        val content: String?,
        val imageUrl: String?,
        val pubDate: Long?,
        val author: String?,
        val guid: String?
    )

    fun fetchAndParse(url: String): RssResult {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RssException("HTTP ${response.code}: ${response.message}")
        }
        val body = response.body?.string() ?: throw RssException("Empty response body")
        return parseXml(body)
    }

    private fun parseXml(xml: String): RssResult {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var title = ""
        var description: String? = null
        var imageUrl: String? = null
        val articles = mutableListOf<RssArticle>()

        var currentTag: String? = null
        var currentTitle: String? = null
        var currentLink: String? = null
        var currentDescription: String? = null
        var currentContent: String? = null
        var currentImageUrl: String? = null
        var currentPubDate: String? = null
        var currentAuthor: String? = null
        var currentGuid: String? = null
        var inItem = false
        var inImage = false
        var inChannel = false

        val dateFormats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.US)
        )

        fun parseDate(dateStr: String): Long? {
            for (fmt in dateFormats) {
                try {
                    return fmt.parse(dateStr.trim())?.time
                } catch (_: Exception) {}
            }
            return try {
                dateStr.trim().toLong()
            } catch (_: Exception) {
                null
            }
        }

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.lowercase()
                    when (currentTag) {
                        "channel" -> inChannel = true
                        "item" -> {
                            inItem = true
                            currentTitle = null
                            currentLink = null
                            currentDescription = null
                            currentContent = null
                            currentImageUrl = null
                            currentPubDate = null
                            currentAuthor = null
                            currentGuid = null
                        }
                        "image" -> if (!inItem) inImage = true
                        "link" -> {
                            // Atom feeds use href attribute
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null && inItem && currentLink == null) {
                                currentLink = href
                            }
                        }
                        "enclosure" -> {
                            if (inItem) {
                                val encUrl = parser.getAttributeValue(null, "url")
                                if (encUrl != null && currentImageUrl == null) {
                                    currentImageUrl = encUrl
                                }
                            }
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (inItem) {
                        when (currentTag) {
                            "title" -> if (currentTitle == null) currentTitle = text
                            "link" -> if (currentLink == null) currentLink = text
                            "description" -> if (currentDescription == null) currentDescription = text
                            "content:encoded", "content" -> if (currentContent == null) currentContent = text
                            "pubdate" -> if (currentPubDate == null) currentPubDate = text
                            "dc:creator", "author" -> if (currentAuthor == null) currentAuthor = text
                            "guid" -> if (currentGuid == null) currentGuid = text
                            "enclosure" -> { /* handled by attributes */ }
                        }
                    } else if (inImage && !inItem) {
                        when (currentTag) {
                            "url" -> if (imageUrl == null) imageUrl = text
                            "title" -> if (title.isEmpty()) title = text
                        }
                    } else if (!inItem && inChannel) {
                        when (currentTag) {
                            "title" -> if (title.isEmpty()) title = text
                            "description" -> if (description == null) description = text
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val tag = parser.name.lowercase()
                    if (tag == "item" && inItem) {
                        // Try to extract image from description if not found
                        var imgUrl = currentImageUrl
                        if (imgUrl == null && currentDescription != null) {
                            imgUrl = extractFirstImageUrl(currentDescription!!)
                        }
                        if (imgUrl == null && currentContent != null) {
                            imgUrl = extractFirstImageUrl(currentContent!!)
                        }

                        articles.add(
                            RssArticle(
                                title = currentTitle ?: "Untitled",
                                link = currentLink ?: "",
                                description = stripHtml(currentDescription),
                                content = currentContent,
                                imageUrl = imgUrl,
                                pubDate = currentPubDate?.let { parseDate(it) },
                                author = currentAuthor,
                                guid = currentGuid
                            )
                        )
                        inItem = false
                    }
                    if (tag == "image") inImage = false
                    if (tag == "channel") inChannel = false
                    currentTag = null
                }
            }
            parser.next()
        }

        // Handle RSS feed that has no <channel> tag (some feeds use <rss><item> directly)
        if (title.isEmpty() && articles.isEmpty()) {
            parser.setInput(StringReader(xml))
            var altTitle = ""
            var altDescription: String? = null
            var altImageUrl: String? = null
            val altArticles = mutableListOf<RssArticle>()
            var inAltItem = false
            var altTag: String? = null
            var aTitle: String? = null
            var aLink: String? = null
            var aDesc: String? = null
            var aImg: String? = null
            var aDate: String? = null
            var aAuthor: String? = null
            var aGuid: String? = null

            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        altTag = parser.name.lowercase()
                        when (altTag) {
                            "entry", "item" -> {
                                inAltItem = true
                                aTitle = null; aLink = null; aDesc = null
                                aImg = null; aDate = null; aAuthor = null; aGuid = null
                            }
                            "feed" -> inChannel = true
                            "link" -> {
                                val href = parser.getAttributeValue(null, "href")
                                if (href != null && inAltItem && aLink == null) {
                                    aLink = href
                                }
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val txt = parser.text?.trim() ?: ""
                        if (!inAltItem && inChannel) {
                            when (altTag) {
                                "title" -> if (altTitle.isEmpty()) altTitle = txt
                                "subtitle", "description" -> if (altDescription == null) altDescription = txt
                                "icon", "logo" -> if (altImageUrl == null) altImageUrl = txt
                            }
                        } else if (inAltItem) {
                            when (altTag) {
                                "title" -> if (aTitle == null) aTitle = txt
                                "link" -> {}
                                "summary", "description" -> if (aDesc == null) aDesc = txt
                                "content" -> if (aDesc == null) aDesc = txt
                                "published", "updated" -> if (aDate == null) aDate = txt
                                "author" -> {}
                                "name" -> if (aAuthor == null) aAuthor = txt
                                "id" -> if (aGuid == null) aGuid = txt
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val et = parser.name.lowercase()
                        if (et == "entry" || et == "item") {
                            if (aImg == null && aDesc != null) aImg = extractFirstImageUrl(aDesc!!)
                            altArticles.add(RssArticle(
                                title = aTitle ?: "Untitled",
                                link = aLink ?: "",
                                description = stripHtml(aDesc),
                                content = null,
                                imageUrl = aImg,
                                pubDate = aDate?.let { parseDate(it) },
                                author = aAuthor,
                                guid = aGuid
                            ))
                            inAltItem = false
                        }
                        if (et == "feed") inChannel = false
                        altTag = null
                    }
                }
                parser.next()
            }
            if (altTitle.isNotEmpty() || altArticles.isNotEmpty()) {
                return RssResult(
                    title = altTitle.ifEmpty { "Untitled Feed" },
                    description = altDescription,
                    imageUrl = altImageUrl,
                    articles = altArticles
                )
            }
        }

        return RssResult(
            title = title.ifEmpty { "Untitled Feed" },
            description = description,
            imageUrl = imageUrl,
            articles = articles
        )
    }

    private fun extractFirstImageUrl(html: String): String? {
        val imgRegex = Regex("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"]", RegexOption.IGNORE_CASE)
        return imgRegex.find(html)?.groupValues?.getOrNull(1)?.takeIf { it.startsWith("http") }
    }

    private fun stripHtml(html: String?): String? {
        if (html == null) return null
        return html
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(500)
    }

    class RssException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
