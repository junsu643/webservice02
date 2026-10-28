package edu.classroom.board;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final SecretKey key;
  private final String issuer;
  private final String audience;
  private final long ttl;
  private final Users users;

  public AuthService(
          @Value("${app.jwt.secret:}") String secret,
          @Value("${app.jwt.issuer}") String issuer,
          @Value("${app.jwt.audience}") String audience,
          @Value("${app.jwt.ttl}") long ttl,
          Users users) {

    if (secret == null || !secret.matches("[0-9a-fA-F]{64}")) {
      throw new IllegalStateException("JWT_SECRET은 64자리 16진수여야 합니다.");
    }
    this.key = Keys.hmacShaKeyFor(HexFormat.of().parseHex(secret));
    this.issuer = issuer;
    this.audience = audience;
    this.ttl = ttl;
    this.users = users;
  }

  public String issueToken(Users.PublicUser user) {
    Instant now = Instant.now();
    return Jwts.builder()
            .subject(String.valueOf(user.id()))
            .issuer(issuer)
            .audience().add(audience).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(ttl)))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
  }

  public long currentUserId(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
      throw new ApiError(401, "TOKEN_REQUIRED", "인증 토큰이 필요합니다.");
    }
    String token = header.substring(7).trim();
    if (token.isEmpty()) {
      throw new ApiError(401, "TOKEN_REQUIRED", "인증 토큰이 필요합니다.");
    }

    Jws<Claims> jws;
    try {
      jws =
              Jwts.parser()
                      .verifyWith(key)
                      .requireIssuer(issuer)
                      .requireAudience(audience)
                      .build()
                      .parseSignedClaims(token);
    } catch (ExpiredJwtException e) {
      throw new ApiError(401, "TOKEN_EXPIRED", "토큰이 만료되었습니다.");
    } catch (JwtException | IllegalArgumentException e) {
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }

    if (!"HS256".equals(jws.getHeader().getAlgorithm())) {
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }
    Claims claims = jws.getPayload();
    if (claims.getExpiration() == null || claims.getSubject() == null) {
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }

    long id;
    try {
      id = Long.parseLong(claims.getSubject());
    } catch (NumberFormatException e) {
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }
    if (users.find(id) == null) {
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }
    return id;
  }

  public void assertOwner(long userId, long authorId) {
    if (userId != authorId) {
      throw new ApiError(403, "FORBIDDEN", "작성자만 수정하거나 삭제할 수 있습니다.");
    }
  }
}