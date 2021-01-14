package com.example.demo.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager

@Configuration
class CommonSecurityConfiguration {

    @Bean
    fun authenticationManager(decoder: ReactiveJwtDecoder) = JwtReactiveAuthenticationManager(decoder)

}
