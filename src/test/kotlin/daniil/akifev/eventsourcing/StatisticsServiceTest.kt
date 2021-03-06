package daniil.akifev.eventsourcing

import org.aspectj.lang.annotation.Before
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.event.annotation.BeforeTestClass
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals


@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatisticsServiceTest {
    @Autowired
    lateinit var controller: EventController

    private val times = mutableListOf<Long>()

    @BeforeAll
    fun initialize() {
        val aId = controller.createAccount()
        controller.addDays(aId, 5)

        val day = Duration.ofDays(1).toMillis()
        val hour = Duration.ofHours(1).toMillis()

        val time = (Date().time / day) * day
        times.add(time)
        times.add(times.last() + hour)
        times.add(times.last() + day)
        times.add(times.last() + 2 * hour)

        controller.withTimestamp(times[0]) { controller.enter(aId) }
        controller.withTimestamp(times[1]) { controller.leave(aId) }
        controller.withTimestamp(times[2]) { controller.enter(aId) }
        controller.withTimestamp(times[3]) { controller.leave(aId) }
    }

    @Test
    fun testPrintStatisticsPerDay() {
        val output = ByteArrayOutputStream()
        System.setOut(PrintStream(output))
        controller.printStatisticsPerDay()
        System.setOut(System.out)

        val expected = """
            ***********************
            PER DAY STATISTICS:
            accountId = 1, type = ENTER, timestamp: ${times[0]}
            accountId = 1, type = LEAVE, timestamp: ${times[1]}

            accountId = 1, type = ENTER, timestamp: ${times[2]}
            accountId = 1, type = LEAVE, timestamp: ${times[3]}

            ***********************
        """.trimIndent()

        assertEquals(expected, output.toString().trim())
    }

    @Test
    fun testPrintAvgStatistics() {
        val output = ByteArrayOutputStream()
        System.setOut(PrintStream(output))
        controller.printAvgStatistics()
        System.setOut(System.out)

        val expected = """
            ***********************
            AVG STATISTICS:
            Average frequency: 1.0
            Average duration: 5400000.0
            ***********************
        """.trimIndent()

        assertEquals(expected, output.toString().trim())
    }
}

