package akuma.whiplash.infrastructure.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class FirebaseConfig {
	private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

	@PostConstruct
    public void init(){
    	try{
        	InputStream serviceAccount = new ClassPathResource("whiplash-firebase-key.json").getInputStream();
        	FirebaseOptions options = new FirebaseOptions.Builder()
            		.setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
                        
         	if (FirebaseApp.getApps().isEmpty()) { // FirebaseApp이 이미 초기화되어 있지 않은 경우에만 초기화 실행
            	FirebaseApp.initializeApp(options);
				log.info("FirebaseApp initialized");
            } else {
				log.debug("FirebaseApp already initialized");
			}
        } catch (Exception e){
			log.error("Failed to initialize FirebaseApp", e);
        }
    }
}