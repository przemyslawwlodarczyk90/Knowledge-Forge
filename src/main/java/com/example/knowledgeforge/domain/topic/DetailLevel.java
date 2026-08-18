package com.example.knowledgeforge.domain.topic;

/**
 * Poziom szczegółowości wpisu — nie "trudność treści", tylko to, jak
 * dopracowana jest notatka:
 *  LOW    — szkic, temat zanotowany i zaplanowany do uzupełnienia później
 *  MEDIUM — notatka robocza, spisana szybko, czytelna głównie dla autora
 *  HIGH   — notatka dopracowana, napisana tak, by zrozumiał ją każdy
 */
public enum DetailLevel {
    LOW, MEDIUM, HIGH
}
