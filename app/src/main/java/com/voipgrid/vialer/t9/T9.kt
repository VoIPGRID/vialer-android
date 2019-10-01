package com.voipgrid.vialer.t9

object T9 {

    private val keyMappings = mapOf(
            '+' to charArrayOf(),
            '0' to charArrayOf(),
            '1' to charArrayOf(),
            '2' to charArrayOf('a', 'b', 'c'),
            '3' to charArrayOf('d', 'e', 'f'),
            '4' to charArrayOf('g', 'h', 'i'),
            '5' to charArrayOf('j', 'k', 'l'),
            '6' to charArrayOf('m', 'n', 'o'),
            '7' to charArrayOf('p', 'q', 'r', 's'),
            '8' to charArrayOf('t', 'u', 'v'),
            '9' to charArrayOf('x', 'y', 'z')
    )

    /**
     * Convert a full t9 query (e.g. 246) to a regex that will match the relevant characters in the map.
     *
     */
    fun convertT9QueryToRegexQuery(query: String) : Regex {
        return query.map { createRegexForKey(it) }.joinToString(separator = "", prefix = "^").toRegex()
    }

    /**
     * Performs a backwards conversion, converting a result (likely a contact name) back into
     * what t9 query would have produced it.
     *
     */
    fun convertResultBackIntoT9Query(query: String) : String {
        var result = ""

        query.forEach {
            keyMappings.forEach { keyMapping ->
                if (keyMapping.value.contains(it.toLowerCase())) {
                    result += keyMapping.key
                }
            }
        }

        return result
    }

    /**
     * Return the string of letters that maps to the given digit.
     *
     */
    fun getLettersThatMapToKey(key: Char) : String? {
        val chars = keyMappings[key] ?: throw RuntimeException("Unable to locate mapping for $key")

        return chars.joinToString("")
    }

    /**
     * Dynamically builds the regex to match a single key on the dial pad.
     *
     * For example, we will match 3 to a regex that will match d/e/f/7.
     *
     */
    private fun createRegexForKey(key: Char) : Regex {
        return Regex("[$key${getLettersThatMapToKey(key)}]{1}[^A-Za-z0-9]?")
    }
}