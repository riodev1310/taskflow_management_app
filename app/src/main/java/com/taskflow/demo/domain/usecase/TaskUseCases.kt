package com.taskflow.demo.domain.usecase

import com.taskflow.demo.data.repository.TaskRepository
import com.taskflow.demo.domain.model.CreateTaskRequest
import com.taskflow.demo.domain.model.Task
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(request: CreateTaskRequest): Task = taskRepository.createTask(request)
}

class UpdateTaskStatusUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, status: String) = taskRepository.updateStatus(taskId, status)
}
