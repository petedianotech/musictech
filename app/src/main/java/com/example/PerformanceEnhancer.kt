package com.example

object PerformanceEnhancer {
    var isAvailable = false

    init {
        try {
            System.loadLibrary("performance_enhancer")
            isAvailable = true
        } catch (e: UnsatisfiedLinkError) {
            isAvailable = false
        }
    }

    external fun getNativeOptimizationFlag(): String
    external fun calculateDurationOpt(duration: Long): Long
}
