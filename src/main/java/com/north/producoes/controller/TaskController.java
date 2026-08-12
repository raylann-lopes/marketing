package com.north.producoes.controller;

import com.north.producoes.controller.api.TaskApi;
import com.north.producoes.controller.dto.request.TaskRequestDTO;
import com.north.producoes.controller.dto.request.TaskStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.response.TaskResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.TaskStatusEnum;
import com.north.producoes.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("api/task")
public class TaskController implements TaskApi {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TaskResponseDTO>> findTask(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @RequestParam(required = false) TaskStatusEnum status,
                                                          @RequestParam(required = false)
                                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                          @RequestParam(required = false)
                                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                          @RequestParam(required = false)
                                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                          @AuthenticationPrincipal UserEntity currentUser) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 200)));
        LocalDate effectiveStartDate = startDate != null ? startDate : date;
        LocalDate effectiveEndDate = endDate != null ? endDate : date;
        return ResponseEntity.ok(taskService.listAllTask(
                currentUser,
                status,
                effectiveStartDate,
                effectiveEndDate,
                pageable));
    }

    @Override
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponseDTO> createTask(@Valid @RequestBody TaskRequestDTO request, @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(request, currentUser));
    }

    @Override
    @PatchMapping("update/status/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponseDTO> updateTaskStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskStatusUpdateRequestDTO request,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(taskService.updateTask(taskId, request, currentUser));
    }

    @Override
    @DeleteMapping("delete/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserEntity currentUser) {
        taskService.deleteTask(taskId, currentUser);
        return ResponseEntity.noContent().build();
    }


}
