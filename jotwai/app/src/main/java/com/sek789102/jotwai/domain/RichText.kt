package com.sek789102.jotwai.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em

/**
 * What-you-see-is-what-you-get formatting on top of the plain-text note body.
 *
 * The note is still stored as text (so search, backup, history, sharing and old notes keep working),
 * but the formatting markers are hidden while editing and reading:
 *  - Markdown: **bold**, *italic*, ~~strike~~, ==highlight==, `code`, # / ## / ### heading,
 *    "- " bullet, "1. " number, "[ ] " / "[x] " box, "> " quote, "---" divider line.
 *  - Tags for everything Markdown cannot express: ⟦u⟧underline⟦/u⟧, ⟦c:E53935⟧colour⟦/c⟧,
 *    ⟦h:FFEB3B⟧highlight colour⟦/h⟧, ⟦z:1.5⟧size⟦/z⟧, ⟦f:serif⟧font⟦/f⟧, ⟦sup⟧ ⟦sub⟧,
 *    and ⟦al:c⟧ / ⟦al:r⟧ / ⟦al:j⟧ at the start of a line for alignment.
 */
object RichText {
    const val OPEN = '⟦'
    const val CLOSE = '⟧'

    enum class Kind { BOLD, ITALIC, UNDERLINE, STRIKE, MARK, COLOR, BG, SIZE, FONT, SUP, SUB, CODE, H1, H2, H3, MARKER, DONE, QUOTE, RULE }

    /** A formatting range in original (stored) text coordinates. */
    data class Span(val kind: Kind, val start: Int, val end: Int, val arg: String = "")

    /** Original range [start, end) shown as [repl] ("" = hidden). Line prefixes prefer the cursor after them. */
    data class Seg(val start: Int, val end: Int, val repl: String, val preferRight: Boolean)

    /** An inline open/close marker pair; content is [openEnd, closeStart). */
    data class MarkerPair(val kind: Kind, val openStart: Int, val openEnd: Int, val closeStart: Int, val closeEnd: Int, val arg: String = "")

    class Parsed(
        val text: String,
        val segs: List<Seg>,
        val spans: List<Span>,
        val pairs: List<MarkerPair>,
        val aligns: List<Triple<Int, Int, TextAlign>>,
        val segAt: IntArray,
        val display: String,
        val o2d: IntArray,
        val d2o: IntArray
    ) {
        fun isHidden(i: Int): Boolean = i in segAt.indices && segAt[i] >= 0 && segs[segAt[i]].repl.isEmpty()
    }

    private val tagRegex = Regex("""⟦(/?)([a-z]{1,3})(?::([#0-9A-Za-z.]{1,12}))?⟧""")
    private val alignPrefix = Regex("""^(?:⟦al:[a-z]⟧)""")
    private val headingRe = Regex("""^(#{1,3}) """)
    private val bulletRe = Regex("""^(\s*)([-*•]) """)
    private val numberRe = Regex("""^(\s*)(\d{1,4}[.)]) """)
    private val boxRe = Regex("""^(\s*)\[( |x|X)\] """)
    private val quoteRe = Regex("""^(\s*)> """)
    private val italicRe = Regex("""(?<![*\w])\*(?!\*)([^*\n]+?)\*(?!\*)""")
    private const val RULE_TEXT = "────────────────────"

    private fun kindOf(tag: String): Kind? = when (tag) {
        "u" -> Kind.UNDERLINE
        "c" -> Kind.COLOR
        "h" -> Kind.BG
        "z" -> Kind.SIZE
        "f" -> Kind.FONT
        "sup" -> Kind.SUP
        "sub" -> Kind.SUB
        "b" -> Kind.BOLD
        "i" -> Kind.ITALIC
        "s" -> Kind.STRIKE
        else -> null
    }

    // Small cache: the editor parses the same text for display and for edit fixes.
    private val cache = object : LinkedHashMap<String, Parsed>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Parsed>?) = size > 6
    }

    fun parse(text: String): Parsed {
        synchronized(cache) { cache[text]?.let { return it } }
        val p = doParse(text)
        synchronized(cache) { cache[text] = p }
        return p
    }

    private fun doParse(text: String): Parsed {
        val segs = mutableListOf<Seg>()
        val spans = mutableListOf<Span>()
        val pairs = mutableListOf<MarkerPair>()
        val aligns = mutableListOf<Triple<Int, Int, TextAlign>>()

        // ---- tags ----
        data class Open(val name: String, val arg: String, val start: Int, val end: Int)
        val stack = ArrayList<Open>()
        for (m in tagRegex.findAll(text)) {
            val closing = m.groupValues[1] == "/"
            val name = m.groupValues[2]
            val arg = m.groupValues[3]
            if (name == "al") {
                val lineStart = if (m.range.first == 0) 0 else text.lastIndexOf('\n', m.range.first - 1) + 1
                if (!closing && m.range.first == lineStart) {
                    segs += Seg(m.range.first, m.range.last + 1, "", preferRight = true)
                    val lineEnd = text.indexOf('\n', m.range.last + 1).let { if (it < 0) text.length else it }
                    val align = when (arg) { "c" -> TextAlign.Center; "r" -> TextAlign.End; "j" -> TextAlign.Justify; else -> TextAlign.Start }
                    aligns += Triple(lineStart, lineEnd, align)
                }
                continue
            }
            val kind = kindOf(name) ?: continue
            segs += Seg(m.range.first, m.range.last + 1, "", preferRight = false)
            if (!closing) {
                stack += Open(name, arg, m.range.first, m.range.last + 1)
            } else {
                val idx = stack.indexOfLast { it.name == name }
                if (idx >= 0) {
                    val o = stack.removeAt(idx)
                    if (m.range.first > o.end) spans += Span(kind, o.end, m.range.first, o.arg)
                    pairs += MarkerPair(kind, o.start, o.end, m.range.first, m.range.last + 1, o.arg)
                }
            }
        }

        // ---- line prefixes ----
        var lineStart = 0
        while (lineStart <= text.length) {
            var lineEnd = text.indexOf('\n', lineStart)
            if (lineEnd < 0) lineEnd = text.length
            var cs = lineStart
            alignPrefix.find(text.substring(lineStart, lineEnd))?.let { cs = lineStart + it.value.length }
            val content = text.substring(cs, lineEnd)
            headingRe.find(content)?.let { m ->
                segs += Seg(cs, cs + m.value.length, "", preferRight = true)
                val k = when (m.groupValues[1].length) { 1 -> Kind.H1; 2 -> Kind.H2; else -> Kind.H3 }
                if (lineEnd > cs + m.value.length) spans += Span(k, cs + m.value.length, lineEnd)
            } ?: boxRe.find(content)?.let { m ->
                val bs = cs + m.groups[1]!!.value.length
                val done = m.groupValues[2].isNotBlank()
                segs += Seg(bs, bs + 3, if (done) "☑" else "☐", preferRight = true)
                spans += Span(Kind.MARKER, bs, bs + 3)
                if (done && lineEnd > cs + m.value.length) spans += Span(Kind.DONE, cs + m.value.length, lineEnd)
            } ?: bulletRe.find(content)?.let { m ->
                val bs = cs + m.groups[2]!!.range.first
                segs += Seg(bs, bs + 1, "•", preferRight = true)
                spans += Span(Kind.MARKER, bs, bs + 1)
            } ?: numberRe.find(content)?.let { m ->
                val r = m.groups[2]!!.range
                spans += Span(Kind.MARKER, cs + r.first, cs + r.last + 1)
            } ?: quoteRe.find(content)?.let { m ->
                val qs = cs + m.groups[1]!!.value.length
                segs += Seg(qs, qs + 1, "┃", preferRight = true)
                spans += Span(Kind.MARKER, qs, qs + 1)
                if (lineEnd > qs + 2) spans += Span(Kind.QUOTE, qs + 2, lineEnd)
            } ?: run {
                if (content.trim() == "---" && content.isNotEmpty()) {
                    segs += Seg(cs, lineEnd, RULE_TEXT, preferRight = false)
                    spans += Span(Kind.RULE, cs, lineEnd)
                }
            }
            if (lineEnd >= text.length) break
            lineStart = lineEnd + 1
        }

        // ---- inline Markdown ----
        fun pairMarkers(token: String, kind: Kind) {
            var from = 0
            val len = token.length
            while (true) {
                val s = text.indexOf(token, from)
                if (s < 0) break
                val e = text.indexOf(token, s + len)
                if (e < 0 || e == s + len || text.substring(s + len, e).contains('\n')) {
                    from = s + len
                    continue
                }
                segs += Seg(s, s + len, "", false)
                segs += Seg(e, e + len, "", false)
                spans += Span(kind, s + len, e)
                pairs += MarkerPair(kind, s, s + len, e, e + len)
                from = e + len
            }
        }
        pairMarkers("**", Kind.BOLD)
        pairMarkers("~~", Kind.STRIKE)
        pairMarkers("==", Kind.MARK)
        pairMarkers("`", Kind.CODE)
        italicRe.findAll(text).forEach { m ->
            val s = m.range.first
            val e = m.range.last
            segs += Seg(s, s + 1, "", false)
            segs += Seg(e, e + 1, "", false)
            spans += Span(Kind.ITALIC, s + 1, e)
            pairs += MarkerPair(Kind.ITALIC, s, s + 1, e, e + 1)
        }

        // Compose starts a new line for every paragraph style, so with alignment the '\n' characters
        // are shown as zero-width spaces (same length, so cursor positions stay one-to-one).
        if (aligns.isNotEmpty()) {
            var i = text.indexOf('\n')
            while (i >= 0) {
                segs += Seg(i, i + 1, "\u200B", preferRight = false)
                i = text.indexOf('\n', i + 1)
            }
        }

        // ---- resolve overlaps (keep the first by start) ----
        val sorted = segs.sortedWith(compareBy<Seg> { it.start }.thenByDescending { it.end })
        val clean = ArrayList<Seg>(sorted.size)
        var lastEnd = 0
        for (s in sorted) {
            if (s.start >= lastEnd && s.end > s.start) {
                clean += s
                lastEnd = s.end
            }
        }

        // ---- build display text + offset maps ----
        val n = text.length
        val segAt = IntArray(n) { -1 }
        clean.forEachIndexed { i, s -> for (k in s.start until s.end) segAt[k] = i }
        val sb = StringBuilder(n)
        val o2d = IntArray(n + 1)
        val d2oList = ArrayList<Int>(n + 1)
        fun setD2o(d: Int, o: Int, force: Boolean = false) {
            while (d2oList.size <= d) d2oList += -1
            if (force || d2oList[d] < 0) d2oList[d] = o
        }
        var o = 0
        var d = 0
        for (s in clean) {
            while (o < s.start) {
                setD2o(d, o); o2d[o] = d; sb.append(text[o]); o++; d++
            }
            val rl = s.repl.length
            if (rl == 0) {
                if (!s.preferRight) setD2o(d, s.start)
            } else {
                for (j in 0 until rl) setD2o(d + j, s.start + minOf(j, s.end - s.start))
            }
            for (k in s.start until s.end) o2d[k] = d + minOf(k - s.start, rl)
            sb.append(s.repl)
            d += rl
            o = s.end
        }
        while (o < n) {
            setD2o(d, o); o2d[o] = d; sb.append(text[o]); o++; d++
        }
        o2d[n] = d
        setD2o(d, n)
        val d2o = IntArray(d + 1) { i -> d2oList.getOrElse(i) { -1 }.let { if (it < 0) n else it } }
        return Parsed(text, clean, spans, pairs, aligns, segAt, sb.toString(), o2d, d2o)
    }

    // ---------------- display ----------------

    data class Palette(val marker: Color, val highlight: Color, val done: Color, val codeBg: Color, val quote: Color, val rule: Color)

    fun parseColor(hex: String): Color? = runCatching {
        val h = hex.removePrefix("#")
        if (h.length != 6) return null
        Color(0xFF000000 or h.toLong(16))
    }.getOrNull()

    fun fontOf(arg: String): FontFamily? = when (arg) {
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        "sans" -> FontFamily.SansSerif
        "hand" -> FontFamily.Cursive
        else -> null
    }

    fun annotate(p: Parsed, pal: Palette): AnnotatedString = buildAnnotatedString {
        append(p.display)
        val len = p.display.length
        fun range(s: Int, e: Int): IntRange? {
            val ds = p.o2d[s.coerceIn(0, p.text.length)]
            val de = p.o2d[e.coerceIn(0, p.text.length)]
            return if (de > ds) ds until de else null
        }
        if (p.aligns.isNotEmpty()) {
            // Paragraph styles must tile the whole text (each line incl. its '\n'); gaps add empty lines.
            val byLine = p.aligns.associate { it.first to it.third }
            var ls = 0
            val t = p.text
            while (ls <= t.length) {
                var le = t.indexOf('\n', ls)
                val last = le < 0
                if (last) le = t.length
                val ds = p.o2d[ls]
                val de = if (last) len else p.o2d[le] + 1
                if (de > ds) addStyle(ParagraphStyle(textAlign = byLine[ls] ?: TextAlign.Start), ds, de.coerceAtMost(len))
                if (last) break
                ls = le + 1
            }
        }
        for (s in p.spans) {
            val r = range(s.start, s.end) ?: continue
            val style = when (s.kind) {
                Kind.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                Kind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                Kind.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                Kind.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                Kind.MARK -> SpanStyle(background = pal.highlight)
                Kind.COLOR -> parseColor(s.arg)?.let { SpanStyle(color = it) }
                Kind.BG -> parseColor(s.arg)?.let { SpanStyle(background = it.copy(alpha = 0.55f)) }
                Kind.SIZE -> s.arg.toFloatOrNull()?.coerceIn(0.3f, 10f)?.let { SpanStyle(fontSize = it.em) }
                Kind.FONT -> fontOf(s.arg)?.let { SpanStyle(fontFamily = it) }
                Kind.SUP -> SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.7.em)
                Kind.SUB -> SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.7.em)
                Kind.CODE -> SpanStyle(fontFamily = FontFamily.Monospace, background = pal.codeBg)
                Kind.H1 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 1.6.em)
                Kind.H2 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 1.35.em)
                Kind.H3 -> SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 1.15.em)
                Kind.MARKER -> SpanStyle(color = pal.marker, fontWeight = FontWeight.Bold)
                Kind.DONE -> SpanStyle(color = pal.done, textDecoration = TextDecoration.LineThrough)
                Kind.QUOTE -> SpanStyle(color = pal.quote, fontStyle = FontStyle.Italic)
                Kind.RULE -> SpanStyle(color = pal.rule)
            } ?: continue
            addStyle(style, r.first, r.last + 1)
        }
    }

    fun render(text: String, pal: Palette): AnnotatedString = annotate(parse(text), pal)

    class Transformation(private val pal: Palette) : VisualTransformation {
        override fun filter(text: AnnotatedString): TransformedText {
            val p = parse(text.text)
            val o2d = p.o2d
            val d2o = p.d2o
            val mapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = o2d[offset.coerceIn(0, o2d.size - 1)]
                override fun transformedToOriginal(offset: Int): Int = d2o[offset.coerceIn(0, d2o.size - 1)]
            }
            return TransformedText(annotate(p, pal), mapping)
        }
        override fun equals(other: Any?) = other is Transformation && other.pal == pal
        override fun hashCode() = pal.hashCode()
    }

    // ---------------- editing helpers ----------------

    /** Kinds active at the cursor (for highlighting toolbar buttons). */
    fun activeKinds(v: TextFieldValue): Map<Kind, String> {
        val p = parse(v.text)
        val at = v.selection.min
        val res = HashMap<Kind, String>()
        for (s in p.spans) if ((at > s.start && at <= s.end) || (!v.selection.collapsed && at == s.start && s.end > s.start)) res[s.kind] = s.arg
        return res
    }

    fun alignAt(v: TextFieldValue): TextAlign {
        val t = v.text
        val ls = lineStartOf(t, v.selection.min)
        val m = alignPrefix.find(t.substring(ls, lineEndOf(t, ls))) ?: return TextAlign.Start
        return when (m.value[4]) { 'c' -> TextAlign.Center; 'r' -> TextAlign.End; 'j' -> TextAlign.Justify; else -> TextAlign.Start }
    }

    private fun lineStartOf(t: String, i: Int) = if (i <= 0) 0 else t.lastIndexOf('\n', i - 1) + 1
    private fun lineEndOf(t: String, i: Int) = t.indexOf('\n', i).let { if (it < 0) t.length else it }

    /** Moves selection edges off hidden markers so "**bold**" selections become "bold". */
    fun normalize(v: TextFieldValue): TextFieldValue {
        if (v.selection.collapsed) return v
        val p = parse(v.text)
        var s = v.selection.min
        var e = v.selection.max
        while (s < e && p.isHidden(s)) s++
        while (e > s && p.isHidden(e - 1)) e--
        return v.copy(selection = TextRange(s, e))
    }

    /** Toggles Markdown [marker] (**, ~~, ==, `, *) with "continue / leave style" behaviour for a bare cursor. */
    fun toggleMarker(v0: TextFieldValue, marker: String): TextFieldValue {
        val v = normalize(v0)
        val t = v.text
        if (v.selection.collapsed) {
            val c = v.selection.start
            val before = t.substring(0, c)
            val after = t.substring(c)
            val single = marker == "*"
            val afterHas = after.startsWith(marker) && !(single && after.startsWith("**"))
            val beforeHas = before.endsWith(marker) && !(single && before.endsWith("**"))
            if (beforeHas && afterHas) return TextEditing.toggleWrap(v, marker)
            if (afterHas && kindAt(v.text, c, markerKind(marker))) return v.copy(selection = TextRange(c + marker.length))
        }
        return TextEditing.toggleWrap(v, marker)
    }

    private fun markerKind(m: String) = when (m) { "**" -> Kind.BOLD; "~~" -> Kind.STRIKE; "==" -> Kind.MARK; "`" -> Kind.CODE; else -> Kind.ITALIC }

    private fun kindAt(text: String, at: Int, k: Kind) = parse(text).spans.any { it.kind == k && at >= it.start && at <= it.end }

    /**
     * Wraps the selection in ⟦name:arg⟧…⟦/name⟧. If the selection is exactly wrapped by the same tag it is
     * replaced (different arg) or removed (same arg / null arg). A bare cursor starts a styled run, or
     * leaves the run when it sits just before its closing tag.
     */
    fun applyTag(v0: TextFieldValue, name: String, arg: String?): TextFieldValue {
        val v = normalize(v0)
        val t = v.text
        val s = v.selection.min
        val e = v.selection.max
        val close = "⟦/$name⟧"
        val p = parse(t)
        // Exactly wrapped already?
        val kind = kindOf(name)
        val exact = p.pairs.firstOrNull { it.kind == kind && it.openEnd == s && it.closeStart == e && t.substring(it.openStart, it.openEnd).startsWith("⟦$name") }
        if (exact != null) {
            val same = arg == null || exact.arg.equals(arg, ignoreCase = true)
            val newOpen = if (same) "" else open(name, arg)
            val newClose = if (same) "" else close
            val text = t.substring(0, exact.openStart) + newOpen + t.substring(exact.openEnd, exact.closeStart) + newClose + t.substring(exact.closeEnd)
            val shift = newOpen.length - (exact.openEnd - exact.openStart)
            return TextFieldValue(text, TextRange(s + shift, e + shift))
        }
        if (arg == null && kind != null && name in listOf("c", "h", "z", "f")) {
            // "Default" chosen: remove this kind of tag around/inside the selection.
            return removeKinds(v, setOf(kind))
        }
        if (s == e && t.startsWith(close, s)) return v.copy(selection = TextRange(s + close.length))
        val o = open(name, arg)
        val text = t.substring(0, s) + o + t.substring(s, e) + close + t.substring(e)
        return TextFieldValue(text, TextRange(s + o.length, e + o.length))
    }

    private fun open(name: String, arg: String?) = if (arg.isNullOrEmpty()) "⟦$name⟧" else "⟦$name:$arg⟧"

    /** Removes formatting (all inline kinds, or only [kinds]) whose content touches the selection / cursor. */
    fun removeKinds(v0: TextFieldValue, kinds: Set<Kind>? = null): TextFieldValue {
        val v = normalize(v0)
        val t = v.text
        val s = v.selection.min
        val e = v.selection.max
        val p = parse(t)
        val hit = p.pairs.filter { pr ->
            (kinds == null || pr.kind in kinds) &&
                if (s == e) s >= pr.openEnd && s <= pr.closeStart else pr.openEnd < e && pr.closeStart > s
        }
        if (hit.isEmpty()) return v
        val cuts = hit.flatMap { listOf(it.openStart until it.openEnd, it.closeStart until it.closeEnd) }.sortedByDescending { it.first }
        val sb = StringBuilder(t)
        var ns = s
        var ne = e
        for (r in cuts) {
            sb.delete(r.first, r.last + 1)
            val len = r.last + 1 - r.first
            if (r.first < ns) ns -= minOf(len, ns - r.first)
            if (r.first < ne) ne -= minOf(len, ne - r.first)
        }
        return TextFieldValue(sb.toString(), TextRange(ns, ne))
    }

    fun clearFormatting(v: TextFieldValue): TextFieldValue = removeKinds(v, null)

    /** Sets alignment for every selected line (Start removes the tag). */
    fun setAlign(v: TextFieldValue, align: TextAlign): TextFieldValue {
        val tag = when (align) { TextAlign.Center -> "⟦al:c⟧"; TextAlign.End, TextAlign.Right -> "⟦al:r⟧"; TextAlign.Justify -> "⟦al:j⟧"; else -> "" }
        return mapLines(v) { line ->
            val m = alignPrefix.find(line)
            tag + (if (m != null) line.substring(m.value.length) else line)
        }
    }

    /** Indents (+1) or outdents (-1) the selected lines by four spaces. */
    fun indent(v: TextFieldValue, dir: Int): TextFieldValue = mapLines(v) { line ->
        val m = alignPrefix.find(line)
        val head = m?.value ?: ""
        val rest = line.substring(head.length)
        if (dir > 0) "$head    $rest" else head + rest.removePrefix(rest.takeWhile { it == ' ' }.take(4))
    }

    private fun mapLines(v: TextFieldValue, f: (String) -> String): TextFieldValue {
        val t = v.text
        val start = lineStartOf(t, v.selection.min)
        val end = lineEndOf(t, v.selection.max)
        val lines = t.substring(start, end).split('\n')
        val changed = lines.joinToString("\n", transform = f)
        val text = t.substring(0, start) + changed + t.substring(end)
        val delta = changed.length - (end - start)
        val sel = if (v.selection.collapsed && lines.size == 1) {
            val firstDelta = f(lines[0]).length - lines[0].length
            TextRange((v.selection.start + firstDelta).coerceIn(start, start + changed.length))
        } else TextRange(start, end + delta)
        return TextFieldValue(text, sel)
    }

    fun insertDivider(v: TextFieldValue): TextFieldValue {
        val t = v.text
        val c = v.selection.max
        val pre = if (c == 0 || t[c - 1] == '\n') "" else "\n"
        val post = if (c < t.length && t[c] == '\n') "" else "\n"
        val ins = "$pre---$post"
        val text = t.substring(0, c) + ins + t.substring(c)
        return TextFieldValue(text, TextRange(c + ins.length))
    }

    /**
     * Fixes edits that would otherwise land on hidden markers: a backspace that deletes a hidden
     * character removes the visible character before it instead, and a backspace on a line prefix
     * (heading, box, bullet, alignment) removes the whole prefix.
     */
    fun fixDeletion(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        if (old.text.length - new.text.length != 1 || !new.selection.collapsed || !old.selection.collapsed) return new
        val pos = new.selection.start
        if (pos !in 0 until old.text.length) return new
        if (!old.text.regionMatches(0, new.text, 0, pos) || !old.text.regionMatches(pos + 1, new.text, pos, new.text.length - pos)) return new
        val p = parse(old.text)
        val si = p.segAt.getOrElse(pos) { -1 }
        if (si < 0) {
            // Backspace on the space after "•", "☐" or "┃" removes the whole list/quote marker.
            val prev = p.segAt.getOrElse(pos - 1) { -1 }
            if (prev >= 0 && old.text[pos] == ' ') {
                val ps = p.segs[prev]
                if (ps.preferRight && ps.repl.isNotEmpty() && ps.end == pos) {
                    val text = old.text.substring(0, ps.start) + old.text.substring(pos + 1)
                    return TextFieldValue(text, TextRange(ps.start))
                }
            }
            return new
        }
        val seg = p.segs[si]
        if (seg.preferRight || (seg.repl.isNotEmpty() && seg.end - seg.start > 1)) {
            // Remove the whole prefix (plus the space after a box/bullet/quote marker).
            var end = seg.end
            if (seg.repl.isNotEmpty() && end < old.text.length && old.text[end] == ' ') end++
            val text = old.text.substring(0, seg.start) + old.text.substring(end)
            return TextFieldValue(text, TextRange(seg.start))
        }
        if (seg.repl.isNotEmpty()) return new
        var j = pos - 1
        while (j >= 0 && p.isHidden(j)) j--
        if (j < 0) return old.copy(selection = TextRange(pos))
        var from = j
        if (j > 0 && Character.isLowSurrogate(old.text[j]) && Character.isHighSurrogate(old.text[j - 1])) from = j - 1
        val text = old.text.substring(0, from) + old.text.substring(j + 1)
        return TextFieldValue(text, TextRange(from))
    }

    /** Removes every marker, for sharing, previews in plain places and the widget. */
    fun stripTags(text: String): String = text.replace(tagRegex, "")
}
