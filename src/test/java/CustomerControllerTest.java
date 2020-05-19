import application.model.Customer;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * The purpose of this class is to test the @see CustomerController class rest end points.
 * @author Oscar I. Ricaud
 */
@FixMethodOrder(MethodSorters.JVM)
public class CustomerControllerTest {
    RestTemplate restTemplate = new RestTemplate();
    String baseUrl = "http://localhost:8080" + "/customer";
    private HttpHeaders headers = new HttpHeaders();
    private Customer newCustomer = new Customer("9875", null, "yooyo",
            "passw0rd", "josh", "hernandez", "helloworld@gmail.com",
            "test.png");

    private SecurityContext ctx = SecurityContextHolder.createEmptyContext();

    @Before
    public void constructor() {
        // Define security context
        SecurityContextHolder.setContext(ctx);
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken("anonymous", "", Arrays.asList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    }

    /**
     * Method is responsible for checking the /check rest end point
     *
     * @throws URISyntaxException
     */
    @Test
    public void check() throws URISyntaxException {
        URI uri = new URI(baseUrl + "/check");
        HttpEntity request = new HttpEntity(headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);
            int responseCode = response.getStatusCodeValue();
            String responseBody = response.getBody();
            System.out.println("responseBody " + responseBody);
            assertEquals(responseBody, "It works!");
        } catch (HttpClientErrorException e) { // Catch error when adding duplicate employee
            assertEquals(406, e.getRawStatusCode()); // Catch error when adding duplicate employee
        }
    }

    /**
     * Method is responsible for testing the functionality of adding a customer to the database
     * table
     *
     * @throws URISyntaxException
     */
    @Test
    public void addCustomer() throws URISyntaxException {
        URI uri = new URI(baseUrl + "/add");
        HttpEntity<Customer> request = new HttpEntity(newCustomer, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);
            int responseCode = response.getStatusCodeValue();
            System.out.println("response " + response);
            assertEquals(201, responseCode);
        } catch (HttpClientErrorException e) { // Catch error when adding duplicate employee
            assertEquals(406, e.getRawStatusCode()); // Catch error when adding duplicate employee
        }
    }

    /**
     * Method is responsible for updating a customer by id
     * table
     *
     * @throws URISyntaxException
     */
    @Test
    public void updateCustomerById() throws URISyntaxException {
        // Define headers and base url
        URI uri = new URI(baseUrl + "/update/" + newCustomer.get_id());

        // Change the customer name
        newCustomer.setUsername("kimmy");

        // Prepare the headers to store the security context
        List<String> temp = new LinkedList<>();
        temp.add(String.valueOf(ctx.getAuthentication().isAuthenticated()));
        headers.put("securitycontext", temp);

        HttpEntity request = new HttpEntity(newCustomer, headers);

        try {
            ResponseEntity<Customer> response = restTemplate.postForEntity(uri, request, Customer.class);
            int responseCode = response.getStatusCodeValue();
            Customer responseBody = response.getBody();
            System.out.println("response " + response);
            assertEquals(200, responseCode);
        } catch (HttpClientErrorException e) { // Catch error when adding duplicate customer
            assertEquals(406, e.getRawStatusCode()); // Catch error when adding duplicate employee
        }
    }

    /**
     * Method is responsible for searching a customer by username
     *
     * @throws URISyntaxException
     */
    @Test
    public void searchCustomerByUsername() throws URISyntaxException {
        URI uri = new URI(baseUrl + "/search-by-username/" + newCustomer.getUsername());

        System.out.println("newCustomer.getUsername()" + newCustomer.getUsername());

        HttpEntity<String> request = new HttpEntity<String>(newCustomer.getUsername(), headers);
        try {

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            int responseCode = response.getStatusCodeValue();
            assertEquals(200, responseCode);
        } catch (HttpClientErrorException e) { // Catch error when adding duplicate customer
            assertEquals(406, e.getRawStatusCode()); // Catch error when adding duplicate employee
        }
    }

    /**
     * Method is responsible for searching a customer by id
     *
     * @throws URISyntaxException
     */
    @Test
    public void searchCustomerById() throws URISyntaxException {
        System.out.println("newCustomer.getId()" + newCustomer.get_id());
        URI uri = new URI(baseUrl + "/search/" + newCustomer.get_id());
        HttpEntity<String> request = new HttpEntity<String>(newCustomer.get_id(), headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);
            int responseCode = response.getStatusCodeValue();
            assertEquals(200, responseCode);
        } catch (HttpClientErrorException e) { // Catch error when adding duplicate customer
            assertEquals(406, e.getRawStatusCode()); // Catch error when adding duplicate employee
        }
    }

    /**
     * Method is responsible for deleting customer
     *
     * @throws URISyntaxException
     */
    @Test
    public void deleteCustomerById() throws URISyntaxException {
        URI uri = new URI(baseUrl + "/delete/" + newCustomer.get_id());
        HttpEntity<String> request = new HttpEntity(newCustomer.get_id(), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);
            int responseCode = response.getStatusCodeValue();
            String responseBody = response.getBody();
            System.out.println("response " + response);
            assertEquals(200, responseCode);
        } catch (HttpClientErrorException e) { // Catch error when adding duplicate employee
            assertEquals(406, e.getRawStatusCode()); // Catch error when adding duplicate employee
        }
    }

    /**
     * Method is responsible for retrieving all customers
     *
     * @throws URISyntaxException
     */
    @Test
    public void getAllCustomers() throws URISyntaxException {
        URI uri = new URI(baseUrl + "/list");
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<String>() {});
        System.out.println("customers list " + response);
        assertEquals(200, response.getStatusCodeValue());
    }
}
