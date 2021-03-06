package daniil.akifev.eventsourcing

import org.jetbrains.annotations.TestOnly
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface EventRepository : JpaRepository<Event, Int> {
    fun findAllByAccountId(accountId: Int): List<Event>
    fun findAllByIdAfter(id: Int): List<Event>

    @TestOnly
    @Modifying
    @Transactional
    @Query(value = "update event set timestamp = ?2 where id = ?1", nativeQuery = true)
    fun setTimestampById(id: Int, timestamp: Long)
}