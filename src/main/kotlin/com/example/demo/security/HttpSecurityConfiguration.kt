package com.example.demo.security

import com.example.demo.security.jwt.SecurityContextRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod.OPTIONS
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
        http: ServerHttpSecurity,
        manager: ReactiveAuthenticationManager,
        repository: SecurityContextRepository
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

            .securityContextRepository(repository)
            .authenticationManager(manager)

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
