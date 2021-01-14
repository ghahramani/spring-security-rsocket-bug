package com.example.demo.security.jwt

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import java.time.Instant

/**
 * @author Navid Ghahremani (ghahramani.navid@gmail.com)
 */

class AuthenticationToken(
    tokenValue: String,
    headers: Map<String, Any>,
    claims: Map<String, Any>
) : Jwt(
    tokenValue,
    claims[JwtClaimNames.IAT] as Instant,
    claims[JwtClaimNames.EXP] as Instant,
    headers,
    claims
)

