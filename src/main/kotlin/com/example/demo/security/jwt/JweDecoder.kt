package com.example.demo.security.jwt

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.crypto.RSADecrypter
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWT
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.stereotype.Component
import org.springframework.util.Base64Utils
import reactor.core.publisher.Mono
import java.text.ParseException
import java.util.*

/**
 * @author Navid Ghahremani (ghahramani.navid@gmail.com)
 */

@Component
class JweDecoder : ReactiveJwtDecoder {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val claimSetConverter by lazy {
        MappedJwtClaimSetConverter.withDefaults(emptyMap())
    }

    private val encryptionKey: RSAKey by lazy {
        val key =
            "ew0KICAgICJwIjogInprbmh6NjMxSGIwZkJNT09oQXV6UG1iMFlYVjdXNEJzY0wwREtYbUF4cjVBVWMwRUs2ZHZhMk92cGM1dy1LUGVKZlVTQXdLOGhKMDc1WmlPbndiMXk1aVQ0VmZ2a2Z3eWRiMzVNVUhtVWRKQUR6WkVBRHBUdDZ4a0ZWLUFncWlNbWRSQjE1eVVjQlA4Si02TzFRM0ZZdjJrcHI3R2lEb0dGUWd3WVJfUkpBOCIsDQogICAgImt0eSI6ICJSU0EiLA0KICAgICJxIjogInFTbDdkOF9pZzZBbjViVWdNT3FfMjNTNnhCVkd2QlprRmwtTUZBWmxRLVBOMGpDX0l4TW9hUVd2eUVvV3JUZlpOUEhpcG1FdTY0cVgzLWl4S0JsVmdlTHlJWVExUXh2bFR5SWpSX0R5M1hQWGlZU2w5aURJU0tJM1VCS0ZCZ09Zb25MUFpMM0dlUFc1LXR0OFVWX2JQZzc1eXlRaE11NWxZUjVNSWFXNU0xVSIsDQogICAgImQiOiAiS2xWRjhPT2hLUkdtQm1lczBxUXBHcVFETGxCY09zblZrYU90LWVuYUMtUnVXdG1XSlN4YzIzSkQyR2FjQTVfRm9hdllLWllJcmtrZFhZREFZMmlQMU9qdzFOSmdfWTdtLXhkR0NfWFJ2bEhxR21LRUw5eExlV2RPaVE5TzZDTUdib2VUR3RQcTJJYjdrQV9STEtWWFRyQjVfTG9ySno5LXB4dVFxVE1zcjFsRTNWZHl0X3NHaG56emNxZEdwVWpYM3FNT2cxTC1uNUZsalhTclV6Zkl4aVdxbGpfQ2xsRHBseGxFclBqaDF4THdKZWxlSE9Hbl9sQWhNNXhudnRYeVNCNUtCQ1hOY0drb1NDUzZXa3VhSFdjQ2FNZXo4NWc3aU83XzZWbjRfSzJWVjd0bG5lZ2VwanoyV0FzRzZOWU9CU3BKVWVHUzJheEJkMGJoMTVQU21RIiwNCiAgICAiZSI6ICJBUUFCIiwNCiAgICAidXNlIjogImVuYyIsDQogICAgImtpZCI6ICJ0ZXN0LXNwcmluZy1zZWN1cml0eSIsDQogICAgInFpIjogIlZEN2pPM1M0REFwaG5ndmUtdm02NkRUZWE3dzFlc01jeXpaTXJFZzdUY3I0eFhtUWxMU1pLM3FTblFoRHFocDBZeFc5TXlCa3VNbjBVUlJ6a2RIZXF5M0tHeDJOeHh5a090UXhzNUFmQ0Y5eElyNGVMaEEtQWVEanlxZDdqZ2luWmNKSmR4UDM2MUpkeDRTdjl2elNSd3dlLWdJWUlWS3c0dVVtOFpsV2lfSSIsDQogICAgImRwIjogImh6UnhaamJ6OHR2am1yRzVsTjQtU3VZYndrekgzejhhMUFGdU14N0p3bmtybXZHdWpCWWZ0dGtOWkxVYjRqczBfTmZWU0ZmZ3kySnF3Wjd0eTFrNmZJaXlVXzNZcld3SWZwZFczbXY4MHhfa2tKUFdtZmVncXotNEVUSVlfb29PTFR5ck12QVRjaW9IR1AwSWJLZXItYWVZNVJrZzhtUmxYQi1UNWZSLWxRMCIsDQogICAgImFsZyI6ICJSUzUxMiIsDQogICAgImRxIjogIk95LXFaV2MwNTd6WGVNV0F0OXpLdC13RmxLbWxFQlg5ZXQ1X1VscFNnQWhxY1FwaE5kSjBKeHE0UUNtNy1XczY4ZzFYc2NMNi1hcUtMT3RyWUk1TW5wOWFfR01YaDUxeEE5ajc0eVhvczViZWFOMGlmQlZJUGpHVGpNNk9BR2F0dHRqWXUzU3l4b3VqUUtRZXdSSDYwYUhId0UycDFfb1ZPbi14UDJ5T2VlayIsDQogICAgIm4iOiAiaUZBelpVOTBzZU5pNEZaZ1FEWGhoV0RwUnBIQ3ZBUkg4SVVGSTZnTjI1U0M4X3ktQTBiVHZGR19naVozX3o0ZEJlUjZMTzViUHF4d29aUVVVX3VRSC1oVE1CblZOWGU4bTJSTWR1LVVfUGJpNHIwaFJnY2NYQTQ4em9Ec2oxejRuSEg2S3pzWms2QTFCRTV6c3NDZWJzODM3S0hBWmZjazBzanJCRlpiN1RhdjBnRlNnTEc5T3lpdGRwRGM4VS0xWGJCLWo3SDBNNk1sMjB1SzdhZlVwYnZLQWRleHhOenVuSm5tXzZIb2k5Zll5eUNJdHo3Nnh2MmpOc2hOelVOTjloYzJtVWJtcDlxeThXZjEwbHlxNm1NMmtadnpRTHhTYms5dmtBR1FRS2s2cnlXaEN5eGR1b0NfNXF6OW1Td0t0UTRFajN5UUsxMUdlVTNsOUhiMS13Ig0KfQ=="
        logger.debug("Decoding encryption key: `$key`")
        RSAKey.parse(String(Base64Utils.decode(key.toByteArray())))
    }

    private val signatureKey: RSAKey by lazy {
        val key =
            "ew0KICAgICJwIjogIjhEZGNVRmpKMGVLYTE4b3ROSlFHNUJlZmxyaGpfNHlYbWh4QXQ5STRtRkRDT0tNM2MzTmlVa2ZubTM5T2FvV2RUampWQ3prYXVIM2xNMjdzRThrd0RSM0tnNDNkZG1MdFhUNnFkSWtuQ1c4VXhFclp4Tmp3ZDk3RHNGdzNjOVlfNldiR2RQZFFENlNlSGhOTmhzT0pfOWxKU1l4RFJIZUlwY3VHclM5cHdMVSIsDQogICAgImt0eSI6ICJSU0EiLA0KICAgICJxIjogIndNdXFUSlN3ZEhySm45Z0t0aG1Xc245d0t0dWV4eVUzeFVES1phWTJaaVNrVTg3RU0wUzBodjZxcGFMMURZa2ttcXZ4b01sMlZ1eWN2WUhVWHhxYTg2V2taQXpqaE5tZ3FZUTdjdTZNVFJPZ0lKYmJBZDRYTmV4WmNMblpCQ0VYNENOcHRZalc1RXpLRWp4d0s1aWlyWWVVRWRENEFINEJEakx1MHBiOXNTcyIsDQogICAgImQiOiAicFQtNWdhcllzU0x2bXJFRVY2b01CSVI1UGNrUTN2d3dhRGlrT1QwVGdDSkFISDd3UEl5YTJscV84X19xZy1XaGRobkdfTkZwUnVIZDVoNnJpUGczUHVUWlVldWk3NkgxaWFXWXR6dHc0U3FBcFRvTnk4MVYxclo3ai0waXNFQ1FFeWdYdGRzWkllbHR2emNUazNLQXVFR2ZpdlhGeVRJZTRneFZseWNqSVhaVnRSMVNXTjBodmNoQWEwbHFlNXJ3Z3dfMTZrTFRtbS1QYk9UcEhTc1dZMm5uelFTODN2dFJFMVNNRExqLU9Ya3Y0YU8yb2ViMnA1emtBWlVmN3BQQ1ZoRmJMWU9EWjFyMUNPU0gweGVnR2dFcjhmVU5FYldqeGR6V0MxYm1tWUJQQnppdmpVaXFtNjFzWWZkZ2xON200ZU1PeGs2Smo2R1gtdmhpbGpLU3dRIiwNCiAgICAiZSI6ICJBUUFCIiwNCiAgICAidXNlIjogInNpZyIsDQogICAgImtpZCI6ICJ0ZXN0LXNwcmluZy1zZWN1cml0eSIsDQogICAgInFpIjogIjN4T19iSF9TN2l6NjB6TTBINmxKemlveThZdzhFc3JFY2JtLWF1YlVQclYyMl9zaVdzbEFGVWFRZXU3YkFHem5FSXQ1S0UzeDZnemJxaXJCTXVLVHdoaW9EVU44c3hpWVBmNE8tR0pIekd4ZWdPZ3kwMUM0ZHNlUlMtQnJ2djRZZHlHTjNIQVRkVmhBWE15WDl1ZUtOZV94ckJkRXZWZF9sNkc5R0VfanVpUSIsDQogICAgImRwIjogInlnTWtoUVZHSkZYWE5pWDJRa05DVUV0MWtFRGRCNm5xVEZYNkx4eUJMOGFxcndyRHRUbVJzX1I0V0JLRmxadHNJTDQybTh1WUp4TG96NG8yeXFUazNnTHhyaFI5Nl9OaF9vcVd1bzRrNEJBa2ZMY2QwQ2RJZ3VZNTEzU1Z4YnFsNE5qbmlMSDdDRXktRkRqVGQ4TzFxQTNKVGlyQTN6RlBIMjY3Z2FWZjRBayIsDQogICAgImFsZyI6ICJSUzUxMiIsDQogICAgImRxIjogIkZMdlNiTG53NEJYMGFRUks0cHFONGxSaU03bTZzYUFIQjJlWVhLWXJZNEM5UzJZSkk1cFg3OHlqZlh5dzdldTV6QWlCeTlEZXNTcHhFTkJOam96RjdUMzdGLWRxOEhhWEdLUG40TXhXdWxOSl9UYzhCazlrSDZCelhmUWVxcHNFZms1UXBfMDNYd1lERlVMNnJUV1ZDU1J6Z0JjQkZNeDRRSmFwcUd3a2pGRSIsDQogICAgIm4iOiAidE9pZzd4V1FIUDM4RnY0THZMd0o2ajUxb0d5M1o1eTNxYkRBbllNdlRUMFJSV2pwS1h2NlNFYl9XRHF6RTNkel9hYy1tWk1IdTZBNjRIb2owVGN1M1hicV9VQUhnWlBZNlpJeGRKTUJMdENnTEphYjlVNHR5cDJFUUtDWTE5RHRYdmhaV1p3OG0tQXVzMXFpOVF0WGRHMzhzMzQxQmI4OU9KazZuajdhNndIa3BwbmF0R2xpeGJXLW91a1YxdTVwZFJlWU9nb1drTFViUk1XeFZyZzRjSXM4aGljU3E1cGp5aXVlZ0RHb1g3dHFYQld6UUtZcnM4MXRCRXBfQkN3WTdKSUxheUJ2QkxjemJFaG1mYTlpRkxDYjQ1ZU9WSU9sLVY2WFkxNHJlTzNXb190ZmxsSmR0UmZsWXhZVEJpSEhzSjVYNFdrdUI2N2pmNG5HbGVHRFp3Ig0KfQ=="
        logger.debug("Decoding signature key: `$key`")
        RSAKey.parse(String(Base64Utils.decode(key.toByteArray())))
    }

    override fun decode(token: String): Mono<Jwt> = Mono
        .just(token)
        .doOnNext { tk -> logger.debug("Decoding jwe $tk") }
        .flatMap { tk -> extractJwe(tk) }
        .flatMap { jwe ->
            if (Date().after(jwe.jwtClaimsSet.expirationTime)) {
                Mono.error(Exception("NotGood"))
            } else {
                createSpringJwt(jwe)
            }
        }
        .doOnError { exception ->
            logger.debug("Unable to finish the decoding jwe with token: $token due to:", exception)
        }

    private fun extractJwe(token: String) = Mono
        .just(token)
        .flatMap {
            try {
                val jwe = JWEObject.parse(token)
                jwe.decrypt(RSADecrypter(encryptionKey))

                val signedJWT = jwe.payload?.toSignedJWT()

                if (signedJWT == null) {
                    Mono.error(JOSEException("No signed JWT found"))
                } else {
                    signedJWT.verify(RSASSAVerifier(signatureKey))
                    Mono.just(signedJWT)
                }
            } catch (ex: Exception) {
                when (ex) {
                    is ParseException,
                    is JOSEException -> {
                        logger.error("Invalid JWT signature: ${ex.message}")
                        Mono.error(Exception())
                    }
                    else -> Mono.error(ex)
                }
            }
        }

    private fun createSpringJwt(parsedJwt: JWT) = Mono
        .create<Jwt> { emitter ->
            try {
                val headers = LinkedHashMap(parsedJwt.header.toJSONObject())
                val claims = claimSetConverter.convert(parsedJwt.jwtClaimsSet.claims) ?: emptyMap()
                emitter
                    .success(
                        AuthenticationToken(
                            tokenValue = parsedJwt.parsedString,
                            headers = headers,
                            claims = claims
                        )
                    )
            } catch (ex: Exception) {
                emitter.error(BadJwtException("Unable to convert the JWE to spring JWT: ${ex.message}", ex))
            }
        }
}
