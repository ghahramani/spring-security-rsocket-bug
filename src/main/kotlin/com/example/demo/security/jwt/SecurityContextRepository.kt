package com.example.demo.security.jwt

import org.slf4j.LoggerFactory
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * @author Navid Ghahremani (ghahramani.navid@gmail.com)
 */

@Component
class SecurityContextRepository(private val manager: ReactiveAuthenticationManager) : ServerSecurityContextRepository {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun save(exchange: ServerWebExchange?, context: SecurityContext?): Mono<Void> {
        throw UnsupportedOperationException("Save is not supported")
    }

    override fun load(exchange: ServerWebExchange): Mono<SecurityContext> =
        resolveToken(exchange.request)
            .flatMap { jwt ->
                logger.debug("user's request uri: {}", exchange.request.uri)
                logger.debug("request's path within application': {}", exchange.request.path.pathWithinApplication())
                logger.debug("request's headers: {}", exchange.request.headers.toSingleValueMap().toString())
                manager
                    .authenticate(BearerTokenAuthenticationToken(jwt))
                    .map { auth ->
                        logger.debug(
                            "Username: {} has roles: {}",
                            (auth.principal as Jwt).subject,
                            auth.authorities.map { item -> item.authority }
                        )
                        val context = SecurityContextImpl(auth)
                        // We need this to have context in non-reactive calls as well
                        SecurityContextHolder.setContext(context)
                        context
                    }
            }

    private fun resolveToken(request: ServerHttpRequest) = Mono
        .just(request)
        .flatMap {
            val bearerToken = request.headers.getFirst("Authorization") ?: ""
            logger.debug("A request was received, is the jwt empty ? {}", bearerToken.isEmpty())
            if (bearerToken.startsWith("Bearer ")) {
                Mono.just(bearerToken.substring("Bearer ".length, bearerToken.length))
            } else {
                Mono.empty()
            }
        }

}
