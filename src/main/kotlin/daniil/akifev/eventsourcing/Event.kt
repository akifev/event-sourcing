package daniil.akifev.eventsourcing

import java.util.*
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    val accountId: Int,
    val type: EventType,
    val data: String? = null,
    val timestamp: Long = Date().time,
) {
    override fun toString(): String {
        return "accountId = $accountId, type = $type, timestamp: $timestamp"
    }
}