package com.example.churchmusicplayer.ui

/**
 * Where a line may break, and where it may not.
 *
 * Korean puts no spaces inside a word, and Android's line breaker will happily
 * split one down the middle — the setting that stops it properly arrived in
 * API 33 and this app still runs on 21. So the rule is spelled into the text
 * itself: a joiner welds a word's characters together, a hard space welds two
 * words into one phrase, and an ordinary space is left as the only opening.
 *
 * Both characters are written by code point rather than typed. They are
 * invisible, and a line of source nobody can see is a line nobody can fix.
 */
private val JOINER = Char(0x2060).toString()
private val HARD_SPACE = Char(0x00A0)

/** Words stay whole; a line may still break between them. */
fun weldWords(text: String): String =
    text.split(" ").joinToString(" ") { word -> word.toCharArray().joinToString(JOINER) }

/** One phrase the line breaker may not open up, however many words are in it. */
fun unbreakable(text: String): String = text.replace(' ', HARD_SPACE)
