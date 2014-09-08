package org.vertx.java.core.http.impl;

public class TestWebSocketImplBase {

	/**
	 * handleFrame
	 * 
	 * assumptions: frames are coming in the correct order, i.e. TEXT/BINARY is
	 * followed by a CONTINUATION (if needed); no CONTINUATION without appropriate
	 * TEXT/BINARY or another CONTINNUATION frames comes; no CONTINUATION after
	 * final frame comes. All these assumptions are assured by the Netty WebSocketFrameDecoder 
	 * 
	 * Note: only processing for the dataHandler is tested since the code for the frameHandler code has not been changed
	 * 
	 * Testing plan:
	 * 
	 * - one frame binary message -> handle on the dataHandler is invoked
	 * 
	 * - one not final frame ->  handle on the dataHandler is not invoked
	 * 
	 * - several frames in the correct order are sent ->  handle on the dataHandler is invoked with the complete data
	 * 
	 * - integration test on a multi frames message 
	 * 
	 */
	
	
}
