package daniil.akifev.eventsourcing

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals

@SpringBootTest
class RegistrationServiceTest {
    @Autowired
    lateinit var controller: EventController

    @Test
    fun testEnter() {
        val aId = controller.createAccount()
        assert(!controller.enter(aId)) // not enough days
        assert(!controller.enter(aId + 1)) // unregistered account

        controller.addDays(aId, 1)
        assert(controller.enter(aId))
        val event = controller.getLastEvent()
        assertEquals(aId, event.accountId)
        assertEquals(EventType.ENTER, event.type)
    }

    @Test
    fun testLeave() {
        val aId = controller.createAccount()
        controller.enter(aId)
        controller.leave(aId)
        val event = controller.getLastEvent()
        assertEquals(aId, event.accountId)
        assertEquals(EventType.LEAVE, event.type)
    }
}