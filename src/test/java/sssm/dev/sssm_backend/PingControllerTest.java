package sssm.dev.sssm_backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sssm.dev.sssm_backend.controller_test.PingController;

class PingControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new PingController()).build();
  }

  @Test
  void pingReturnsPong() throws Exception {
    mockMvc.perform(get("/ping")).andExpect(status().isOk()).andExpect(content().string("pong"));
  }
}
