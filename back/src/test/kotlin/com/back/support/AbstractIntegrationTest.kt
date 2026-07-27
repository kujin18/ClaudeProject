package com.back.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIntegrationTest {

    companion object {
        // Manually-managed singleton: started once for the whole test JVM and never
        // explicitly stopped (Testcontainers' Ryuk reaper cleans it up on JVM exit).
        // Using @Testcontainers/@Container here would let each test class's own
        // afterAll callback stop this shared container as soon as that class finishes,
        // breaking every test class that runs afterward.
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine").also { it.start() }
    }
}
