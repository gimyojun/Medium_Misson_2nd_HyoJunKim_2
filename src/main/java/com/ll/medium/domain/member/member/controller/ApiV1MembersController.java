package com.ll.medium.domain.member.member.controller;

import com.ll.medium.domain.member.member.dto.MemberDto;
import com.ll.medium.domain.member.member.entity.Member;
import com.ll.medium.domain.member.member.service.MemberService;
import com.ll.medium.global.rq.Rq.Rq;
import com.ll.medium.global.rsData.RsData.RsData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class ApiV1MembersController {
    private final MemberService memberService;
    private final Rq rq;

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
    public RsData<LoginResponseBody> login(@RequestBody LoginRequestBody requestBody) {
        RsData<Member> rsData = memberService.checkUsernameAndPassword(requestBody.getUsername(), requestBody.getPassword());

        // return new RsData<LoginResponseBody>
        return rsData.of(new LoginResponseBody(rsData.getData()));
    }
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/regenApikey")
    public RsData<?> regenApikey() {

        Member member = rq.getCustomLoginedMember();
        memberService.regenApikey(member);

        return RsData.of("200", "apiKey가 재발급되었습니다");
    }

}
