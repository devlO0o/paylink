package com.paylink.paylink;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

// No Postgres in CI: turn off DataSource/JPA auto-config so the context starts without a DB.
@SpringBootTest(properties = "spring.autoconfigure.exclude="
    + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
    + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration")
@AutoConfigureMockMvc
class PaylinkApplicationTests {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void healthEndpointIsUp() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }
}
