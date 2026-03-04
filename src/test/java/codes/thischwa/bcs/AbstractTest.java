package codes.thischwa.bcs;

import java.util.Locale;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestBcgApp.class)
@ActiveProfiles("test")
public abstract class AbstractTest {

  @BeforeAll
  static void init() {
    Locale.setDefault(Locale.ENGLISH);
  }
}
