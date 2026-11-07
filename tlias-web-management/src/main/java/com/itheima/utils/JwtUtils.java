package com.itheima.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Map;

/**
 * JWT 令牌工具类
 */
public class JwtUtils {

    /**
     * 签名密钥
     */
    private static final String SIGN_KEY = "tlias-web-management-jwt-secret-key";

    /**
     * 过期时间：12 小时
     */
    private static final long EXPIRE_TIME = 12 * 60 * 60 * 1000L;

    /**
     * 生成 JWT 令牌
     *
     * @param claims 自定义载荷，例如员工的 id、username
     * @return 生成的 JWT 令牌字符串
     */
    public static String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(SignatureAlgorithm.HS256, SIGN_KEY)
                .compact();
    }

    /**
     * 解析 JWT 令牌
     *
     * @param token JWT 令牌字符串
     * @return 令牌中的载荷
     * @throws io.jsonwebtoken.JwtException 令牌被篡改、签名不匹配或已过期时抛出
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(SIGN_KEY)
                .parseClaimsJws(token)
                .getBody();
    }
}
