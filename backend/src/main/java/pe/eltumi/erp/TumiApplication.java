package pe.eltumi.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
public class TumiApplication {
    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Lima"));
        SpringApplication.run(TumiApplication.class, args);
    }
}
