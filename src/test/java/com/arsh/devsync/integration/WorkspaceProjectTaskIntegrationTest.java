package com.arsh.devsync.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkspaceProjectTaskIntegrationTest extends BaseIntegrationTest {

    @Test
    void workspaceProjectTaskFlow_shouldWorkEndToEnd() throws Exception {
        String ownerToken = signupAndGetAccessToken(
                "Owner",
                "owner.integration@test.com"
        );

        String memberToken = signupAndGetAccessToken(
                "Member",
                "member.integration@test.com"
        );

        Long workspaceId = createWorkspace(ownerToken);
        addMember(ownerToken, workspaceId, "member.integration@test.com", "MEMBER");

        Long projectId = createProject(ownerToken, workspaceId);

        Long taskId = createTask(ownerToken, projectId, null);

        mockMvc.perform(get("/tasks/" + taskId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tasks/" + taskId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk());
    }

    @Test
    void memberShouldNotCreateProject() throws Exception {
        String ownerToken = signupAndGetAccessToken(
                "Owner Two",
                "owner2.integration@test.com"
        );

        String memberToken = signupAndGetAccessToken(
                "Member Two",
                "member2.integration@test.com"
        );

        Long workspaceId = createWorkspace(ownerToken);
        addMember(ownerToken, workspaceId, "member2.integration@test.com", "MEMBER");

        mockMvc.perform(post("/workspaces/" + workspaceId + "/projects")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Forbidden Project",
                                  "description": "Member should not create this"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private String signupAndGetAccessToken(String name, String email) throws Exception {
        String response = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(name, email)))
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
                                  "name": "DevSync Workspace",
                                  "description": "Integration test workspace"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return toJson(response).get("id").asLong();
    }

    private void addMember(
            String ownerToken,
            Long workspaceId,
            String memberEmail,
            String role
    ) throws Exception {
        mockMvc.perform(post("/workspaces/" + workspaceId + "/members")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "role": "%s"
                                }
                                """.formatted(memberEmail, role)))
                .andExpect(status().isCreated());
    }

    private Long createProject(String token, Long workspaceId) throws Exception {
        String response = mockMvc.perform(post("/workspaces/" + workspaceId + "/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Backend API",
                                  "description": "Spring Boot backend"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return toJson(response).get("id").asLong();
    }

    private Long createTask(
            String token,
            Long projectId,
            Long assigneeId
    ) throws Exception {
        String assigneeField = assigneeId == null
                ? "null"
                : assigneeId.toString();

        String response = mockMvc.perform(post("/projects/" + projectId + "/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Build task API",
                                  "description": "Create task integration test",
                                  "status": "TODO",
                                  "priority": "HIGH",
                                  "dueDate": "2026-06-01",
                                  "assigneeId": %s
                                }
                                """.formatted(assigneeField)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return toJson(response).get("id").asLong();
    }
}