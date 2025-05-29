package pe.edu.vallegrande.report_workshop_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import pe.edu.vallegrande.report_workshop_service.config.DotenvInitializer;

@SpringBootTest
@ContextConfiguration(initializers = DotenvInitializer.class)
class ReportWorkshopServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
