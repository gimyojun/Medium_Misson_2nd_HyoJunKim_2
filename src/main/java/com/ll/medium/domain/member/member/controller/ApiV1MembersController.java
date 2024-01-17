package com.ll.medium.domain.member.member.controller;

import com.ll.medium.domain.member.member.dto.MemberDto;
import com.ll.medium.domain.member.member.entity.Member;
import com.ll.medium.domain.member.member.service.MemberService;
import com.ll.medium.global.rq.Rq.Rq;
import com.ll.medium.global.rsData.RsData.RsData;
import com.ll.medium.global.util.jwt.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.GrantedAuthority;

import java.util.Map;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(value = "/api/v1/members", produces = "application/json", consumes = "application/json")
@Tag(name = "ApiV1MembersController", description = "회원가입, 로그인, 로그아웃 컨트롤러")
@RequiredArgsConstructor
public class ApiV1MembersController {
    private final MemberService memberService;
    private final Rq rq;
    private final JwtService JwtService;

    @Getter
    @Setter
    public static class LoginRequestBody {
        private String username;
        private String password;

    }
    @Getter
    public static class LoginResponseBody {
        private final MemberDto result;

        public LoginResponseBody(Member member) {
            result = new MemberDto(member);
        }

    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "로그인")
    public RsData<LoginResponseBody> login(@RequestBody LoginRequestBody requestBody) {
        RsData<Member> rsData = memberService.checkUsernameAndPassword(requestBody.getUsername(), requestBody.getPassword());
        Member member = rsData.getData();
        Long id = member.getId();
        String accessToken = JwtService.encode(
                Map.of(
                        "id", id.toString(),
                        "username", member.getUsername(),
                        "authorities", member.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(toList())
                ),
                (long) 1000 * 60 * 10  // 10분짜리 엑세스 토큰
        );
        String refreshToken = JwtService.encode(
                Map.of(
                        "id", id.toString(),
                        "username", member.getUsername()

                ),
                (long) 1000 * 60 * 60 * 24 * 365
        );
        rq.setCrossDomainCookie("accessToken", accessToken);
        rq.setCrossDomainCookie("refreshToken", refreshToken);

        memberService.setRefreshToken(member, refreshToken);
        return rsData.of(new LoginResponseBody(member));
    }

}
