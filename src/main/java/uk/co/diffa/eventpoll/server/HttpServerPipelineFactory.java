package uk.co.diffa.eventpoll.server;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

import static org.jboss.netty.channel.Channels.pipeline;

public class HttpServerPipelineFactory implements ChannelPipelineFactory {

    /** Set of connected client channels */
	private ChannelGroup clientChannels;
	
	public HttpServerPipelineFactory(ChannelGroup clientChannels) {
		this.clientChannels = clientChannels;
	}
	
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("handler", new ChunkedHttpRequestHandler(clientChannels));
        return pipeline;
    }
}