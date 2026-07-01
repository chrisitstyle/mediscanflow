package com.chrisitstyle.mediscanflow.medicalplatform.system;

import com.chrisitstyle.mediscanflow.medicalplatform.system.dto.SystemComponentStatusDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.system.dto.SystemStatusResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemStatusController.class)
class SystemStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemStatusService systemStatusService;

    @Test
    void getStatusReturnsAllComponentsAsUp() throws Exception {
        when(systemStatusService.getStatus())
                .thenReturn(statusResponse(
                        "UP",
                        "UP",
                        "UP",
                        "UP",
                        "UP"
                ));

        mockMvc.perform(get("/api/system/status").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.rabbitmq.status").value("UP"))
                .andExpect(jsonPath("$.components.minio.status").value("UP"))
                .andExpect(jsonPath("$.components.aiWorker.status").value("UP"));
    }

    @Test
    void getStatusReturnsPartialDownStatus() throws Exception {
        when(systemStatusService.getStatus())
                .thenReturn(statusResponse(
                        "DOWN",
                        "UP",
                        "DOWN",
                        "UP",
                        "DOWN"
                ));

        mockMvc.perform(get("/api/system/status").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.rabbitmq.status").value("DOWN"))
                .andExpect(jsonPath("$.components.minio.status").value("UP"))
                .andExpect(jsonPath("$.components.aiWorker.status").value("DOWN"));
    }

    private static SystemStatusResponseDTO statusResponse(
            String overallStatus,
            String databaseStatus,
            String rabbitmqStatus,
            String minioStatus,
            String aiWorkerStatus
    ) {
        Map<String, SystemComponentStatusDTO> components = new LinkedHashMap<>();
        components.put("database", new SystemComponentStatusDTO(databaseStatus));
        components.put("rabbitmq", new SystemComponentStatusDTO(rabbitmqStatus));
        components.put("minio", new SystemComponentStatusDTO(minioStatus));
        components.put("aiWorker", new SystemComponentStatusDTO(aiWorkerStatus));

        return new SystemStatusResponseDTO(overallStatus, components);
    }
}
