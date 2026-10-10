package pe.eltumi.erp;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.*;
import java.security.*;
import static pe.eltumi.erp.Store.*;
@Configuration
public class Security {
 public record User(long id,String name,String email,String role){}
 public static User user(){var a=SecurityContextHolder.getContext().getAuthentication();if(a==null||!(a.getPrincipal() instanceof User))throw new Problem(401,"Inicia sesión nuevamente");return (User)a.getPrincipal();}
 public static String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 public static String token(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 @Bean BCryptPasswordEncoder passwords(){return new BCryptPasswordEncoder(12);}
 @Bean SecurityFilterChain chain(HttpSecurity http,Store db,@Value("${app.cors}")String origins)throws Exception{
  var cors=new CorsConfiguration();cors.setAllowedOrigins(Arrays.asList(origins.split(",")));cors.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));cors.setAllowedHeaders(List.of("Authorization","Content-Type"));cors.setExposedHeaders(List.of("Content-Disposition"));var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",cors);
  return http.csrf(c->c.disable()).cors(c->c.configurationSource(source)).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a->a.requestMatchers("/","/index.html","/assets/**","/favicon.svg","/api/auth/login","/actuator/health").permitAll().anyRequest().authenticated()).exceptionHandling(x->x.authenticationEntryPoint((req,res,e)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Inicia sesión para continuar\"}");})).addFilterBefore(new OncePerRequestFilter(){protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{String h=req.getHeader("Authorization");if(h!=null&&h.startsWith("Bearer ")){var rows=db.rows("SELECT u.id,u.name,u.email,u.role FROM sessions s JOIN users u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>CURRENT_TIMESTAMP AND u.status='ACTIVO'",hash(h.substring(7)));if(!rows.isEmpty()){var u=rows.get(0);var p=new User(id(u,"id"),str(u,"name"),str(u,"email"),str(u,"role"));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,List.of()));}}chain.doFilter(req,res);}},UsernamePasswordAuthenticationFilter.class).build();
 }
}
