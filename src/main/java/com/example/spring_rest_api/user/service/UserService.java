package com.example.spring_rest_api.user.service;

import com.example.spring_rest_api.common.exception.NotFoundException;
import com.example.spring_rest_api.common.exception.RequestConflictException;
import com.example.spring_rest_api.image.entity.ImageFile;
import com.example.spring_rest_api.image.entity.ImageStatus;
import com.example.spring_rest_api.image.repository.ImageFileRepository;
import com.example.spring_rest_api.image.util.ImageFileUtil;
import com.example.spring_rest_api.user.entity.User;
import com.example.spring_rest_api.user.repository.UserQueryRepository;
import com.example.spring_rest_api.user.repository.UserRepository;
import com.example.spring_rest_api.user.service.request.UserCreateRequest;
import com.example.spring_rest_api.user.service.request.UserUpdateInfoRequest;
import com.example.spring_rest_api.user.service.request.UserUpdatePasswordRequest;
import com.example.spring_rest_api.user.service.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserQueryRepository userQueryRepository;
    private final ImageFileRepository imageFileRepository;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RequestConflictException("이미 존재하는 이메일입니다.");
        }
        if (userRepository.findByNickname(request.getNickname()).isPresent()) {
            throw new RequestConflictException("이미 존재하는 닉네임입니다.");
        }

        ImageFile profileImage = resolveProfileImage(request.getProfileImageUrl());

        User saved = userRepository.save(User.create(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getNickname(),
                profileImage
        ));

        if (profileImage != null) {
            profileImage.changeAttachedStatus();
            profileImage.changeUploaderId(saved.getUserId());
        }

        return UserResponse.from(saved);
    }

    public UserResponse read(Long userId) {
        return UserResponse.from(
                userQueryRepository.findByIdWithProfileImage(userId)
                        .filter(u -> u.getDeletedAt() == null)
                        .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"))
        );
    }

    @Transactional
    public UserResponse updateInformation(Long userId, UserUpdateInfoRequest request) {
        User user = userQueryRepository.findByIdWithProfileImage(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));
        User userByNickname = userRepository.findByNickname(request.getNickname())
                .filter(u -> u.getDeletedAt() == null)
                .orElse(null);

        if (userByNickname != null && !userByNickname.getUserId().equals(userId)) {
            throw new RequestConflictException("이미 존재하는 닉네임입니다.");
        }

        ImageFile previousProfileImage = user.getProfileImage();

        user.updateInformation(
                request.getNickname(),
                resolveProfileImage(request.getProfileImageUrl())
        );

        ImageFile updatedImageFile = user.getProfileImage();
        if (previousProfileImage != null && !isSameImage(previousProfileImage, updatedImageFile)) {
            previousProfileImage.changeTempStatus();
        }
        if (updatedImageFile != null && updatedImageFile.getImageStatus() == ImageStatus.TEMP) {
            updatedImageFile.changeAttachedStatus();
            updatedImageFile.changeUploaderId(user.getUserId());
        }

        return UserResponse.from(user);
    }

    private boolean isSameImage(ImageFile previousProfileImage, ImageFile updatedImageFile) {
        if (previousProfileImage == updatedImageFile) {
            return true;
        }
        if (previousProfileImage == null || updatedImageFile == null) {
            return false;
        }
        return Objects.equals(previousProfileImage.getId(), updatedImageFile.getId());
    }

    @Transactional
    public UserResponse updatePassword(Long userId, UserUpdatePasswordRequest request) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));
        return UserResponse.from(user.updatePassword(
                passwordEncoder.encode(request.getPassword())
        ));
    }

    @Transactional
    public UserResponse delete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));
        if (user.getDeletedAt() != null) {
            throw new RequestConflictException("이미 삭제된 회원입니다.");
        }

        userRepository.deleteRefreshTokensByUserId(userId);
        user.delete();
        return UserResponse.from(user);
    }




    private ImageFile resolveProfileImage(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return null;
        }
        String relativePath = extractPathFromUrl(profileImageUrl);
        return imageFileRepository.findByFilePath(relativePath)
                .orElseThrow(() -> new NotFoundException("PROFILE_IMAGE_NOT_FOUND"));
    }

    private String extractPathFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            return path != null ? path : url;

        } catch (URISyntaxException e) {
            return url;
        }
    }

    private ImageFile getImageFile(String path) {
        if (path == null) {
            return null;
        }
        String relativePath = ImageFileUtil.extractPathFromUrl(path);
        return imageFileRepository.findByFilePath(relativePath)
                .orElseThrow(() -> new NotFoundException("PROFILE_IMAGE_NOT_FOUND"));
    }
}
