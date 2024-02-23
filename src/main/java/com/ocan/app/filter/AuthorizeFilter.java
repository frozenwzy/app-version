package com.ocan.app.filter;

import com.alibaba.fastjson.JSON;
import com.ocan.app.mode.Result;
import com.ocan.app.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
@Slf4j
@WebFilter
public class AuthorizeFilter implements Filter {

    // 定义路径匹配器PATH_MATCHER，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    //需要拦截的路径
    public static String[] needTokenUrls = new String[]{
//            "/meeting/**",
    };

    /*
    不需要拦截的路径
    获取验证码，登录，注册，查询，检查新版本，下载主文件，下载指定文件
     */
    public static String[] noNeedTokenUrls = new String[]{
            "/meeting/system/randomImage",
            "/meeting/system/login",
            "/meeting/system/register",
            "/meeting/appVersion/list",
            "/meeting/appUpdate/*"
    };


    //进行请求的拦截、权限验证和放行的逻辑处理。
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //获取请求的路径
        String requestURI = request.getRequestURI();
        log.info("拦截到请求，路径是:{}", requestURI);

        //判断是否是不需要拦截的路径
        if (match(noNeedTokenUrls, requestURI)) {
            log.info("不需要拦截的路径，直接放行");
            filterChain.doFilter(request, response);
            return;
        }

        //对拦截路径进行分析
        if (match(needTokenUrls, requestURI)) {

            log.info("该路径被拦截");
            //获取请求头里的token
            String token = request.getHeader("Token");
            log.info("token:{}", token);

            //判断token是否存在
            if (StringUtils.isEmpty(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(JSON.toJSONString(Result.error("Token不存在")));
                return;
            }

            //判断token是否过期
            try {
                Claims claimsBody = JwtUtil.getClaimsBody(token);
                int result = JwtUtil.verifyToken(claimsBody);
                if (result == 1 || result == 2) {
                    //如果过期，给用户提示
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(JSON.toJSONString(Result.error("Token已过期")));
                } else {
                    //没有过期的话则放行
                    filterChain.doFilter(request, response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.info("其他不需要拦截的路径，直接放行");
            filterChain.doFilter(request, response);
        }

    }

    public boolean match(String[] urls, String requestURI) {

        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            //如果匹配到，就返回true
            if (match) {
                return true;
            }
        }
        // 遍历全部，没有匹配的，就返回false
        return false;
    }


}
