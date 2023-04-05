package dev.toad;

public abstract sealed class RetryStrategy permits RetryStrategyDelay, RetryStrategyExponential { }
