package com.devcycle.sdk.android.eventsource;


interface ConnectionHandler {
  // setReconnectionTime should be in Milliseconds
  void setReconnectionTime(long reconnectionTime);
  void setLastEventId(String lastEventId);
}