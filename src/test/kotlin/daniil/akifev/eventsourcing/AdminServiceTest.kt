package daniil.akifev.eventsourcing

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals

@SpringBootTest
class AdminServiceTest {
    @Autowired
    lateinit var controller: EventController

    @Test
    fun testCreateAccount() {
        val aId = controller.createAccount()
        assertEquals(0, controller.getDaysLeft(aId))
        val event = controller.getLastEvent()
        assertEquals(aId, event.accountId)
        assertEquals(EventType.CREATE_ACCOUNT, event.type)
    }

    @Test
    fun testAddDays() {
        val aId = controller.createAccount()
        assertEquals(0, controller.getDaysLeft(aId))
        controller.addDays(aId, 5)
        assertEquals(5, controller.getDaysLeft(aId))
        var event = controller.getLastEvent()
        assertEquals(aId, event.accountId)
        assertEquals(EventType.ADD_DAYS, event.type)
        assertEquals("""{ "days": 5 }""", event.data)
        controller.addDays(aId, 2)
        assertEquals(7, controller.getDaysLeft(aId))
        event = controller.getLastEvent()
        assertEquals(aId, event.accountId)
        assertEquals(EventType.ADD_DAYS, event.type)
        assertEquals("""{ "days": 2 }""", event.data)
    }

    @Test
    fun testGetDaysLeft() {
        val aId = controller.createAccount()
        assertEquals(0, controller.getDaysLeft(aId))
        controller.addDays(aId, 5)
        assertEquals(5, controller.getDaysLeft(aId))
    }
}