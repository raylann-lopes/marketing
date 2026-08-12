package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.TaskRequestDTO;
import com.north.producoes.controller.dto.request.TaskStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.response.TaskResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.TaskStatusEnum;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

public interface TaskApi {
    @GetMapping
    ResponseEntity<Page<TaskResponseDTO>> findTask(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) TaskStatusEnum status,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                   @AuthenticationPrincipal UserEntity currentUser);

    @PostMapping("/create")
    ResponseEntity<TaskResponseDTO> createTask(@Valid @RequestBody TaskRequestDTO request, @AuthenticationPrincipal UserEntity currentUser);

    @PatchMapping("update/status/{taskId}")
    ResponseEntity<TaskResponseDTO> updateTaskStatus(@PathVariable Long taskId, @RequestBody TaskStatusUpdateRequestDTO request);

    @DeleteMapping("delete/{taskId}")
    ResponseEntity<Void> deleteTask(@PathVariable Long taskId);
}
