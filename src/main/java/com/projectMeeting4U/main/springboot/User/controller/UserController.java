package com.projectMeeting4U.main.springboot.User.controller;

import com.projectMeeting4U.main.springboot.Location.controller.LocationController;
import com.projectMeeting4U.main.springboot.Location.entity.CurrentLocation;
import com.projectMeeting4U.main.springboot.Location.entity.CurrentLocationRedis;
import com.projectMeeting4U.main.springboot.Location.repository.CurrentLocationRedisRepository;
import com.projectMeeting4U.main.springboot.Security.JwtTokenProvider;
import com.projectMeeting4U.main.springboot.User.dto.*;
import com.projectMeeting4U.main.springboot.User.entity.User;
import com.projectMeeting4U.main.springboot.User.repository.UserRepository;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Api(tags = {"Users"})
@RestController
@RequestMapping(path = "api")
public class UserController {
    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    LocationController locationController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentLocationRedisRepository currentLocationRedisRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    public UserController(PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/hello") // Test Code
    public String hello() { return "hello world!"; }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-AUTH-TOKEN", value = "로그인 성공 후 access_token", required = true, dataType = "String", paramType = "header")
    })
    @ApiOperation(value = "Get All User's Info", notes = "모든 회원 정보 조회")
    @GetMapping("/users") // Get All Users Info
    public List<User> getAllUsers() { return userRepository.findAll(); }

    @ApiOperation(value = "Create New User Obj", notes = "회원 가입")
    @PostMapping("/sign-up")
    @Transactional
    public NewUserResponse singUp(@ApiParam(value = "회원 가입", required = true) @Valid @RequestBody NewUserRequest newUserRequest) { // Create New User Data

        User user = new User(
                newUserRequest.getUserId(),
                newUserRequest.getName(),
                passwordEncoder.encode(newUserRequest.getPassword()),
                newUserRequest.getEmail(),
                newUserRequest.getPhoneNumber(),
                newUserRequest.getHomeAddress(),
                Collections.singletonList("ROLE_USER") // 권한 부여
        );

        CurrentLocationRedis currentLocationRedis = new CurrentLocationRedis(
                                                        newUserRequest.getUserId(),
                                                        null,
                                                        null );

        try {
            userRepository.save(user);
            currentLocationRedisRepository.save(currentLocationRedis);
            NewUserResponse newUserResponse = new NewUserResponse("true");
            return newUserResponse;
        } catch (Exception e){
            NewUserResponse newUserResponse = new NewUserResponse("false");
            return newUserResponse;
        }

    }

    @ApiOperation(value = "Login", notes = "로그인")
    @PostMapping("/sign-in")
    @Transactional
    public LoginResponse signIn(@ApiParam(value = "id", required = true) @Valid @RequestBody LoginRequest loginRequest) {
        User user = userRepository.findByUserId(loginRequest.getUserId());
        LoginResponse loginResponse = new LoginResponse();

        if(user == null) { // id가 존재하지 않는 경우
            loginResponse.setLoginResult("false");
            return loginResponse;
        }
        boolean check = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()); // 비밀번호 checking

        if(!check) { // password not matching
            loginResponse.setLoginResult("false");
        } else {
            loginResponse.setLoginResult("true");
        }

        System.out.println("Role = " + user.getRoles());
        loginResponse.setJwtToken(jwtTokenProvider.createToken(user.getUserId(), user.getRoles()));
        return loginResponse;
    }


    @ApiOperation(value = "Check ID exist already", notes = "아이디 중복 조회")
    @GetMapping("/sign-up/{id}")
    public String checkUserId(@PathVariable String id) { // Check ID exist already
        if(userRepository.findByUserId(id) == null)
            return "true";

        return "false";
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-AUTH-TOKEN", value = "로그인 성공 후 access_token", required = true, dataType = "String", paramType = "header")
    })
    @ApiOperation(value = "Get User Information", notes = "유저 정보 조회")
    @GetMapping("/user/{id}")
    @Transactional
    public UserResponse getUser(@PathVariable String id) { // Get {id} User Information
        User user = userRepository.findByUserId(id);
        UserResponse userResponse = new UserResponse();

        if (user == null) {
            userResponse.setResult("false");
        } else {
            userResponse.setResult("true");
            userResponse.setUserId(user.getUserId());
            userResponse.setName(user.getName());
            userResponse.setEmail(user.getEmail());
            userResponse.setPhoneNumber(user.getPhoneNumber());
            userResponse.setHomeAddress(user.getHomeAddress());
            userResponse.setCurrentLocation(user.getCurrentLocation());
            userResponse.setCreatedAt(user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            userResponse.setUpdatedAt(user.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }

        return userResponse;

    }



}
