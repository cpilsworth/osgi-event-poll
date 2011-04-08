package uk.co.diffa.eventpoll.server;

import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class ChunkedHttpRequestHandler extends SimpleChannelUpstreamHandler {

    private static final Logger log = LoggerFactory.getLogger(ChunkedHttpRequestHandler.class);

    private final ChannelGroup clientChannels;
    
    public ChunkedHttpRequestHandler(ChannelGroup clientChannels) {
    	this.clientChannels = clientChannels;
    }

    /**
     * Handle the initial request and prepare clients for long poll updates
     * @param ctx
     * @param e
     * @throws Exception
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

            writeResponse(e);
    }

    /**
     * Write a basic HTTP response headers back so that clients expect chunked content
     * @param e
     */
    private void writeResponse(MessageEvent e) {

        // Build the response object.
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);               
        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.setHeader(TRANSFER_ENCODING, "chunked");

        // Write initial response headers
        final Channel channel = e.getChannel();
        channel.write(response);                  
        clientChannels.add(channel);

        // When a channel is closed remove from the channels notification pool
        channel.getCloseFuture().addListener(new ChannelFutureListener() {			
			private final Logger log = LoggerFactory.getLogger(ChannelFutureListener.class);
			public void operationComplete(ChannelFuture channelFuture) throws Exception {
				log.debug("Channel closed: {}", channel);
                if (clientChannels.contains(channel)) {
                    clientChannels.remove(channel);
                }
			}
		});                      
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        log.error("Exception caught handling HTTP request - closing channel", e.getCause());
        e.getChannel().close();
    }
}