package top.mill.kchat.service

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import top.mill.kchat.UUIDManager
import top.mill.kchat.contacts.User
import top.mill.kchat.database.DatabaseManager
import top.mill.kchat.database.UserSchema
import top.mill.kchat.logger
import top.mill.kchat.network.Client
import java.net.InetAddress

// Local invoke -> Request from client to broadcast & Storage data
// Network invoke -> Storage data
class UserService {
    private val logger = logger("Application")

    private val localUUID = UUIDManager.getUUIDString()

    suspend fun create(user: User): String {
        logger.info { "User ${user.name} created." }

        onLocalUser(user.id) {
            Client.broadcastPostRequest(path = "user", body = user)
        }
        val userSchema = UserSchema(DatabaseManager.getDatabase())
        if (userSchema.getByUUID(user.id) == null) {
            return userSchema.create(user)
        } else throw Exception("User ${user.name} already exists.")
    }

    suspend fun login(user: User) {
        logger.info { "User ${user.name} logged in." }
        onLocalUser(user.id) {
            Client.broadcastPutRequest(path = "user/login", body = user)
        }
    }

    fun logout() {
        TODO("Logout")
    }

    fun updateInfo() {
        TODO("Update information of user")
    }

    fun deleteUser() {
        TODO("Delete a user")
    }

    private inline fun onLocalUser(uuid: String, block: () -> Unit) {
        if (localUUID == uuid) block()
    }
}

fun Route.usersRoute(database: Database) {
    val service = UserService()
    route("/user") {
        get("/id/{id}") {
            TODO("Get user by ID")
        }

        get("/name/{name}") {
            TODO("Get users by Name")
        }

        post {
            try {
                val user = call.receive<User>()
                val uuid = service.create(user)
                val remoteAddress = call.receive<InetAddress>()

                Client.updateUUID(uuid, remoteAddress)
                call.respondText(text = "User $uuid from $remoteAddress created", status = HttpStatusCode.Created)
            } catch (_: ContentTransformationException) {
                call.respondText(text = "Failed to parse User", status = HttpStatusCode.BadRequest)
            } catch (_: Exception) {
                call.respondText(text = "Failed to create User", status = HttpStatusCode.InternalServerError)
            }
        }

        put("/name") {
            TODO("Update User Name by ID")
        }

        put("/login") {
            val user = call.receive<User>()
            service.login(user)
        }

        delete("/{id}") {
            TODO("Delete user by ID")
        }
    }
}