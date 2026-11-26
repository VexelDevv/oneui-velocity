package dev.velocity.oneui.core

/**
 * Context handed to every hook installation. Only created for processes on
 * the allowlist (see ProcessInfo/Process), after process-name filtering in
 * the entry point.
 */
class HookContext(
    val process: Process,
    val classLoader: ClassLoader,
)
