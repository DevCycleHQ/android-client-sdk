package com.devcycle.sdk.android.util

import android.os.Build
import android.util.Log
import org.jetbrains.annotations.NonNls
import java.io.PrintWriter
import java.io.StringWriter
import java.util.ArrayList
import java.util.regex.Pattern

/** Logging for lazy people. */
class DevCycleLogger private constructor() {
  init {
    throw AssertionError()
  }

  /** A facade for handling logging calls. Install instances via [`DVCLogger.start()`][.start]. */
  abstract class Logger {
    @get:JvmSynthetic // Hide from public API.
    internal val explicitTag = ThreadLocal<String>()

    @get:JvmSynthetic // Hide from public API.
    internal open val tag: String?
      get() {
        val tag = explicitTag.get()
        if (tag != null) {
          explicitTag.remove()
        }
        return tag
      }

    /** Log a verbose message with optional format args. */
    open fun v(message: String?, vararg args: Any?) {
      prepareLog(Log.VERBOSE, null, message, *args)
    }

    /** Log a verbose exception and a message with optional format args. */
    open fun v(t: Throwable?, message: String?, vararg args: Any?) {
      prepareLog(Log.VERBOSE, t, message, *args)
    }

    /** Log a verbose exception. */
    open fun v(t: Throwable?) {
      prepareLog(Log.VERBOSE, t, null)
    }

    /** Log a debug message with optional format args. */
    open fun d(message: String?, vararg args: Any?) {
      prepareLog(Log.DEBUG, null, message, *args)
    }

    /** Log a debug exception and a message with optional format args. */
    open fun d(t: Throwable?, message: String?, vararg args: Any?) {
      prepareLog(Log.DEBUG, t, message, *args)
    }

    /** Log a debug exception. */
    open fun d(t: Throwable?) {
      prepareLog(Log.DEBUG, t, null)
    }

    /** Log an info message with optional format args. */
    open fun i(message: String?, vararg args: Any?) {
      prepareLog(Log.INFO, null, message, *args)
    }

    /** Log an info exception and a message with optional format args. */
    open fun i(t: Throwable?, message: String?, vararg args: Any?) {
      prepareLog(Log.INFO, t, message, *args)
    }

    /** Log an info exception. */
    open fun i(t: Throwable?) {
      prepareLog(Log.INFO, t, null)
    }

    /** Log a warning message with optional format args. */
    open fun w(message: String?, vararg args: Any?) {
      prepareLog(Log.WARN, null, message, *args)
    }

    /** Log a warning exception and a message with optional format args. */
    open fun w(t: Throwable?, message: String?, vararg args: Any?) {
      prepareLog(Log.WARN, t, message, *args)
    }

    /** Log a warning exception. */
    open fun w(t: Throwable?) {
      prepareLog(Log.WARN, t, null)
    }

    /** Log an error message with optional format args. */
    open fun e(message: String?, vararg args: Any?) {
      prepareLog(Log.ERROR, null, message, *args)
    }

    /** Log an error exception and a message with optional format args. */
    open fun e(t: Throwable?, message: String?, vararg args: Any?) {
      prepareLog(Log.ERROR, t, message, *args)
    }

    /** Log an error exception. */
    open fun e(t: Throwable?) {
      prepareLog(Log.ERROR, t, null)
    }

    /** Log an assert message with optional format args. */
    open fun wtf(message: String?, vararg args: Any?) {
      prepareLog(Log.ASSERT, null, message, *args)
    }

    /** Log an assert exception and a message with optional format args. */
    open fun wtf(t: Throwable?, message: String?, vararg args: Any?) {
      prepareLog(Log.ASSERT, t, message, *args)
    }

    /** Log an assert exception. */
    open fun wtf(t: Throwable?) {
      prepareLog(Log.ASSERT, t, null)
    }

    /** Log at `priority` a message with optional format args. */
    open fun log(priority: Int, message: String?, vararg args: Any?) {
      prepareLog(priority, null, message, *args)
    }

    /** Log at `priority` an exception and a message with optional format args. */
    open fun log(priority: Int, t: Throwable?, message: String?, vararg args: Any?) {
      prepareLog(priority, t, message, *args)
    }

    /** Log at `priority` an exception. */
    open fun log(priority: Int, t: Throwable?) {
      prepareLog(priority, t, null)
    }

    /** Return whether a message at `priority` should be logged. */
    @Deprecated("Use isLoggable(String, int)", ReplaceWith("this.isLoggable(null, priority)"))
    protected open fun isLoggable(priority: Int) = true

    /** Return whether a message at `priority` or `tag` should be logged. */
    protected open fun isLoggable(tag: String?, priority: Int) = isLoggable(priority)

    private fun prepareLog(priority: Int, t: Throwable?, message: String?, vararg args: Any?) {
      // Consume tag even when message is not loggable so that next message is correctly tagged.
      val tag = tag
      if (!isLoggable(tag, priority)) {
        return
      }

      var message = message
      if (message.isNullOrEmpty()) {
        if (t == null) {
          return  // Swallow message if it's null and there's no throwable.
        }
        message = getStackTraceString(t)
      } else {
        if (args.isNotEmpty()) {
          message = formatMessage(message, args)
        }
        if (t != null) {
          message += "\n" + getStackTraceString(t)
        }
      }

      log(priority, tag, message, t)
    }

    /** Formats a log message with optional arguments. */
    protected open fun formatMessage(message: String, args: Array<out Any?>) = message.format(*args)

    private fun getStackTraceString(t: Throwable): String {
      // Don't replace this with Log.getStackTraceString() - it hides
      // UnknownHostException, which is not what we want.
      val sw = StringWriter(256)
      val pw = PrintWriter(sw, false)
      t.printStackTrace(pw)
      pw.flush()
      return sw.toString()
    }

    /**
     * Write a log message to its destination. Called for all level-specific methods by default.
     *
     * @param priority Log level. See [Log] for constants.
     * @param tag Explicit or inferred tag. May be `null`.
     * @param message Formatted log message.
     * @param t Accompanying exceptions. May be `null`.
     */
    protected abstract fun log(priority: Int, tag: String?, message: String, t: Throwable?)
  }

  /** A [Logger] for debug builds. Automatically infers the tag from the calling class. */
  open class DebugLogger : Logger() {
    private val fqcnIgnore = listOf(
      DevCycleLogger::class.java.name,
      DevCycleLogger.Loggers::class.java.name,
      Logger::class.java.name,
      DebugLogger::class.java.name
    )

    override val tag: String?
      get() = super.tag ?: Throwable().stackTrace
        .first { it.className !in fqcnIgnore }
        .let(::createStackElementTag)

    /**
     * Extract the tag which should be used for the message from the `element`. By default
     * this will use the class name without any anonymous class suffixes (e.g., `Foo$1`
     * becomes `Foo`).
     *
     * Note: This will not be called if a [manual tag][.tag] was specified.
     */
    protected open fun createStackElementTag(element: StackTraceElement): String? {
      var tag = element.className.substringAfterLast('.')
      val m = ANONYMOUS_CLASS.matcher(tag)
      if (m.find()) {
        tag = m.replaceAll("")
      }
      // Tag length limit was removed in API 26.
      return if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= 26) {
        tag
      } else {
        tag.substring(0, MAX_TAG_LENGTH)
      }
    }

    /**
     * Break up `message` into maximum-length chunks (if needed) and send to either
     * [Log.println()][Log.println] or
     * [Log.wtf()][Log.wtf] for logging.
     *
     * {@inheritDoc}
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
      if (message.length < MAX_LOG_LENGTH) {
        if (priority == Log.ASSERT) {
          Log.wtf(tag, message)
        } else {
          Log.println(priority, tag, message)
        }
        return
      }

      // Split by line, then ensure each line can fit into Log's maximum length.
      var i = 0
      val length = message.length
      while (i < length) {
        var newline = message.indexOf('\n', i)
        newline = if (newline != -1) newline else length
        do {
          val end = Math.min(newline, i + MAX_LOG_LENGTH)
          val part = message.substring(i, end)
          if (priority == Log.ASSERT) {
            Log.wtf(tag, part)
          } else {
            Log.println(priority, tag, part)
          }
          i = end
        } while (i < newline)
        i++
      }
    }

    companion object {
      private const val MAX_LOG_LENGTH = 4000
      private const val MAX_TAG_LENGTH = 23
      private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
    }
  }

  companion object Loggers : Logger() {
    /** Log a verbose message with optional format args. */
    @JvmStatic override fun v(@NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.v(message, *args) }
    }

    /** Log a verbose exception and a message with optional format args. */
    @JvmStatic override fun v(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.v(t, message, *args) }
    }

    /** Log a verbose exception. */
    @JvmStatic override fun v(t: Throwable?) {
      loggerArray.forEach { it.v(t) }
    }

    /** Log a debug message with optional format args. */
    @JvmStatic override fun d(@NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.d(message, *args) }
    }

    /** Log a debug exception and a message with optional format args. */
    @JvmStatic override fun d(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.d(t, message, *args) }
    }

    /** Log a debug exception. */
    @JvmStatic override fun d(t: Throwable?) {
      loggerArray.forEach { it.d(t) }
    }

    /** Log an info message with optional format args. */
    @JvmStatic override fun i(@NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.i(message, *args) }
    }

    /** Log an info exception and a message with optional format args. */
    @JvmStatic override fun i(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.i(t, message, *args) }
    }

    /** Log an info exception. */
    @JvmStatic override fun i(t: Throwable?) {
      loggerArray.forEach { it.i(t) }
    }

    /** Log a warning message with optional format args. */
    @JvmStatic override fun w(@NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.w(message, *args) }
    }

    /** Log a warning exception and a message with optional format args. */
    @JvmStatic override fun w(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.w(t, message, *args) }
    }

    /** Log a warning exception. */
    @JvmStatic override fun w(t: Throwable?) {
      loggerArray.forEach { it.w(t) }
    }

    /** Log an error message with optional format args. */
    @JvmStatic override fun e(@NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.e(message, *args) }
    }

    /** Log an error exception and a message with optional format args. */
    @JvmStatic override fun e(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.e(t, message, *args) }
    }

    /** Log an error exception. */
    @JvmStatic override fun e(t: Throwable?) {
      loggerArray.forEach { it.e(t) }
    }

    /** Log an assert message with optional format args. */
    @JvmStatic override fun wtf(@NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.wtf(message, *args) }
    }

    /** Log an assert exception and a message with optional format args. */
    @JvmStatic override fun wtf(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.wtf(t, message, *args) }
    }

    /** Log an assert exception. */
    @JvmStatic override fun wtf(t: Throwable?) {
      loggerArray.forEach { it.wtf(t) }
    }

    /** Log at `priority` a message with optional format args. */
    @JvmStatic override fun log(priority: Int, @NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.log(priority, message, *args) }
    }

    /** Log at `priority` an exception and a message with optional format args. */
    @JvmStatic
    override fun log(priority: Int, t: Throwable?, @NonNls message: String?, vararg args: Any?) {
      loggerArray.forEach { it.log(priority, t, message, *args) }
    }

    /** Log at `priority` an exception. */
    @JvmStatic override fun log(priority: Int, t: Throwable?) {
      loggerArray.forEach { it.log(priority, t) }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
      throw AssertionError() // Missing override for log method.
    }

    /**
     * A view into DVCLogger's started loggers as a logger itself. This can be used for injecting a logger
     * instance rather than using static methods or to facilitate testing.
     */
    @Suppress(
      "NOTHING_TO_INLINE", // Kotlin users should reference `Logger.Forest` directly.
      "NON_FINAL_MEMBER_IN_OBJECT" // For japicmp check.
    )
    @JvmStatic
    open inline fun asLogger(): Logger = this

    /** Set a one-time tag for use on the next logging call. */
    @JvmStatic fun tag(tag: String): Logger {
      for (logger in loggerArray) {
        logger.explicitTag.set(tag)
      }
      return this
    }

    /** Add a new logging logger. */
    @JvmStatic fun start(logger: Logger) {
      require(logger !== this) { "Cannot start DVCLogger into itself." }
      synchronized(loggers) {
        loggers.add(logger)
        loggerArray = loggers.toTypedArray()
      }
    }

    /** Remove a started logger. */
    @JvmStatic fun stop(logger: Logger) {
      synchronized(loggers) {
        require(loggers.remove(logger)) { "Cannot stop logger which is not started: $logger" }
        loggerArray = loggers.toTypedArray()
      }
    }

    @get:[JvmStatic JvmName("loggerCount")]
    val loggerCount get() = loggerArray.size

    // Both fields guarded by 'loggers'.
    private val loggers = ArrayList<Logger>()
    @Volatile private var loggerArray = emptyArray<Logger>()
  }
}

@Deprecated("DVCLogger is deprecated, use DevCycleLogger instead")
typealias DVCLogger = DevCycleLogger