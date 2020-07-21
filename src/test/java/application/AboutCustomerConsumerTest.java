package application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;

public class AboutCustomerConsumerTest {
	
	@Rule
    public PactProviderRule mockProvider = new PactProviderRule("customer_provider","localhost", 8080, this);
    
    @Pact(provider = "customer_provider", consumer = "customer_consumer")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        PactDslJsonBody bodyResponse = new PactDslJsonBody()
                .stringValue("name", "Customer Service")
                .stringValue("parentRepo", "Storefront")
                .stringValue("description", "Manages all customer data");
        
        System.out.println(bodyResponse);
        
        return builder
        	      .given("test GET")
        	        .uponReceiving("GET REQUEST")
        	        .path("/micro/about")
        	        .method("GET")
        	      .willRespondWith()
        	        .status(200)
        	        .headers(headers)
        	        .body(bodyResponse)
        	        .toPact();
    }
    
    @Test
    @PactVerification()
    public void givenGet_whenSendRequest_shouldReturn200WithProperHeaderAndBody(){
    	// when
        ResponseEntity<String> response = new RestTemplate().getForEntity(mockProvider.getUrl() + "/micro/about", String.class);
//     
        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().get("Content-Type").contains("application/json")).isTrue();
        assertThat(response.getBody()).contains("name", "Customer Service","parentRepo", "Storefront", "description", "Manages all customer data");
    }

}
