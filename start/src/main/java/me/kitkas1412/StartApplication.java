package me.kitkas1412;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Khởi động Spring Boot và bật scheduler cho outbox publisher cùng tác vụ đồng bộ tồn kho.
 *
 * <p>Class này nằm ở {@code me.kitkas1412.ticketbooking}, còn các module nằm ở
 * {@code me.kitkas1412.<module>}; mặc định Spring chỉ quét package của class khởi động.
 * {@code scanBasePackages} để nạp bean của các module, {@code @AutoConfigurationPackage}
 * để JPA tìm được entity và repository, {@code @ConfigurationPropertiesScan} cho các
 * class {@code @ConfigurationProperties} (vd: JwtProperties).
 */
@SpringBootApplication(scanBasePackages = "me.kitkas1412")
public class StartApplication {
    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class, args);
    }
}
