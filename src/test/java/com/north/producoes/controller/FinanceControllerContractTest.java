package com.north.producoes.controller;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.entity.enums.FinanceTypeEnum;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.FinanceRepository;
import com.north.producoes.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de rotas do financeiro — pinna método HTTP + path + formato do
 * payload que o frontend consome. A tela de financeiro já quebrou em
 * produção por deriva silenciosa deste contrato (405 em rota renomeada);
 * se este teste falhar, o financeService.ts do frontend precisa mudar junto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Contrato de rotas do financeiro")
class FinanceControllerContractTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ClientRepository clientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FinanceRepository financeRepository;

    private UserEntity admin;
    private Long clientId;
    private Long financeId;

    @BeforeEach
    void seed() {
        admin = new UserEntity();
        admin.setName("Admin Financeiro");
        admin.setEmail("finance-" + System.nanoTime() + "@example.com");
        admin.setPassword("secret");
        admin.setRole(UserRoleEnum.ADMIN);
        admin = userRepository.save(admin);

        ClientEntity client = new ClientEntity();
        client.setName("Cliente Financeiro");
        client.setNumber("55" + System.nanoTime());
        client.setNiche("Saude");
        client.setVoiceTone("Formal");
        clientId = clientRepository.save(client).getId();

        FinanceEntity entry = new FinanceEntity();
        entry.setClient(clientRepository.findById(clientId).orElseThrow());
        entry.setUser(admin);
        entry.setDescription("Mensalidade contrato");
        entry.setValue(BigDecimal.valueOf(1200));
        entry.setStatus(FinanceStatusEnum.PENDING);
        entry.setType(FinanceTypeEnum.VARIABLE_REVENUE);
        entry.setExpirationDate(LocalDateTime.of(2026, 8, 5, 0, 0));
        entry.setPaymentDate(LocalDateTime.of(2026, 8, 5, 0, 0));
        financeId = financeRepository.save(entry).getId();
    }

    @AfterEach
    void cleanup() {
        // deleteAll (não-bulk) é transacional por padrão — o bulk
        // deleteByClientId exigiria transação externa no @AfterEach
        financeRepository.deleteAll(financeRepository.findByClientId(clientId));
        clientRepository.deleteById(clientId);
        userRepository.deleteById(admin.getId());
    }

    @Test
    @DisplayName("POST /api/finance cria registro (201) — rota usada pelo frontend")
    void postCreatesFinance() throws Exception {
        mockMvc.perform(post("/api/finance")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": %d, "description": "Nova conta", "value": 500.00,
                                 "status": "PENDING", "type": "VARIABLE_REVENUE",
                                 "expirationDate": "2026-09-10", "paymentDate": null}
                                """.formatted(clientId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("PUT /api/finance/{id} atualiza registro (200) — usado para editar e receber conta")
    void putUpdatesFinance() throws Exception {
        mockMvc.perform(put("/api/finance/" + financeId)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": %d, "description": "Mensalidade contrato", "value": 1200,
                                 "status": "PAY", "type": "VARIABLE_REVENUE",
                                 "expirationDate": "2026-08-05", "paymentDate": "2026-08-01"}
                                """.formatted(clientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAY"))
                .andExpect(jsonPath("$.paymentDate").exists());
    }

    @Test
    @DisplayName("DELETE /api/finance/{id} remove registro (204)")
    void deleteRemovesFinance() throws Exception {
        mockMvc.perform(delete("/api/finance/" + financeId).with(user(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/finance aceita filtros opcionais de status e vencimento")
    void getSupportsFilters() throws Exception {
        mockMvc.perform(get("/api/finance")
                        .with(user(admin))
                        .param("status", "PENDING")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(financeId)).exists());

        // Fora do intervalo → não retorna o registro
        mockMvc.perform(get("/api/finance")
                        .with(user(admin))
                        .param("from", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(financeId)).doesNotExist());

        // Filtro de pagas não retorna a pendente
        mockMvc.perform(get("/api/finance")
                        .with(user(admin))
                        .param("status", "PAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(financeId)).doesNotExist());
    }

    @Test
    @DisplayName("resposta da listagem expõe os campos que o frontend consome")
    void listExposesFrontendContractFields() throws Exception {
        mockMvc.perform(get("/api/finance").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].clientId".formatted(financeId)).exists())
                .andExpect(jsonPath("$[?(@.id == %d)].expirationDate".formatted(financeId)).exists())
                .andExpect(jsonPath("$[?(@.id == %d)].paymentDate".formatted(financeId)).exists())
                .andExpect(jsonPath("$[?(@.id == %d)].status".formatted(financeId)).exists());
    }
}
