package com.example.demo

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.resolver.DefaultAddressResolverGroup
import io.rsocket.transport.netty.client.TcpClientTransport
import org.springframework.boot.autoconfigure.rsocket.RSocketProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.codec.Decoder
import org.springframework.core.codec.Encoder
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.cbor.Jackson2CborDecoder
import org.springframework.http.codec.cbor.Jackson2CborEncoder
import org.springframework.messaging.rsocket.MetadataExtractorRegistry
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.util.MimeType
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.util.retry.Retry
import java.time.Duration

@Configuration
class Config {

    @Bean
    fun www(webClient: WebClient.Builder): WebClient {
        val httpClient = HttpClient
            .create()
            .secure { provider ->
                val sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build()
                provider.sslContext(sslContext)
            }
            .resolver(DefaultAddressResolverGroup.INSTANCE)

        val strategies = ExchangeStrategies
            .builder()
            .codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(524288000) }
            .build()
        return WebClient
            .builder()
            .exchangeStrategies(strategies)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }

    @Bean
    fun rSocketStrategies() = RSocketStrategies
        .builder()
        .encoders { encoders: MutableList<Encoder<*>> ->
            encoders.add(Jackson2CborEncoder())
        }
        .decoders { decoders: MutableList<Decoder<*>> -> decoders.add(Jackson2CborDecoder()) }
        .metadataExtractorRegistry { metadataExtractorRegistry: MetadataExtractorRegistry ->
            metadataExtractorRegistry.metadataToExtract(
                MimeType.valueOf("message/x.file.name"),
                String::class.java,
                null
            )
        }
        .build()

    @Bean
    fun requester(properties: RSocketProperties, strategies: RSocketStrategies, builder: RSocketRequester.Builder): Mono<RSocketRequester> =
        Mono.create { emitter ->
            val requester = builder
                .rsocketConnector { connector -> connector.reconnect(Retry.fixedDelay(2, Duration.ofSeconds(2))) }
                .rsocketStrategies(strategies)
                .transport(TcpClientTransport.create("localhost", 9998))
            emitter.success(requester)
        }

}
