package kr.gracelove.openapidiffdemo.`interface`

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController {

    @GetMapping
    fun getUser(): User {
        return User("grace", 20)
    }

    @PostMapping
    fun createUser(user: User): User {
        return user
    }


    data class User(
        val name: String,
        val age: Int
    )
}