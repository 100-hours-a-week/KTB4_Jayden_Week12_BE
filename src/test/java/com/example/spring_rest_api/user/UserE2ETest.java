package com.example.spring_rest_api.user;

import com.example.spring_rest_api.authorization.service.request.LoginRequest;
import com.example.spring_rest_api.authorization.service.response.LoginResponse;
import com.example.spring_rest_api.common.response.ApiResponse;
import com.example.spring_rest_api.image.service.response.ImageFileUploadResponse;
import com.example.spring_rest_api.user.repository.UserRepository;
import com.example.spring_rest_api.user.service.request.UserCreateRequest;
import com.example.spring_rest_api.user.service.request.UserUpdateInfoRequest;
import com.example.spring_rest_api.user.service.request.UserUpdatePasswordRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserE2ETest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

    private static final Path TEST_UPLOAD_ROOT = createTestUploadDirectory();

    private static Path createTestUploadDirectory() {
        try {
            return Files.createTempDirectory("spring-rest-api-upload-test-");
        } catch (IOException exception) {
            throw new IllegalStateException("테스트 업로드 디렉토리 생성 실패", exception);
        }
    }

    @DynamicPropertySource
    static void configureUploadDirectory(DynamicPropertyRegistry registry) {
        registry.add("app.upload.root", TEST_UPLOAD_ROOT::toString);
    }

    @BeforeEach
    void setup() throws Exception{
        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                new ClassPathResource("images/test-image.png").getInputStream()
        );

        MvcResult result = mockMvc.perform(
                        multipart("/users/me/profile-image")
                                .file(profileImage)
                )
                .andReturn();

        String responseBody = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<ImageFileUploadResponse> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});
        String fileUrl = apiResponse.getData().getFileUrl();

        UserCreateRequest request = new UserCreateRequest(
                "before@abc.com",
                "Abc1234!",
                "before",
                fileUrl
        );

        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );
    }

    @AfterEach
    void cleanUpUploadedFiles() throws IOException {
        FileSystemUtils.deleteRecursively(TEST_UPLOAD_ROOT);
        Files.createDirectories(TEST_UPLOAD_ROOT);
    }

    @AfterAll
    static void deleteTestUploadDirectory() throws IOException {
        FileSystemUtils.deleteRecursively(TEST_UPLOAD_ROOT);
    }

    @Test
    @DisplayName("회원가입 성공")
    void signupSuccessTest() throws Exception {
        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                new ClassPathResource("images/test-image.png").getInputStream()
        );

        MvcResult result = mockMvc.perform(
                        multipart("/users/me/profile-image")
                                .file(profileImage)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("profile_image_upload_success"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<ImageFileUploadResponse> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});
        String fileUrl = apiResponse.getData().getFileUrl();

        UserCreateRequest request = new UserCreateRequest(
                "email@abc.com",
                "Abc1234!",
                "e2etest",
                fileUrl
        );

        mockMvc.perform(
                        post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("register_success"))
                .andExpect(jsonPath("$.data.email").value(request.getEmail()))
                .andExpect(jsonPath("$.data.nickname").value(request.getNickname()))
                .andExpect(jsonPath("$.data.profileImageUrl").value(request.getProfileImageUrl()));

        assertThat(userRepository.findByEmail(request.getEmail())).isPresent();

    }

    @Test
    @DisplayName("로그인 - 회원 조회 - 정보 수정 - 비밀번호 수정 - 탈퇴 성공")
    void updateScenarioSuccessTest() throws Exception {
        //login
        LoginRequest loginRequest = new LoginRequest(
                "before@abc.com",
                "Abc1234!"
        );

        MvcResult loginResult = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("login_success"))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<LoginResponse> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});
        String token = apiResponse.getData().getToken().getAccessToken();

        //read
        mockMvc.perform(
                        get("/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("get_user_info_success"))
                .andExpect(jsonPath("$.data.email").value(loginRequest.getEmail()));

        assertThat(userRepository.findByEmail(loginRequest.getEmail())).isPresent();


        //updateInformation
        UserUpdateInfoRequest updateInfoRequest = new UserUpdateInfoRequest(
                "change",
                null
        );

        mockMvc.perform(
                patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateInfoRequest))
                        .header("Authorization", "Bearer " + token)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("user_info_update_success"))
                .andExpect(jsonPath("$.data.email").value(loginRequest.getEmail()))
                .andExpect(jsonPath("$.data.nickname").value(updateInfoRequest.getNickname()))
                .andExpect(jsonPath("$.data.profileImageUrl").isEmpty());

        assertThat(userRepository.findByEmail(loginRequest.getEmail()).orElseThrow().getNickname()).isEqualTo(updateInfoRequest.getNickname());

        //updatePassword
        UserUpdatePasswordRequest updatePasswordRequest = new UserUpdatePasswordRequest(
                "Aabb1234!"
        );

        mockMvc.perform(
                        patch("/users/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updatePasswordRequest))
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("user_password_update_success"))
                .andExpect(jsonPath("$.data.email").value(loginRequest.getEmail()));

        //delete
        mockMvc.perform(
                        delete("/users/me")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("user_delete_success"))
                .andExpect(jsonPath("$.data.deletedAt").isNotEmpty());
    }

    @Test
    @DisplayName("로그인 - 로그아웃 성공")
    void logoutScenarioSuccessTest() throws Exception {
        //login
        LoginRequest loginRequest = new LoginRequest(
                "before@abc.com",
                "Abc1234!"
        );

        MvcResult loginResult = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("login_success"))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<LoginResponse> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});
        String token = apiResponse.getData().getToken().getAccessToken();

        Cookie[] loginCookies = loginResult.getResponse().getCookies();
        System.out.println("loginCookies = " + Arrays.toString(loginCookies));

        //logout
        MvcResult logoutResult = mockMvc.perform(
                        post("/auth/logout")
                                .header("Authorization", "Bearer " + token)
                                .cookie(loginCookies)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("logout_success"))
                .andReturn();

        Cookie refreshToken = logoutResult.getResponse().getCookie("refreshToken");

        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.getValue()).isBlank();
    }
}
