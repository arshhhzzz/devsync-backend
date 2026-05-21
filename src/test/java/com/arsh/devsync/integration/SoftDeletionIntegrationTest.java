package com.arsh.devsync.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SoftDeleteIntegrationTest extends BaseIntegrationTest {

    @Test
    void deleteProject_shouldHideProjectAndTask_thenRestoreBoth() throws Exception {
        String token = signupAndGetAccessToken(uniqueEmail("project-soft"));

        Long workspaceId = createWorkspace(token);
        Long projectId = createProject(token, workspaceId);
        Long taskId = createTask(token, projectId);

        mockMvc.perform(delete("/projects/" + projectId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/projects/" + projectId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/tasks/" + taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/projects/" + projectId + "/restore")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/projects/" + projectId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tasks/" + taskId + "/restore")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tasks/" + taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void restoreTask_shouldFail_whenProjectIsStillDeleted() throws Exception {
        String token = signupAndGetAccessToken(uniqueEmail("task-soft"));

        Long workspaceId = createWorkspace(token);
        Long projectId = createProject(token, workspaceId);
        Long taskId = createTask(token, projectId);

        mockMvc.perform(delete("/projects/" + projectId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(post("/tasks/" + taskId + "/restore")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteWorkspace_shouldHideWorkspaceProjectAndTask_thenRestoreWorkspaceOnly() throws Exception {
        String token = signupAndGetAccessToken(uniqueEmail("workspace-soft"));

        Long workspaceId = createWorkspace(token);
        Long projectId = createProject(token, workspaceId);
        Long taskId = createTask(token, projectId);

        mockMvc.perform(delete("/workspaces/" + workspaceId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/workspaces/" + workspaceId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/projects/" + projectId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/tasks/" + taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/workspaces/" + workspaceId + "/restore")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/workspaces/" + workspaceId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        // Workspace restore should not automatically restore child project/task.
        mockMvc.perform(get("/projects/" + projectId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/tasks/" + taskId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    private String signupAndGetAccessToken(String email) throws Exception {
        String response = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Soft Delete User",
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return toJson(response).get("accessToken").asText();
    }

    private Long createWorkspace(String token) throws Exception {
        String response = mockMvc.perform(post("/workspaces")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Soft Delete Workspace",
                                  "description": "Testing soft delete"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return toJson(response).get("id").asLong();
    }

    private Long createProject(String token, Long workspaceId) throws Exception {
        String response = mockMvc.perform(post("/workspaces/" + workspaceId + "/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Soft Delete Project",
                                  "description": "Testing project soft delete"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return toJson(response).get("id").asLong();
    }

    private Long createTask(String token, Long projectId) throws Exception {
        String response = mockMvc.perform(post("/projects/" + projectId + "/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Soft Delete Task",
                                  "description": "Testing task soft delete",
                                  "status": "TODO",
                                  "priority": "HIGH",
                                  "dueDate": "2026-06-01",
                                  "assigneeId": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return toJson(response).get("id").asLong();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.com";
    }
}