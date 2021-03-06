package daniil.akifev.eventsourcing

import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.annotations.TestOnly
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*
import java.time.Duration

@RestController
@RequestMapping("/")
class EventController {
    @Autowired
    lateinit var eventRepository: EventRepository

    /** Admin service */

    private var maxAccountId = 1

    // Command

    @PostMapping("/admin/accounts/create")
    fun createAccount(): Int {
        val accountId = maxAccountId++
        eventRepository.save(Event(accountId = accountId, type = EventType.CREATE_ACCOUNT))
        return accountId
    }

    @PostMapping("/admin/accounts/{accountId}/add")
    fun addDays(@PathVariable accountId: Int, @RequestParam days: Int) {
        val data = """{ "days": $days }"""
        eventRepository.save(Event(accountId = accountId, type = EventType.ADD_DAYS, data = data))
    }

    // Query

    @GetMapping("/admin/accounts/{accountId}/days")
    fun getDaysLeft(@PathVariable accountId: Int): Int? {
        val events = eventRepository.findAllByAccountId(accountId)
        if (events.isEmpty()) {
            return null
        }
        var daysLeft = 0
        val addDaysEvents = events.filter { it.type == EventType.ADD_DAYS }
        for (addDaysEvent in addDaysEvents) {
            val jsonData = addDaysEvent.data
            val days = ObjectMapper().readTree(jsonData).get("days").intValue()
            daysLeft += days
        }

        return daysLeft
    }

    /** Registration service */

    // Command

    @PostMapping("/registration/{accountId}/enter")
    fun enter(@PathVariable accountId: Int): Boolean {
        val daysLeft = getDaysLeft(accountId)
        if (daysLeft == null) {
            println("unregistered account")
            return false
        }
        require(daysLeft >= 0)
        if (daysLeft == 0) {
            println("not enough days")
            return false
        }
        eventRepository.save(Event(accountId = accountId, type = EventType.ENTER))
        return true
    }

    @PostMapping("/registration/{accountId}/leave")
    fun leave(@PathVariable accountId: Int) {
        eventRepository.save(Event(accountId = accountId, type = EventType.LEAVE))
    }

    /** Statistics service */

    private var position = 0
    private val registrationEvents = mutableListOf<Event>()

    private fun getRawRegistrationEvents(): List<Event> {
        val rawEvents = eventRepository.findAllByIdAfter(position)
        position += rawEvents.size
        return rawEvents.filter { it.type == EventType.ENTER || it.type == EventType.LEAVE }
    }

    // Query

    @GetMapping("/statistics/day")
    fun printStatisticsPerDay() {
        registrationEvents.addAll(getRawRegistrationEvents())

        println("***********************")
        println("PER DAY STATISTICS:")
        for (entry in registrationEvents.groupBy { it.timestamp / Duration.ofDays(1).toMillis() }) {
            for (event in entry.value) {
                println(event)
            }
            println()
        }
        println("***********************")
    }

    @GetMapping("/statistics/avg")
    fun printAvgStatistics() {
        registrationEvents.addAll(getRawRegistrationEvents())

        val averageFrequency = getAverageFrequency(registrationEvents)
        val averageDuration = getAverageDuration(registrationEvents)

        println("***********************")
        println("AVG STATISTICS:")
        println("Average frequency: $averageFrequency")
        println("Average duration: $averageDuration")
        println("***********************")
    }

    private fun getAverageFrequency(registrationEvents: MutableList<Event>): Double {
        val entering = registrationEvents.filter { it.type == EventType.ENTER }
        val groupedEvents = registrationEvents.groupBy { it.timestamp / Duration.ofDays(1).toMillis() }
        return entering.size.toDouble() / groupedEvents.size
    }

    private fun getAverageDuration(registrationEvents: MutableList<Event>): Double {
        val accountEvents = registrationEvents.groupBy { it.accountId }
        var cnt = 0
        var sum: Long = 0
        for (entry in accountEvents) {
            var left: Long = 0
            var right: Long = 0
            for (event in entry.value) {
                if (event.type == EventType.ENTER) {
                    left = event.timestamp
                }
                if (event.type == EventType.LEAVE) {
                    right = event.timestamp
                    sum += right - left
                    cnt++
                }
            }
        }

        return sum.toDouble() / cnt
    }
}

@TestOnly
internal fun EventController.getLastEvent(): Event = eventRepository.findAll(Sort.by("id")).last()

@TestOnly
internal fun <R> EventController.withTimestamp(timestamp: Long, block: EventController.() -> R): R {
    val sizeBefore = eventRepository.count()
    val result = block()
    val sizeAfter = eventRepository.count()

    val newEvents = eventRepository.findAll(Sort.by("id")).takeLast((sizeAfter - sizeBefore).toInt())

    for (newEvent in newEvents) {
        eventRepository.setTimestampById(newEvent.id, timestamp)
    }

    return result
}
