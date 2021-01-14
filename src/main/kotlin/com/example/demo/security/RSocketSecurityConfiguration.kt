package com.example.demo.security

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity.AuthorizePayloadsSpec
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor

@Configuration
@EnableRSocketSecurity
class RSocketSecurityConfiguration {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun messageHandler(strategies: RSocketStrategies): RSocketMessageHandler {
        val handler = RSocketMessageHandler()
        handler.argumentResolverConfigurer.addCustomResolver(AuthenticationPrincipalArgumentResolver())
        handler.rSocketStrategies = strategies
        logger.info("Configuring RSocket handler")
        return handler
    }

    @Bean
    fun authorization(
        security: RSocketSecurity
    ): PayloadSocketAcceptorInterceptor = security
        .authorizePayload { spec: AuthorizePayloadsSpec ->
            logger.info("Configuring RSocket authorization and jwt")
            spec
                .anyExchange().permitAll()
        }
        .build()

}
