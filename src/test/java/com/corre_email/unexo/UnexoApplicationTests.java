package com.corre_email.unexo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Al levantar la app arranca tambien la revision del buzon cada 10 s. Se apunta
// el correo a 127.0.0.1 (donde no hay servidor) para que un test NUNCA lea,
// responda ni borre correos del buzon real.
@SpringBootTest(properties = { "app.mail.host=127.0.0.1", "spring.mail.host=127.0.0.1" })
class UnexoApplicationTests {

	@Test
	void contextLoads() {
	}

}
