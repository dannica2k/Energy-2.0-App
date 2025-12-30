package com.example.energy20.utils

import org.jsoup.Jsoup

/**
 * Utility class to extract JSON data from HTML pages
 */
object HtmlJsonExtractor {
    
    /**
     * Extracts the dailyData JSON object from the HTML page
     * Looks for: const dailyData = {...};
     * 
     * @param html The HTML content from daily_consumption.php
     * @return JSON string or null if not found
     */
    fun extractDailyData(html: String): String? {
        try {
            // Parse HTML with Jsoup
            val doc = Jsoup.parse(html)
            
            // Find all script tags
            val scripts = doc.select("script")
            
            // Look for the script containing dailyData
            for (script in scripts) {
                val scriptContent = script.html()
                
                // Check if this script contains dailyData
                if (scriptContent.contains("const dailyData")) {
                    // Extract JSON using regex
                    // Pattern: const dailyData = {...};
                    // Note: The JSON may span multiple lines
                    val regex = Regex(
                        pattern = "const dailyData\\s*=\\s*(\\{[^;]+\\});",
                        options = setOf(RegexOption.DOT_MATCHES_ALL)
                    )
                    
                    val match = regex.find(scriptContent)
                    if (match != null && match.groupValues.size > 1) {
                        return match.groupValues[1]
                    }
                }
            }
            
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
