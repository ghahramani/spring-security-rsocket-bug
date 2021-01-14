package com.example.demo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveFlux
import org.springframework.util.MimeType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.Serializable

@RestController
@RequestMapping
class TestResource {

    @Autowired
    private lateinit var requester: Mono<RSocketRequester>


    @Autowired
    private lateinit var webClient: WebClient

    @PostMapping("/api/v2/uploads.json", consumes = [MediaType.ALL_VALUE])
    fun aa(@RequestParam filename: String, @RequestBody(required = false) data: Flux<DataBuffer>?) =
        data?.flatMap {
            println("New Data: ${it.toString(Charsets.UTF_8)}")
            Mono.create<Result> { emitter ->
                println("Filename: $filename")
                emitter.success(Result(listOf(Attachment(20, filename))))
            }
        }?.next()

    @MessageMapping("test")
    fun aa(@Payload @Validated data: Flux<DataBuffer>): Mono<String> =
        webClient
            .post()
            .uri(
                UriComponentsBuilder
                    .fromUriString("http://localhost:9999/api/v2/uploads.json")
                    .queryParam("filename", "TestFileName")
                    .build(true)
                    .toUri()
            )
            .contentType(MediaType("application", "binary"))
            .body(data, DataBuffer::class.java)
            .exchangeToMono { response ->
                response.bodyToMono<String>()
            }

    @GetMapping("/test-upload")
    fun test() = requester
        .map { client ->
            client
                .route("upload.file")
                .metadata("test", MimeType.valueOf("message/x.file.name"))
                .data(Flux.just("data", "test", "dat2"))
        }
        .flatMapMany { client -> client.retrieveFlux<UploadResponseVM>() }


}

data class UploadResponseVM(val attachmentId: String) : Serializable
data class Attachment(val id: Long, val fileName: String)
data class Result(val attachments: List<Attachment>)
