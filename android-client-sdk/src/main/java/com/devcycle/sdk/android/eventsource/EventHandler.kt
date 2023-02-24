package com.devcycle.sdk.android.eventsource

import com.devcycle.sdk.android.util.DVCLogger


class Handler(
    private var onMessageHandler: (messageEvent: MessageEvent?) -> Any,
    private val logger: DVCLogger
): EventHandler {

    fun init() {}

    /**
     * EventSource calls this method when the stream connection has been opened.
     * @throws Exception throwing an exception here will cause it to be logged and also sent to [.onError]
     */
    override fun onOpen() {
        logger.d("Opened SSE connection. Now listening for config updates.")
    }

    /**
     * EventSource calls this method when the stream connection has been closed.
     *
     *
     * This method is *not* called if the connection was closed due to a [ConnectionErrorHandler]
     * returning [ConnectionErrorHandler.Action.SHUTDOWN]; EventSource assumes that if you registered
     * such a handler and made it return that value, then you already know that the connection is being closed.
     *
     *
     * There is a known issue where `onClosed()` may or may not be called if the stream has been
     * permanently closed by calling `close()`.
     *
     * @throws Exception throwing an exception here will cause it to be logged and also sent to [.onError]
     */
    override fun onClosed() {
        logger.d("Closed SSE connection.")
    }

    /**
     * EventSource calls this method when it has received a new event from the stream.
     * @param event the event name, from the `event:` line in the stream
     * @param messageEvent a [MessageEvent] object containing all the other event properties
     * @throws Exception throwing an exception here will cause it to be logged and also sent to [.onError]
     */
    @Throws(Exception::class)
    override fun onMessage(event: String?, messageEvent: MessageEvent?) {
        this.onMessageHandler(messageEvent)
    }

    @Throws(Exception::class)
    override fun onComment(comment: String?) {
    }

    /**
     * This method will be called for all exceptions that occur on the socket connection (including
     * an [UnsuccessfulResponseException] if the server returns an unexpected HTTP status),
     * but only after the [ConnectionErrorHandler] (if any) has processed it.  If you need to
     * do anything that affects the state of the connection, use [ConnectionErrorHandler].
     *
     *
     * This method is *not* called if the error was already passed to a [ConnectionErrorHandler]
     * which returned [ConnectionErrorHandler.Action.SHUTDOWN]; EventSource assumes that if you registered
     * such a handler and made it return that value, then you do not want to handle the same error twice.
     *
     * @param t  a `Throwable` object
     */
    override fun onError(t: Throwable?) {
        logger.e("", t)
    }
}