package hei.school.kofiatrans;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=context-test-secret-long-enough-for-hs256")
class KofiaTransApplicationTests {

	@Test
	void contextLoads() {
	}

}
