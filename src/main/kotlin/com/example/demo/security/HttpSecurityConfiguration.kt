package com.example.demo.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity(proxyTargetClass = true)
@ConditionalOnClass(name = ["org.springframework.web.reactive.config.WebFluxConfigurer"])
class HttpSecurityConfiguration {

    @Bean
    fun filter(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain {

        return http
            .csrf().disable()
            .httpBasic().disable()
            .formLogin().disable()
            .logout().disable()

            .authorizeExchange()

            .anyExchange().permitAll()

            .and()

            .exceptionHandling()

            .and()

            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(PASSWORD_STRENGTH)
    }

    companion object {
        private const val PASSWORD_STRENGTH = 10
    }
}
