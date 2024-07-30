package kr.gracelove.openapidiffdemo.interfaces

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/external/users")
class ExternalUserController {

    @GetMapping
    fun getExternalUser(): ExternalUser {

        return ExternalUser("external-grace", 20)
    }

    @PostMapping
    fun createExternalUser(user: ExternalUser): ExternalUser {
        return user
    }

    data class ExternalUser(
        val name: String,
        val age: Int
    )


}