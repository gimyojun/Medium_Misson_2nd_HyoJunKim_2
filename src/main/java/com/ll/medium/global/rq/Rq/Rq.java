package com.ll.medium.global.rq.Rq;

import com.ll.medium.domain.member.member.entity.Member;
import com.ll.medium.domain.member.member.service.MemberService;
import com.ll.medium.global.auth.CustomUser;
import com.ll.medium.global.rsData.RsData.RsData;
import com.ll.medium.standard.util.Ut.Ut;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Component
@RequestScope
@RequiredArgsConstructor
public class Rq {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final MemberService memberService;
    private Member member;
    private final EntityManager entityManager;


    public String redirect(String url, String msg) {
        if(msg == null){
            return "redirect:" + url;
        }
        boolean containsTtl = msg.contains(";ttl=");

        if (containsTtl) {
            msg = msg.split(";ttl=", 2)[0];
        }

        msg = URLEncoder.encode(msg, StandardCharsets.UTF_8);
        msg += ";ttl=" + (new Date().getTime() + 1000 * 5);

        return "redirect:" + url + "?msg=" + msg;
    }
    public String historyBack(String msg) {
        request.setAttribute("failMsg", msg);

        return "global/js";
    }

    public String redirectOrBack(RsData<?> rs, String path) {
        if (rs.isFail()) return historyBack(rs.getMsg());

        return redirect(path, rs.getMsg());
    }
    //TODO 사용하지 않을 메서드. usages확인하고 메서드 대체되면 삭제요망
    public User getUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .filter(it -> it instanceof User)
                .map(it -> (User) it)
                .orElse(null);
    }
    public CustomUser getCurrentUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getPrincipal() instanceof CustomUser)
                .map(authentication -> (CustomUser) authentication.getPrincipal())
                .orElse(null);
    }

    public boolean isLogin() {
        return getCurrentUser() != null;
    }

    public boolean isLogout() {
        return !isLogin();
    }

    //TODO getCurrentUser()를 통해 현재 로그인된 멤버가 관리자임을 확인
    public boolean isAdmin() {
        if (isLogout()) return false;

        return getUser()
                .getAuthorities()
                .stream()
                .anyMatch(it -> it.getAuthority().equals("ROLE_ADMIN"));
    }
    // TODO 대체 메서드 만들어야함.
    public boolean isPaid() {
        if(isLogout())return false;
        return getUser()
                .getAuthorities()
                .stream()
                .anyMatch(it -> it.getAuthority().equals("ROLE_PAID"));
    }

    public void setAttribute(String key, Object value) {
        request.setAttribute(key, value);
    }

    public String getCurrentQueryStringWithoutParam(String paramName) {
        String queryString = request.getQueryString();

        if (queryString == null) {
            return "";
        }

        queryString = Ut.url.deleteQueryParam(queryString, paramName);

        return queryString;
    }

    //현재 로그인된 멤버 이름을 통해 객체를 반환
    public Member getLoginedMember(){
        if (isLogout())
            return null;
        Member member = memberService.findByUsername(this.getCurrentUser().getUsername()).get();
        return member;
    }

    public Member getAuthenticatedMemberFromSecurityContext(){
        if (isLogout())
            throw new RuntimeException("rq1 : 로그인이 필요합니다. 로그인 후 다시 시도해주세요.");
        if(member == null){
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUser user = (CustomUser) authentication.getPrincipal();
            long memberId = user.getId();
            member = entityManager.getReference(Member.class, memberId);
        }
        return member;
    }
}
