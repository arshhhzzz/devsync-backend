package com.arsh.devsync.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentIntegrationTest extends BaseIntegrationTest {

    @Test
    void workspaceMember_shouldAddAndListTaskComments() throws Exception {
        String ownerEmail = uniqueEmail("comment-owner");
        String memberEmail = uniqueEmail("comment-member");

        String ownerToken = signupAndGetAccessToken("Owner", ownerEmail);
        String memberToken = signupAndGetAccessToken("Member", memberEmail);

        Long workspaceId = createWorkspace(ownerToken);
        addMember(ownerToken, workspaceId, memberEmail, "MEMBER");

        Long projectId = createProject(ownerToken, workspaceId);
        Long taskId = createTask(ownerToken, projectId);

        mockMvc.perform(post("/tasks/" + taskId + "/comments")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "This task needs more details."
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tasks/" + taskId + "/comments")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk());
    }

    @Test
    void workspaceMember_shouldAddAndListProjectComments() throws Exception {
        String ownerEmail = uniqueEmail("project-comment-owner");
        String memberEmail = uniqueEmail("project-comment-member");

        String ownerToken = signupAndGetAccessToken("Owner", ownerEmail);
        String memberToken = signupAndGetAccessToken("Member", memberEmail);

        Long workspaceId = createWorkspace(ownerToken);
        addMember(ownerToken, workspaceId, memberEmail, "MEMBER");

        Long projectId = createProject(ownerToken, workspaceId);

        mockMvc.perform(post("/projects/" + projectId + "/comments")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Project planning looks good."
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/projects/" + projectId + "/comments")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk());
    }

    @Test
    void outsider_shouldNotCommentOnTask() throws Exception {
        String ownerEmail = uniqueEmail("task-owner");
        String outsiderEmail = uniqueEmail("task-outsider");

        String ownerToken = signupAndGetAccessToken("Owner", ownerEmail);
        String outsiderToken = signupAndGetAccessToken("Outsider", outsiderEmail);

        Long workspaceId = createWorkspace(ownerToken);
        Long projectId = createProject(ownerToken, workspaceId);
        Long taskId = createTask(ownerToken, projectId);

        mockMvc.perform(post("/tasks/" + taskId + "/comments")
                        .header("Authorization", bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "I should not be allowed."
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void author_shouldUpdateOwnComment() throws Exception {
        String ownerEmail = uniqueEmail("update-comment-owner");

        String ownerToken = signupAndGetAccessToken("Owner", ownerEmail);

        Long workspaceId = createWorkspace(ownerToken);
        Long projectId = createProject(ownerToken, workspaceId);

        Long commentId = createProjectComment(
                ownerToken,
                projectId,
                "Original comment"
        );

        mockMvc.perform(put("/comments/" + commentId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Updated comment"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void nonAuthor_shouldNotUpdateComment() throws Exception {
        String ownerEmail = uniqueEmail("non-author-owner");
        String memberEmail = uniqueEmail("non-author-member");

        String ownerToken = signupAndGetAccessToken("Owner", ownerEmail);
        String memberToken = signupAndGetAccessToken("Member", memberEmail);

        Long workspaceId = createWorkspace(ownerToken);
        addMember(ownerToken, workspaceId, memberEmail, "MEMBER");

        Long projectId = createProject(ownerToken, workspaceId);

        Long commentId = createProjectComment(
                ownerToken,
                projectId,
                "Owner comment"
        );

        mockMvc.perform(put("/comments/" + commentId)
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Member tries to edit"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerOrAdmin_shouldDeleteAnyComment() throws Exception {
        String ownerEmail = uniqueEmail("delete-owner");
        String memberEmail = uniqueEmail("delete-member");

        String ownerToken = signupAndGetAccessToken("Owner", ownerEmail);
        String memberToken = signupAndGetAccessToken("Member", memberEmail);

        Long workspaceId = createWorkspace(ownerToken);
        addMember(ownerToken, workspaceId, memberEmail, "MEMBER");

        Long projectId = createProject(ownerToken, workspaceId);

        Long commentId = createProjectComment(
                memberToken,
                projectId,
                "Member comment"
        );

        mockMvc.perform(delete("/comments/" + commentId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
    }

    @Test
    void member_shouldNotDeleteOtherMembersComment() throws Exception {
        String ownerEmail = uniqueEmail("comment-owner");
        String memberOneEmail = uniqueEmail("comment-member-one");
        String memberTwoEmail = uniqueEmail("comment-member-two");

        String ownerToken = signupAndGetAccessToken("Owner", ownerEmail);
        String memberOneToken = signupAndGetAccessToken("Member One", memberOneEmail);
        String memberTwoToken = signupAndGetAccessToken("Member Two", memberTwoEmail);

        Long workspaceId = createWorkspace(ownerToken);
        addMember(ownerToken, workspaceId, memberOneEmail, "MEMBER");
        addMember(ownerToken, workspaceId, memberTwoEmail, "MEMBER");

        Long projectId = createProject(ownerToken, workspaceId);

        Long commentId = createProjectComment(
                memberOneToken,
                projectId,
                "Member one comment"
        );

        mockMvc.perform(delete("/comments/" + commentId)
                        .header("Authorization", bearer(memberTwoToken)))
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
                                  "name": "Comment Workspace",
                                  "description": "Workspace for comment tests"
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
                                  "name": "Comment Project",
                                  "description": "Project for comments"
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
                                  "title": "Comment Task",
                                  "description": "Task for comment tests",
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

    private Long createProjectComment(
            String token,
            Long projectId,
            String content
    ) throws Exception {
        String response = mockMvc.perform(post("/projects/" + projectId + "/comments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "%s"
                                }
                                """.formatted(content)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return toJson(response).get("id").asLong();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.com";
    }
}